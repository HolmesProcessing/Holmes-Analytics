package com.holmesprocessing.analytics.actors

import java.io.File

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.typesafe.config.Config


object AnalyticEngineManager {
	def props(cfg: Config): Props = { Props(new AnalyticEngineManager(cfg)) }
}

object AnalyticEngineManagerProtocol {
	final case class GetEngine(name: String)
}

class AnalyticEngineManager(cfg: Config) extends Actor with ActorLogging {
	//TODO: Check these beforehand to make sure they exist
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

case class unknownEngine() extends Exception
