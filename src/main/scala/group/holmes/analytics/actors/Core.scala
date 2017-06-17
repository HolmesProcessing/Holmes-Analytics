package group.holmes.analytics.actors

import java.io.File

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.typesafe.config.Config


object Core {
	def props(cfg: Config, analyticEngineManager: ActorRef, analyticServiceManager: ActorRef): Props = { Props(new Core(cfg, analyticEngineManager, analyticServiceManager)) }
}

class Core(cfg: Config, analyticEngineManager: ActorRef, analyticServiceManager: ActorRef) extends Actor with ActorLogging {
	override def preStart(): Unit = log.info("Core started")
	override def postStop(): Unit = log.info("Core stopped")
	override def receive = Actor.emptyBehavior

	// create the WebServer
	val webserver = context.actorOf(WebServer.props(cfg.getConfig("webserver")))

	// create the amqp consumer
	val amqpConsumer = context.actorOf(RabbitConsumer.props(cfg.getConfig("amqp")))

	// create the scheduler
	//TODO: Pass ref to AnalyticEngineManager and AnalyticServiceManager to the scheduler
	val scheduler = context.actorOf(Scheduler.props(analyticEngineManager, analyticServiceManager))
}
