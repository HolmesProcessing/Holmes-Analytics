package group.holmes.analytics.actors

import java.io.File

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.typesafe.config.ConfigFactory


object AnalyticsSupervisor {
	def props(cfgPath: String): Props = Props(new AnalyticsSupervisor(cfgPath))
}

class AnalyticsSupervisor(cfgPath: String) extends Actor with ActorLogging {
	override def preStart(): Unit = log.info("Holmes-Analytics started")
	override def postStop(): Unit = log.info("Holmes-Analytics stopped")
	override def receive = Actor.emptyBehavior


	//TODO: save a reference to the AE actor and pass along
	var analyticEngineManager: ActorRef = _
	var analyticServiceManager: ActorRef = _

	// load the config
	val cfg = ConfigFactory.parseFile(new File(cfgPath))

	// create the core
	val core = context.actorOf(Core.props(cfg, analyticEngineManager, analyticServiceManager))
}
