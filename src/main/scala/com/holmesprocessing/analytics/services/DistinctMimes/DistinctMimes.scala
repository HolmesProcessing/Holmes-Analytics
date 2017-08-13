package com.holmesprocessing.analytics.services.distinctmimes

import sys.process._

import com.holmesprocessing.analytics.types.{GenericAnalyticService}

/** Service returning the number of distinct mime types in the database. */
class DistinctMimes() extends GenericAnalyticService {
	val name: String = "DistinctMimes"
	val engine: String = "spark"

	def build(servicesPath: String): Boolean = {
		Process(Seq("sbt","package"), new java.io.File(servicesPath + "/DistinctMimes/_sdata")).! match {
			case 0 => this.isBuild = true; true
			case _ => false
		}
	}

	def getObjPath(servicesPath: String): String = {
		if(!isBuild){
			this.build(servicesPath)
		}

		"--class DistinctMimes %s/DistinctMimes/_sdata/target/scala-2.11/distinctmimes.jar".format(servicesPath)
	}
}
