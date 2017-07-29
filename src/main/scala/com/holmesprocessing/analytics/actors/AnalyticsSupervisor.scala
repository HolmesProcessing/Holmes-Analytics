package com.holmesprocessing.analytics.actors

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

	// load the config
	val cfg = ConfigFactory.parseFile(new File(cfgPath))

	// create the aem
	val analyticEngineManager: ActorRef = context.actorOf(AnalyticEngineManager.props(cfg))

	// create the core
	val core = context.actorOf(Core.props(cfg, analyticEngineManager))
}
