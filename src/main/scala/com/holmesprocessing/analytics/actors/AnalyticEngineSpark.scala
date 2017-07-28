package com.holmesprocessing.analytics.actors

import java.io.File
import java.util.UUID
import java.net.URI

import scala.collection.mutable.HashMap
import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.sys.process._

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import com.cloudera.livy.LivyClientBuilder
import com.cloudera.livy.scalaapi._

import com.holmesprocessing.analytics.types.{GenericAnalyticEngine}


object AnalyticEngineSpark {
	def props(sparkSubmitPath: String, cmdArgs: String): Props = Props(new AnalyticEngineSpark(sparkSubmitPath, cmdArgs))

}

class AnalyticEngineSpark(sparkSubmitPath: String, cmdArgs: String) extends GenericAnalyticEngine {
	override def preStart(): Unit = log.info("AnalyticEngineSpark started")
	override def postStop(): Unit = log.info("AnalyticEngineSpark stopped")

	/** Submits a job to the execution queue. This will _not_ run the job directly if there
	 * aren't enough worker threads availible.
	 */
	def execute(objPath: String): Boolean = {
		val command = sparkSubmitPath + " " + cmdArgs + " " + objPath
		
		//TODO: Save last ~20 lines as log?
		//use future so process execution is non-blocking
		val processRun: Future[Unit] = Future {
			val p = Process(command).lineStream(ProcessLogger(line => this.parseLine(line), err => this.parseLine(err)))
		}

		true
	}

	def stop(): Boolean = {
		//currently not possible, refer to lineStream documentation
		//this.process.destroy()
		false
	}

	def parseLine(line: String): Unit = {
		log.info(line)

		if(!line.startsWith("===HOLMESANALYTICS:")){
			return;
		}

		val resultsPattern = """^===HOLMESANALYTICS:RESULTS:(.*)""".r
		line match {
			case "===HOLMESANALYTICS:START" => this.status = "Running"
			case "===HOLMESANALYTICS:DONE" => this.status = "Finished"
			case resultsPattern(res) => this.result += res
		}
	}
}
