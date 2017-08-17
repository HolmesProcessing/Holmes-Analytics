package com.holmesprocessing.analytics.actors

import java.util.UUID

import scala.concurrent.duration._

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.util.Timeout

import com.holmesprocessing.analytics.types.{AnalyticEngineProtocol, GenericAnalyticService}

/** Factory for [[actors.Job]] actors. */
object Job {
	def props(id: UUID, name: String, analyticEngine: ActorRef, analyticService: GenericAnalyticService, servicesPath: String, parameters: Map[String, String]): Props = Props(new Job(id, name, analyticEngine, analyticService, servicesPath, parameters))
}

/** Holds all messages accepted by [[actors.Job]] */
object JobProtocol {
	final case class GetId()
	final case class GetName()
	final case class GetStatus()
	final case class GetResult()
}

/** Actor that manages the execution of a job consisting of an [[types.GenericAnalyticEngine]] and a [[types.GenericAnalyticService]].
 *
 *  @constructor create a new job actor.
 *  @param id UUID to identify the job
 *  @param name Phonetic name for the job
 *  @param analyticEngine engine supporting [[types.GenericAnalyticEngine]]
 *  @param analyticService service supporting [[types.GenericAnalyticService]]
 *  @param servicesPath path to the folder containing all services
 *  @param parameters parameters for the job in the form of Map[key, value]
 */
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

	/** Start the job by building the service and executing it on the engine. */
	def start(): Unit = {
		implicit val timeout: Timeout = 10.minutes

		//TODO: catch build errors here
		analyticService.build(servicesPath)
		val objPath = analyticService.getObjPath(servicesPath)

		analyticEngine ! AnalyticEngineProtocol.Execute(objPath)
	}

	/** Stop the execution on the engine. */
	def stop(): Unit = {
		analyticEngine ! AnalyticEngineProtocol.Stop
	}
}
