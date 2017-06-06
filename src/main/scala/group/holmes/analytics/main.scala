package group.holmes.analytics

import akka.actor.ActorSystem

import group.holmes.analytics.actors._


object HolmesAnalytics extends App {
	val system = ActorSystem("analytics")

	val cfgPath: String = if (args.length > 0) { args(0) } else { "./config/analytics.conf" }

	val supervisor = system.actorOf(AnalyticsSupervisor.props(cfgPath), "analytics-supervisor")
}
