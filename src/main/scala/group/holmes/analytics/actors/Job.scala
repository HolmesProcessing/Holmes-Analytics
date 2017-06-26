package group.holmes.analytics.actors

import java.util.UUID

import scala.collection.mutable.HashMap

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.typesafe.config.ConfigFactory

object Job {
	def props(name: String, analyticEngine: ActorRef, analyticService: ActorRef, parameters: HashMap[String, String]): Props = Props(new Job(UUID.randomUUID(), name, analyticEngine, analyticService, parameters))

	// Message used to get the id of this Job
	final case class Id()

	// Message used to get the status of this Job
	final case class Status()
}

class Job(id: UUID, name: String, analyticEngine: ActorRef, analyticService: ActorRef, parameters: HashMap[String, String]) extends Actor with ActorLogging {
	private var status: String = "Halted"

	override def preStart(): Unit = log.info("Job " + name + " started")
	override def postStop(): Unit = log.info("Job " + name + " stopped")

	override def receive = {
		// request the id of this job
		case msg: Job.Id =>
			sender() ! this.id

		// request to get the status of this job
		case msg: Job.Status =>
			sender() ! this.status

		case x => log.warning("Received unknown message: {}", x)
	}

	def start(): Unit = {
		//TODO: Kick-off service

		this.status = "Running"
	}

	def stop(): Unit = {
		//TODO: stop running service

		this.status = "Halted"
	}

	def refresh(): Unit = {
		//TODO: Manually refresh the status 

		this.status = "\"Refreshed\""
	}

}
