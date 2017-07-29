import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark._

import com.datastax.spark.connector._


import org.apache.spark.rdd.RDD
import scala.collection.mutable.ArrayBuffer
import java.util.zip.{GZIPOutputStream, GZIPInputStream}
import scala.io.Source.fromInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import org.apache.commons.io.IOUtils


import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.cassandra._
import org.apache.spark.sql.functions.array_contains 
import org.apache.spark.sql.Row

object DistinctMimes extends App {

	val spark = SparkSession.builder
	.appName("DistinctMimes")
	.enableHiveSupport()
	.getOrCreate()

	import spark.implicits._

	System.err.println("===HOLMESANALYTICS:START");
	
	//TODO: Pass keyspace name via ENV
	val objects = spark.sparkContext.cassandraTable("gsoc1","objects")
	.map(x=> (x.get[String]("sha256"),x.get[Option[String]]("md5"),x.get[Option[String]]("file_mime"),x.get[Seq[String]]("file_name"),x.get[Option[String]]("sha1"),x.get[Seq[String]]("source"),x.get[Seq[String]]("submissions")))
	.map(x=> (x._1,x._2.getOrElse(""),x._3.getOrElse("").split(",")(0),x._4.mkString(", "),x._5.getOrElse(""),x._6.mkString(", "),x._7.mkString(", "))).toDF("sha256","md5","mime","file_name","sha1","source","submissions")

	objects.registerTempTable("objects")
	objects.cache()

	val distinctMimes = spark
	.sql("select count(distinct mime) as value from objects")
	.collect()(0).getLong(0)

	System.err.println("===HOLMESANALYTICS:RESULTS:"+distinctMimes);

	System.err.println("===HOLMESANALYTICS:DONE");

	spark.stop()
	sys.exit(0)
}
