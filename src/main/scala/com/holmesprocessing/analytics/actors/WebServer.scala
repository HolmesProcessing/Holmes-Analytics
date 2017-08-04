package com.holmesprocessing.analytics.actors

import java.text.{ ParseException, SimpleDateFormat }
import java.util.{ Date, UUID }
import java.nio.file.{Files, Paths}


import scala.util.{ Success, Failure }
import scala.concurrent.duration._
import scala.concurrent.{ Future, Await }
import scala.collection.JavaConverters._


import akka.actor.ActorSystem
import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout

import spray.json._
import spray.json.DefaultJsonProtocol._

import com.typesafe.config.Config

final case class APISuccess[T](status: String = "success", result: T)
final case class APIError(status: String = "failure", error: String)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
	
	implicit object UUIDFormat extends JsonFormat[UUID] {
		def write(uuid: UUID) = JsString(uuid.toString)
		def read(value: JsValue) = {
			value match {
				case JsString(uuid) => UUID.fromString(uuid)
				case _              => throw DeserializationException("Expected hexadecimal UUID string")
			}
		}
	}

	implicit object ActorRefFormat extends JsonFormat[ActorRef] {
		def write(ar: ActorRef) = JsString(ar.toString)
		def read(value: JsValue) = {
			value match {
				case _ => throw DeserializationException("Can't unmarshal ActorRef") //no possible without context
			}
		}
	}

	def parseIsoDateString(date: String): Option[Date] = {
		if (date.length != 28) None
		else try Some(localIsoDateFormatter.get().parse(date))
		catch {
			case p: ParseException => None
		}
	}

	private val localIsoDateFormatter = new ThreadLocal[SimpleDateFormat] {
		override def initialValue() = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
	}

	def dateToIsoString(date: Date) = localIsoDateFormatter.get().format(date)

	implicit object DateFormat extends JsonFormat[Date] {

		def write(date : Date) : JsValue = JsString(dateToIsoString(date))

		def read(json: JsValue) : Date = json match {

			case JsString(rawDate) => parseIsoDateString(rawDate) match {
				case None => deserializationError(s"Expected ISO Date format, got $rawDate")
				case Some(isoDate) => isoDate
			}

			case unknown => deserializationError(s"Expected JsString, got $unknown")
		}
	}


	implicit val JobRefFormat = jsonFormat4(JobRef)
	implicit val APISuccessUUIDFormat = jsonFormat2(APISuccess[UUID])
	implicit val APISuccessJobRefFormat = jsonFormat2(APISuccess[JobRef])
	implicit val APISuccessStringFormat = jsonFormat2(APISuccess[String])
	implicit val APISuccessMapUJFormat = jsonFormat2(APISuccess[Map[UUID, JobRef]])
	implicit val APIErrorFormat = jsonFormat2(APIError)
}

object WebServer {
	def props(cfg: Config, scheduler: ActorRef): Props = Props(new WebServer(cfg, scheduler))
}

class WebServer(cfg: Config, scheduler: ActorRef) extends Actor with ActorLogging with Directives with JsonSupport {
	override def preStart(): Unit = log.info("WebServer started")
	override def postStop(): Unit = log.info("WebServer stopped")
	override def receive: Receive = Actor.emptyBehavior

	//TODO: Add blockingDispatcher with fixed threads
	implicit val executionContext = context.dispatcher

	implicit val materializer = ActorMaterializer()

	implicit val timeout: Timeout = 10.seconds

	val staticDir = cfg.getString("staticDir")


	/*
	TODO: Move the API documentation as soon as it is final.


	   GET / -> Redirect to /web

	   GET /web -> The webinterface

	   GET /api/v1 -> Base API URL, greeting
	
	   GET ../jobs -> get a list of job
	  POST ../jobs -> create a new job

	   GET ../jobs/$UUID -> get all infos about job $UUID w/ result
	DELETE ../jobs/$UUID -> delete job $UUID
	   GET ../jobs/$UUID/result -> get result of job $UUID
	*/
	

	private def getExtensions(fileName: String) : String = {
		val index = fileName.lastIndexOf('.')
		if(index != 0) {
			fileName.drop(index+1)
		}

		""
	}

	def routeWeb =
		pathPrefix("web") {
			get {
				entity(as[HttpRequest]) { requestData =>
					complete {
						val fullPath = requestData.uri.path.toString match {
							case "/"=> Paths.get(staticDir + "/index.html")
							case "" => Paths.get(staticDir + "/index.html")
							case _ => Paths.get(staticDir +  requestData.uri.path.toString)
						}

						val ext = getExtensions(fullPath.getFileName.toString)
						val mediaType = MediaTypes.forExtension(ext)
						val c: ContentType = mediaType match {
							case x: MediaType.Binary           => ContentType(x)
							case x: MediaType.WithFixedCharset => ContentType(x)
							case x: MediaType.WithOpenCharset  => ContentType(x, HttpCharsets.`UTF-8`)
						}

						val byteArray = Files.readAllBytes(fullPath)
						HttpResponse(StatusCodes.OK, entity = HttpEntity(c, byteArray))
					}
				}
			}
		}
	

	val routeJobs =
		pathPrefix("jobs") {
			pathEnd {
				// GET /jobs
				get {
					onSuccess(scheduler ? SchedulerProtocol.GetList()) {
						case resp: JobRefMap =>
							complete(APISuccess[Map[UUID, JobRef]](result = resp.m))
						case t =>
							log.warning("WebServer Failure: {}", t)
							complete(APIError(error = t.toString))
					}
				} ~
				// POST /jobs
				post {
					formFields('name.as[String], 'engine.as[String], 'service.as[String]) { (name, engine, service) =>
						onSuccess(scheduler ? SchedulerProtocol.New(name, engine, service, Map.empty[String, String])) {
							case resp: UUID =>
								complete(APISuccess[UUID](result = resp))
							case t =>
								log.warning("WebServer Failure: {}", t)
								complete(APIError(error = t.toString))
						}
					}

				}
			} ~
			path( JavaUUID ) { id =>
				pathEnd {
					// GET /jobs/$UUID
					get {
						onSuccess(scheduler ? SchedulerProtocol.GetJob(id)) {
							case resp: JobRef =>
								complete(APISuccess[JobRef](result = resp))
							case t =>
								log.warning("WebServer Failure: {}", t)
								complete(APIError(error = t.toString))
						}
					} ~
					delete {
						onSuccess(scheduler ? SchedulerProtocol.DeleteJob(id)) {
							case resp: String =>
								complete(APISuccess[String](result = resp))
							case t =>
								log.warning("WebServer Failure: {}", t)
								complete(APIError(error = t.toString))
						}
					}
				} ~
				path( "result" ) {
					// GET /jobs/$UUID/result
					get {
						onSuccess(scheduler ? SchedulerProtocol.GetResult(id)) {
							case resp: String =>
								complete(APISuccess[String](result = resp))
							case t =>
								log.warning("WebServer Failure: {}", t)
								complete(APIError(error = t.toString))
						}
					}
				}
			}
		}

	val routeAPI =
		pathPrefix("api" / "v1") {
			pathEnd {
				get {
					complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Holmes-Analytics API v1"))
				}
			} ~
			routeJobs
		}

	val route =
		pathSingleSlash {
			redirect("/web/index.html", StatusCodes.TemporaryRedirect)
		} ~
		routeWeb ~
		routeAPI

	val bindingFuture = Http(context.system).bindAndHandle(route, cfg.getString("interface"), cfg.getInt("port"))
}
