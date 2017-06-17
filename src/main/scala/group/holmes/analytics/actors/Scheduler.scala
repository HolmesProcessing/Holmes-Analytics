package group.holmes.analytics.actors

import java.io.File

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.typesafe.config.ConfigFactory


object Scheduler {
	def props(analyticEngineManager: ActorRef, analyticServiceManager: ActorRef): Props = Props(new Scheduler(analyticEngineManager, analyticServiceManager))
}

class Scheduler(analyticEngineManager: ActorRef, analyticServiceManager: ActorRef) extends Actor with ActorLogging {
	override def preStart(): Unit = log.info("Scheduler started")
	override def postStop(): Unit = log.info("Scheduler stopped")
	override def receive = Actor.emptyBehavior

	// Dummy Actor
}
