package com.holmesprocessing.analytics.actors

import java.util.UUID

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global


import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.typesafe.config.ConfigFactory
import akka.util.Timeout
import akka.pattern.ask

import com.holmesprocessing.analytics.types.{AnalyticEngineProtocol, GenericAnalyticService}


object Job {
	def props(id: UUID, name: String, analyticEngine: ActorRef, analyticService: GenericAnalyticService, servicesPath: String, parameters: Map[String, String]): Props = Props(new Job(id, name, analyticEngine, analyticService, servicesPath, parameters))
}

object JobProtocol {
	final case class GetId()
	final case class GetName()
	final case class GetStatus()
	final case class GetResult()
}

class Job(id: UUID, name: String, analyticEngine: ActorRef, analyticService: GenericAnalyticService, servicesPath: String, parameters: Map[String, String]) extends Actor with ActorLogging {
	override def preStart(): Unit = log.info("Job " + name + " started")
	override def postStop(): Unit = log.info("Job " + name + " stopped")

	// no reason to wait, start the job
	this.start()

	override def receive = {
		case msg: JobProtocol.GetId =>
			sender() ! this.id

		case msg: JobProtocol.GetName =>
			sender() ! this.name

		case msg: JobProtocol.GetStatus =>
			analyticEngine forward AnalyticEngineProtocol.GetStatus()

		case msg: JobProtocol.GetResult =>
			analyticEngine forward AnalyticEngineProtocol.GetResult()

		case x => log.warning("Received unknown message: {}", x)
	}

	def start(): Unit = {
		implicit val timeout: Timeout = 10.minutes

		//TODO: catch build errors here
		analyticService.build(servicesPath)
		val objPath = analyticService.getObjPath(servicesPath)

		analyticEngine ! AnalyticEngineProtocol.Execute(objPath)
	}

	def stop(): Unit = {
		analyticEngine ! AnalyticEngineProtocol.Stop
	}
}
