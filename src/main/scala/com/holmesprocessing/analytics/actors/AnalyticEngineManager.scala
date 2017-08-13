package com.holmesprocessing.analytics.actors

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.typesafe.config.Config

/** Factory for [[actors.AnalyticEngineManager]] actors. */
object AnalyticEngineManager {
	def props(cfg: Config): Props = { Props(new AnalyticEngineManager(cfg)) }
}

/** Holds all messages accepted by [[actors.AnalyticEngineManager]] */
object AnalyticEngineManagerProtocol {
	final case class GetEngine(name: String)
}

/** Actor that creates and manages AnalyticEngine* Actors.
 *
 * This actor creates new AnalyticEngine* Actors on demand. A job may ask this actor to provide him
 * with a engine to exectue on. All provided engines implement GenericAnalyticEngine.
 *
 *  @constructor create a new manager with a certain config.
 *  @param cfg config containing settings for all engines
 */
class AnalyticEngineManager(cfg: Config) extends Actor with ActorLogging {
	//TODO: Check these beforehand to make sure they exist
	/** All arguments passed to spark-submit */
	val sparkArgs = """--packages datastax:spark-cassandra-connector:%s --conf spark.cassandra.connection.host=%s --conf spark.cassandra.auth.username=%s --conf spark.cassandra.auth.password=%s""".format(
			cfg.getString("spark.spark-cassandra-connector"),
			cfg.getString("cassandra.host"),
			cfg.getString("cassandra.username"),
			cfg.getString("cassandra.password")
		)

	
	override def preStart(): Unit = log.info("AnalyticEngineManager started")
	override def postStop(): Unit = log.info("AnalyticEngineManager stopped")

	override def receive = {
		case msg: AnalyticEngineManagerProtocol.GetEngine =>
			sender() ! this.getEngine(msg.name)

		case x => log.warning("Received unknown message: {}", x)
	}

	/** Creates a new actor based on the engine name and returns the matching ActorRef */
	def getEngine(name: String): ActorRef = {
		name match {
			case "spark" => context.actorOf(
				AnalyticEngineSpark.props(
					cfg.getString("spark.sparkSubmitPath"),
					sparkArgs))
			case _ => throw new unknownEngine()
		}
	}
}

/** Thrown if desired engine is unknown */
case class unknownEngine() extends Exception
