name := "Holmes-Analytics"
version := "0.1"
organization := "group.holmes"

scalaVersion := "2.12.2"


resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
//this should only be a temporary fix due to mismatching sha1 sums
resolvers += "JBoss" at "https://repository.jboss.org/"


libraryDependencies ++= {
	Seq(
		"com.typesafe.akka"   %% "akka-actor"              % "2.5.2",
		"com.typesafe.akka"   %% "akka-stream"             % "2.5.2",
		"com.typesafe.akka"   %% "akka-slf4j"              % "2.5.2",
		"com.typesafe.akka"   %% "akka-http"               % "10.0.7",
		"com.typesafe.akka"   %% "akka-http-spray-json"    % "10.0.7",
		"ch.qos.logback"      %  "logback-classic"         % "1.2.3",
		"io.getquill"         %% "quill-cassandra"         % "1.2.1",
		"joda-time"           % "joda-time"                % "2.9.7",
		"com.rabbitmq"        % "amqp-client"              % "3.4.2"
		)
}
