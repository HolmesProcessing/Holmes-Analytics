package com.holmesprocessing.analytics.types

/** Trait for all services in the service/ directory. */
trait GenericAnalyticService {
	//meta information
	val engine: String
	val name: String

	protected var isBuild: Boolean = false

	/** Builds/Prepares the service if necessary. */
	def build(servicesPath: String): Boolean

	/** Returns the path to the service package/file. */
	def getObjPath(servicesPath: String): String
}
