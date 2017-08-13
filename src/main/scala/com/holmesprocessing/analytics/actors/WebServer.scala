package com.holmesprocessing.analytics.actors

import java.util.UUID
import java.nio.file.{Files, Paths, NoSuchFileException}

import scala.concurrent.duration._

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout

import com.typesafe.config.Config

import com.holmesprocessing.analytics.types.{JsonSupport, APISuccess, APIError}

/** Factory for [[actors.WebServer]] actors. */
object WebServer {
	def props(cfg: Config, scheduler: ActorRef): Props = Props(new WebServer(cfg, scheduler))
}

/** Actor holding the webserver.
 *
 *  @param cfg WebServer config
 *  @param scheduler ActorRef to the [[actors.Scheduler]] actor.
 */
class WebServer(cfg: Config, scheduler: ActorRef) extends Actor with ActorLogging with Directives with JsonSupport {
	override def preStart(): Unit = log.info("WebServer started")
	override def postStop(): Unit = log.info("WebServer stopped")
	override def receive: Receive = Actor.emptyBehavior

	//TODO: Add blockingDispatcher with fixed amount of threads
	implicit val executionContext = context.dispatcher

	implicit val materializer = ActorMaterializer()

	implicit val timeout: Timeout = 10.seconds

	implicit def myExceptionHandler: ExceptionHandler =
	ExceptionHandler {
		case _: NoSuchFileException =>
		extractUri { uri =>
			log.debug("File not found: {}", uri)
			complete(HttpResponse(StatusCodes.NotFound, entity = "File not found"))
		}
	}

	private def getExtension(fileName: String) : String = {
		val index = fileName.lastIndexOf('.')
		if(index != 0) {
			return fileName.drop(index+1)
		}

		""
	}

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
	

	def routeWeb =
		pathPrefix("web") {
			get {
				entity(as[HttpRequest]) { requestData =>
					val uri = requestData.uri.path.toString
					val fullPath = uri match {
						case _ if uri.endsWith("/") => Paths.get(staticDir + uri + "/index.html")
						case _ => Paths.get(staticDir + uri)
					}

					if(Files.isDirectory(fullPath)){
						redirect(staticDir + uri + "/", StatusCodes.PermanentRedirect)
					} else {
						val ext = getExtension(fullPath.getFileName.toString)
						val mediaType = MediaTypes.forExtension(ext)
						val c: ContentType = mediaType match {
							case x: MediaType.Binary           => ContentType(x)
							case x: MediaType.WithFixedCharset => ContentType(x)
							case x: MediaType.WithOpenCharset  => ContentType(x, HttpCharsets.`UTF-8`)
						}

						val byteArray = Files.readAllBytes(fullPath)
						complete(HttpResponse(StatusCodes.OK, entity = HttpEntity(c, byteArray)))
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
			pathPrefix( JavaUUID ) { id =>
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
