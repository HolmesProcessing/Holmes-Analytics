package com.holmesprocessing.analytics.actors

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.sys.process._

import akka.actor.{ Props }

import com.holmesprocessing.analytics.types.{GenericAnalyticEngine}

/** Factory for [[actors.AnalyticEngineSpark]] actors. */
object AnalyticEngineSpark {
	def props(sparkSubmitPath: String, cmdArgs: String): Props = Props(new AnalyticEngineSpark(sparkSubmitPath, cmdArgs))

}

/** Actor that provides access to Spark via the [[types.GenericAnalyticEngine]] trait.
 *
 *  @constructor create a new actor with a given spark-submit path and cmd args.
 *  @param sparkSubmitPath the full path to the spark-submit binary
 *  @param cmdArgs all cmd args as one concated string
 */
class AnalyticEngineSpark(sparkSubmitPath: String, cmdArgs: String) extends GenericAnalyticEngine {
	override def preStart(): Unit = log.info("AnalyticEngineSpark started")
	override def postStop(): Unit = log.info("AnalyticEngineSpark stopped")

	/** Submits the file found in objPath to spark-submit and monitors the execution
	 * by sending the output to [[actors.AnalyticEngineSpark#parseLine]].
	 * 
	 * @param objPath full path to the file passed to spark-submit
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

	/** Parse the service output and update status as well as result. */
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
