package group.holmes.analytics.actors

import java.io.File
import java.util.UUID

import scala.collection.mutable.HashMap
import scala.concurrent.duration._
import scala.concurrent.Await

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

/** Factory for [[group.holmes.analytics.actors.Scheduler]] actors. */
object Scheduler {
	/** Creates a Scheduler with:
	 *
	 *  @param analyticEngineManager
	 *  @param analyticServiceManager
	 */
	def props(analyticEngineManager: ActorRef, analyticServiceManager: ActorRef): Props = Props(new Scheduler(analyticEngineManager, analyticServiceManager))

	/** Messages */
	// Message used to create a new Job Actor
	final case class New(name: String, engine: String, service: String, parameters: HashMap[String, String])

	// Message used to get the status of a Job
	final case class Status(id: UUID)

	// Force a refresh of all job statuses
	final case class Refresh()

	// Get a list of all jobs
	final case class List()


	/** Exposed ccs */
	// Job object to track and keep state
	final case class ScheduledJob(ref: ActorRef, id: UUID, name: String, status: String)
}

class Scheduler(analyticEngineManager: ActorRef, analyticServiceManager: ActorRef) extends Actor with ActorLogging {
	private val jobs = HashMap.empty[UUID, Scheduler.ScheduledJob]
	implicit val timeout: Timeout = 10.seconds //TODO: Discuss sensible value / error mitigation strategy

	override def preStart(): Unit = log.info("Scheduler started")
	override def postStop(): Unit = log.info("Scheduler stopped")

	override def receive = {
		// request to create a new job
		case msg: Scheduler.New =>
			val ref = context.actorOf(Job.props(msg.name, this.getEngine(msg.engine), this.getService(msg.service), msg.parameters))
			val id = Await.result(ref ? Job.Id(), Duration.Inf).asInstanceOf[UUID] //TODO: Discuss sensible value / error mitigation strategy
			val status = Await.result(ref ? Job.Status(), Duration.Inf).asInstanceOf[String] //TODO: Discuss sensible value / error mitigation strategy
			this.jobs += (id -> Scheduler.ScheduledJob(ref, id, msg.name, status))
			sender() ! id

		// request to get the status of a job
		case msg: Scheduler.Status =>
			if (jobs.contains(msg.id)) {
				//jobs(msg.id) forward Job.Status()
				this.jobs += (msg.id -> this.refreshStatus(jobs(msg.id)))
				sender() ! jobs(msg.id).status
			} else {
				//TODO: better error management using supervision
				sender() ! "unknown"
			}

		// refresh all jobs
		case msg: Scheduler.Refresh =>
			jobs.foreach { case (id, job) => this.jobs += (id -> this.refreshStatus(job)) }

		// get a list back
		case msg: Scheduler.List =>
			sender() ! this.jobs

		// default catch-all
		case x => log.warning("Received unknown message: {}", x)
	}

	def refreshStatus(job: Scheduler.ScheduledJob): Scheduler.ScheduledJob = {
		Scheduler.ScheduledJob(job.ref, job.id, job.name, Await.result(job.ref ? Job.Status(), Duration.Inf).asInstanceOf[String])
	}

	def getService(name: String): ActorRef = {
		//TODO: This function will ask the analyticServiceManager for the reference to the
		//analyticService Actor and return it.

		val analyticService: ActorRef = context.actorOf(Dummy.props("analyticService"))
		analyticService
	}

	def getEngine(name: String): ActorRef = {
		//TODO: This function will ask the analyticEngineManager for the reference to the
		//analyticEngine Actor and return it.

		val analyticEngine: ActorRef = context.actorOf(Dummy.props("analyticEngine"))
		analyticEngine
	}
}
