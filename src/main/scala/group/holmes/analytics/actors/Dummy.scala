package group.holmes.analytics.actors

import akka.actor.{ Actor, ActorLogging, Props }

object Dummy {
	def props(name: String): Props = { Props(new Dummy(name)) }
}

class Dummy(name: String) extends Actor with ActorLogging {
	override def preStart(): Unit = log.info("Dummy \"{}\" started", name)
	override def postStop(): Unit = log.info("Dummy \"{}\" stopped", name)

	override def receive = {
		case x => log.info("Dummy \"{}\" received message: {}", name, x)
	}
}
