package com.holmesprocessing.analytics

import akka.actor.ActorSystem

import com.holmesprocessing.analytics.actors._


object HolmesAnalytics extends App {
	val system = ActorSystem("analytics")
	import system.dispatcher

	val cfgPath: String = if (args.length > 0) { args(0) } else { "./config/analytics.conf" }

	val supervisor = system.actorOf(AnalyticsSupervisor.props(cfgPath), "analytics-supervisor")
}
