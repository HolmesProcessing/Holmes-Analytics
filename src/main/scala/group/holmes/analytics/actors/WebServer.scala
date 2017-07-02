package group.holmes.analytics.actors

import java.text.{ ParseException, SimpleDateFormat }
import java.util.{ Date, UUID }
import spray.json.{ DefaultJsonProtocol, DeserializationException, JsString, JsValue, JsonFormat, deserializationError }

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json.DefaultJsonProtocol._
import scala.concurrent.duration._
import scala.concurrent.{ Future, Await }
import scala.collection.mutable.HashMap
import scala.collection.JavaConverters._

import scala.util.{ Success, Failure }

import com.typesafe.config.Config

object WebServer {
	def props(cfg: Config, scheduler: ActorRef): Props = Props(new WebServer(cfg, scheduler))
}

class WebServer(cfg: Config, scheduler: ActorRef) extends Actor with ActorLogging {
	override def preStart(): Unit = log.info("WebServer started")
	override def postStop(): Unit = log.info("WebServer stopped")
	override def receive: Receive = Actor.emptyBehavior

	implicit val executionContext = context.dispatcher

	implicit val materializer = ActorMaterializer()

	implicit val timeout: Timeout = 5.seconds

	val route =
		path("api" / "v1" / "jobs") {
			pathEnd {
				get {
					onSuccess(scheduler ? Scheduler.List()) {
						case resp: HashMap[UUID, Scheduler.ScheduledJob] =>
							complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, resp.toString))
						case t =>
							log.warning("WebServer Failure: {}", t)
							complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Error occured"))
					}
				} ~
					post {
						formFields('name.as[String], 'engine.as[String], 'service.as[String]) { (name, engine, service) =>
							onSuccess(scheduler ? Scheduler.New(name, engine, service, HashMap.empty[String, String])) {
								case resp: UUID =>
									complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, resp.toString))
								case t =>
									log.warning("WebServer Failure: {}", t)
									complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Error occured"))
							}
						}

					}
			}
		} ~
			path("api" / "v1") {
				get {
					complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Holmes-Analytics API v1"))
				}
			}

	val bindingFuture = Http(context.system).bindAndHandle(route, cfg.getString("interface"), cfg.getInt("port"))
}
