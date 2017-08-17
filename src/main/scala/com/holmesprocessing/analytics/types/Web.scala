package com.holmesprocessing.analytics.types

import java.text.{ParseException, SimpleDateFormat}
import java.util.{UUID, Date}

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import spray.json._
import spray.json.DefaultJsonProtocol._

import com.holmesprocessing.analytics.actors.{JobRef}

/** API response for a successfull request. */
final case class APISuccess[T](status: String = "success", result: T)

/** API response for a failed request. */
final case class APIError(status: String = "failure", error: String)

/** Provides implicit JSON conversions. */
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
	implicit object UUIDFormat extends JsonFormat[UUID] {
		def write(uuid: UUID) = JsString(uuid.toString)
		def read(value: JsValue) = {
			value match {
				case JsString(uuid) => UUID.fromString(uuid)
				case _              => throw DeserializationException("Expected hexadecimal UUID string")
			}
		}
	}

	implicit object ActorRefFormat extends JsonFormat[ActorRef] {
		def write(ar: ActorRef) = JsString(ar.toString)
		def read(value: JsValue) = {
			value match {
				case _ => throw DeserializationException("Can't unmarshal ActorRef") //no possible without context
			}
		}
	}

	def parseIsoDateString(date: String): Option[Date] = {
		if (date.length != 28) None
		else try Some(localIsoDateFormatter.get().parse(date))
		catch {
			case p: ParseException => None
		}
	}

	private val localIsoDateFormatter = new ThreadLocal[SimpleDateFormat] {
		override def initialValue() = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
	}

	def dateToIsoString(date: Date) = localIsoDateFormatter.get().format(date)

	implicit object DateFormat extends JsonFormat[Date] {

		def write(date : Date) : JsValue = JsString(dateToIsoString(date))

		def read(json: JsValue) : Date = json match {

			case JsString(rawDate) => parseIsoDateString(rawDate) match {
				case None => deserializationError(s"Expected ISO Date format, got $rawDate")
				case Some(isoDate) => isoDate
			}

			case unknown => deserializationError(s"Expected JsString, got $unknown")
		}
	}

	implicit val JobRefFormat = jsonFormat4(JobRef)
	implicit val APISuccessUUIDFormat = jsonFormat2(APISuccess[UUID])
	implicit val APISuccessJobRefFormat = jsonFormat2(APISuccess[JobRef])
	implicit val APISuccessStringFormat = jsonFormat2(APISuccess[String])
	implicit val APISuccessMapUJFormat = jsonFormat2(APISuccess[Map[UUID, JobRef]])
	implicit val APIErrorFormat = jsonFormat2(APIError)
}
