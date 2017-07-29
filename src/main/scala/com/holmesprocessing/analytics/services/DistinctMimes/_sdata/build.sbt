name := "DistinctMimes"

version := "1.0"

scalaVersion := "2.11.8"

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
	artifact.name + "." + artifact.extension
}

val sparkVersion = "2.0.2"
val connectorVersion = "2.0.3"

val cassandraVersion = "3.1"
resolvers += "Spark Packages Repo" at "https://dl.bintray.com/spark-packages/maven"

libraryDependencies ++= Seq(
	"org.apache.spark" %% "spark-core" % sparkVersion % "provided",
	"org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
	"org.apache.spark" %% "spark-hive" % sparkVersion % "provided",
	"com.datastax.spark" %% "spark-cassandra-connector" % connectorVersion % "provided"
)
