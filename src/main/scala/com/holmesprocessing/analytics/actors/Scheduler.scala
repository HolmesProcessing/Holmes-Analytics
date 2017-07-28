package com.holmesprocessing.analytics.actors

import java.io.File
import java.util.UUID

import scala.collection.mutable.HashMap
import scala.concurrent.duration._
import scala.concurrent.Await

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import com.holmesprocessing.analytics.types.{GenericAnalyticService}

import com.holmesprocessing.analytics.services.distinctmimes.DistinctMimes

// To track and keep state
final case class JobRef(ref: ActorRef, id: UUID, name: String, status: String)

/** Factory for [[com.holmesprocessing.analytics.actors.Scheduler]] actors. */
object Scheduler {
	def props(analyticEngineManager: ActorRef, servicesPath: String): Props = Props(new Scheduler(analyticEngineManager, servicesPath))
}

object SchedulerProtocol {
	final case class New(name: String, engine: String, service: String, parameters: HashMap[String, String])
	final case class GetStatus(id: UUID)
	final case class Refresh()
	final case class GetList()
	final case class GetResult(id: UUID)
}

class Scheduler(analyticEngineManager: ActorRef, servicesPath: String) extends Actor with ActorLogging {
	private val jobs = HashMap.empty[UUID, JobRef]
	implicit val timeout: Timeout = 10.seconds //TODO: Discuss sensible value / error mitigation strategy

	override def preStart(): Unit = log.info("Scheduler started")
	override def postStop(): Unit = log.info("Scheduler stopped")

	override def receive = {
		// request to create a new job
		case msg: SchedulerProtocol.New =>
			val id = UUID.randomUUID()
			val service = this.getService(msg.service)

			//TODO: does the service or the user decide which engine to use?
			val ref = context.actorOf(
				Job.props(
					id,
					msg.name,
					this.getEngine(service.engine),
					service,
					servicesPath,
					msg.parameters))

			this.jobs += (id -> JobRef(ref, id, msg.name, "unknown"))
			sender() ! id

		// request to get the status of a job
		case msg: SchedulerProtocol.GetStatus =>
			if (jobs.contains(msg.id)) {
				//jobs(msg.id) forward JobProtocol.GetStatus()
				this.jobs += (msg.id -> this.refreshStatus(jobs(msg.id)))
				sender() ! jobs(msg.id).status
			} else {
				//TODO: better error management using supervision
				sender() ! "unknown"
			}

		// refresh all jobs
		case msg: SchedulerProtocol.Refresh =>
			jobs.foreach { case (id, job) => this.jobs += (id -> this.refreshStatus(job)) }

		// get a list back
		case msg: SchedulerProtocol.GetList =>
			sender() ! this.jobs

		// get a result back
		case msg: SchedulerProtocol.GetResult =>
			if (jobs.contains(msg.id)) {
				jobs(msg.id).ref forward JobProtocol.GetResult()
			} else {
				//TODO: better error management using supervision
				sender() ! "unknown"
			}

		// default catch-all
		case x => log.warning("Received unknown message: {}", x)
	}

	def refreshStatus(job: JobRef): JobRef = {
		JobRef(job.ref, job.id, job.name, Await.result(job.ref ? JobProtocol.GetStatus(), Duration.Inf).asInstanceOf[String])
	}

	def getService(name: String): GenericAnalyticService = {
		name match {
			case "DistinctMimes" => new DistinctMimes()
			case _ => throw new unknownService()
		}
	}

	def getEngine(name: String): ActorRef = {
		Await.result(analyticEngineManager ? AnalyticEngineManagerProtocol.GetEngine(name), Duration.Inf).asInstanceOf[ActorRef]
	}
}

case class unknownService() extends Exception
