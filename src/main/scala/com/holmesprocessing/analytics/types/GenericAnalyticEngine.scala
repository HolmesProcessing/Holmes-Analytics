package com.holmesprocessing.analytics.types

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.pattern.ask


object AnalyticEngineProtocol {
	final case class GetStatus()
	final case class GetResult()
	final case class Stop()
	final case class Execute(objPath: String)
}

/** The trait all analyticEngine's / actors should support
 *
 *  This trait provides all necessary procedures that each analytic engine should support in some way.
 *  These functions are called and relied upon other actors, failure to implement them in your
 *  engine can result in unstable behaviour.
 *  
 *  All engine actors should directly initialize all their connections and needed contexts on 
 *  creation without any further method call to so.
 */
trait GenericAnalyticEngine extends Actor with ActorLogging {
	protected var status: String = "Waiting"
	protected var result: String = ""

	override def receive = {
		case msg: AnalyticEngineProtocol.GetStatus =>
			sender() ! this.status

		case msg: AnalyticEngineProtocol.GetResult =>
			sender() ! this.result

		case msg: AnalyticEngineProtocol.Stop =>
			this.stop()

		case msg: AnalyticEngineProtocol.Execute =>
			this.execute(msg.objPath)

		case x => log.warning("Received unknown message: {}", x)
	}

	/** Execute the obj using this engine . */
	def execute(objPath: String): Boolean

	/** Stops the execution (if possible). */
	def stop(): Boolean
}
