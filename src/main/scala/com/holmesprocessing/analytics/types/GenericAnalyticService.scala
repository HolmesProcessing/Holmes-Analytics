package com.holmesprocessing.analytics.types


// This is not an actor!
trait GenericAnalyticService {
	//meta information
	val engine: String
	val name: String

	protected var isBuild: Boolean = false

	def build(servicesPath: String): Boolean
	def getObjPath(servicesPath: String): String
}
