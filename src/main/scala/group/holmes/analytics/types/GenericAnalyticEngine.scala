package group.holmes.analytics.types

import java.util.UUID


/** The trait all analyticEngine's / actors should support
 *
 *  This trait provides all necessary procedures that each analytic engine should support in some way.
 *  These functions are called and relied upon other actors, failure to implement them in your
 *  engine can result in unstable behaviour.
 *  
 *  All engine actors should directly initialize all their connections and needed contexts on 
 *  creation without any further method call to so.
 */
trait GenericAnalyticEngine {
	/** Get the current status of the engine. Should return "Good", "Down", "Failed" or "Busy" */
	def getStatus(): String

	/** Execute function f. f should be specific to the engine, a UUID to track the execution should be returned. */
	def execute(f: => Any): UUID

	/** Stops a job by UUID either gracefully or hard. */
	def stop(id: UUID, force: Boolean): Boolean

	/** Shuts the engine and stops the actor */
	def shutdown: Unit
}
