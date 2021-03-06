package com.holmesprocessing.analytics.actors

import java.util.UUID

import scala.concurrent.duration._
import scala.concurrent.Await

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.pattern.ask
import akka.util.Timeout

import com.holmesprocessing.analytics.types.{GenericAnalyticService}

import com.holmesprocessing.analytics.services.distinctmimes.DistinctMimes

/** CC to keep track of a running job. */
final case class JobRef(ref: ActorRef, id: UUID, name: String, status: String)
final case class JobRefMap(m: Map[UUID, JobRef])

/** Factory for [[actors.Scheduler]] actors. */
object Scheduler {
	def props(analyticEngineManager: ActorRef, servicesPath: String): Props = Props(new Scheduler(analyticEngineManager, servicesPath))
}

/** Holds all messages accepted by [[actors.Scheduler]] */
object SchedulerProtocol {
	final case class New(name: String, engine: String, service: String, parameters: Map[String, String])
	final case class GetStatus(id: UUID)
	final case class Refresh()
	final case class GetList()

	final case class GetJob(id: UUID)
	final case class DeleteJob(id: UUID)
	final case class GetResult(id: UUID)
}

/** This actor creates new [[actors.Job]] actors and keeps track of them.
 *
 *  @param analyticEngineManager ActorRef to the [[actors.AnalyticEngineManager]] actor.
 *  @param servicesPath path to the folder containing all services
 */
class Scheduler(analyticEngineManager: ActorRef, servicesPath: String) extends Actor with ActorLogging {
	private var jobs = Map.empty[UUID, JobRef]
	implicit val timeout: Timeout = 10.seconds //TODO: Discuss sensible value / error mitigation strategy

	override def preStart(): Unit = log.info("Scheduler started")
	override def postStop(): Unit = log.info("Scheduler stopped")

	override def receive = {
		// request to create a new job
		case msg: SchedulerProtocol.New =>
			val id = UUID.randomUUID()
			val service = getService(msg.service)

			//TODO: does the service or the user decide which engine to use?
			val ref = context.actorOf(
				Job.props(
					id,
					msg.name,
					getEngine(service.engine),
					service,
					servicesPath,
					msg.parameters))

			jobs += (id -> JobRef(ref, id, msg.name, "unknown"))
			sender() ! id

		// request to get the status of a job
		case msg: SchedulerProtocol.GetStatus =>
			if (jobs.contains(msg.id)) {
				//jobs(msg.id) forward JobProtocol.GetStatus()
				jobs += (msg.id -> refreshStatus(jobs(msg.id)))
				sender() ! jobs(msg.id).status
			} else {
				//TODO: better error management using supervision
				sender() ! "unknown id"
			}

		// refresh all jobs
		case msg: SchedulerProtocol.Refresh =>
			jobs.foreach { case (id, job) => jobs += (id -> refreshStatus(job)) }

		// get a list back
		case msg: SchedulerProtocol.GetList =>
			sender() ! JobRefMap(jobs)

		// get a job back
		case msg: SchedulerProtocol.GetJob =>
			if (jobs.contains(msg.id)) {
				jobs += (msg.id -> refreshStatus(jobs(msg.id)))
				sender ! jobs(msg.id)
			} else {
				//TODO: better error management using supervision
				sender() ! "unknown id"
			}

		// get a result back
		case msg: SchedulerProtocol.GetResult =>
			if (jobs.contains(msg.id)) {
				jobs(msg.id).ref forward JobProtocol.GetResult()
			} else {
				//TODO: better error management using supervision
				sender() ! "unknown id"
			}

		// delete a job
		case msg: SchedulerProtocol.DeleteJob =>
			if (jobs.contains(msg.id)) {
				context stop jobs(msg.id).ref
				jobs -= msg.id
				sender() ! "done"
			} else {
				//TODO: better error management using supervision
				sender() ! "unknown id"
			}

		// default catch-all
		case x => log.warning("Received unknown message: {}", x)
	}

	/** Refreshes the status of a JobRef and returns the refreshed one. */
	def refreshStatus(job: JobRef): JobRef = {
		JobRef(job.ref, job.id, job.name, Await.result(job.ref ? JobProtocol.GetStatus(), Duration.Inf).asInstanceOf[String])
	}

	/** Creates a new instance of the named [[types.GenericAnalyticService]]. */
	def getService(name: String): GenericAnalyticService = {
		name match {
			case "DistinctMimes" => new DistinctMimes()
			case _ => throw new unknownService()
		}
	}

	/** Creates a new actor of the named [[types.GenericAnalyticEngine]]. */
	def getEngine(name: String): ActorRef = {
		Await.result(analyticEngineManager ? AnalyticEngineManagerProtocol.GetEngine(name), Duration.Inf).asInstanceOf[ActorRef]
	}
}

case class unknownService() extends Exception
