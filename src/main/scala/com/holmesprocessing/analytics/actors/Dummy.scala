package com.holmesprocessing.analytics.actors

import akka.actor.{ Actor, ActorLogging, Props }

/** Factory for [[actors.Dummy]] actors. */
object Dummy {
	def props(name: String): Props = { Props(new Dummy(name)) }
}

/** Dummy actor to be used as placehoder, has no function. */
class Dummy(name: String) extends Actor with ActorLogging {
	override def preStart(): Unit = log.info("Dummy \"{}\" started", name)
	override def postStop(): Unit = log.info("Dummy \"{}\" stopped", name)

	override def receive = {
		case x => log.info("Dummy \"{}\" received message: {}", name, x)
	}
}
