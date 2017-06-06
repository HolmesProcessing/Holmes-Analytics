package group.holmes.analytics.actors

import java.util.UUID

import akka.actor._
import com.rabbitmq.client._
import scala.concurrent.duration.{ FiniteDuration, _ }
import com.typesafe.config.Config

case class RabbitMessage(deliveryTag: Long, body: Array[Byte])

object RabbitConsumer {
	def props(cfg: Config): Props = Props(new RabbitConsumer(cfg))
}

class RabbitConsumer(cfg: Config) extends Actor with ActorLogging {
	//TODO: switch to https://github.com/NewMotion/akka-rabbitmq

	var channel: Channel = _

	val host = cfg.getConfig("host")
	val exchange = cfg.getConfig("exchange")
	val queue = cfg.getConfig("queue")

	val factory: ConnectionFactory = new ConnectionFactory()
	factory.setHost(host.getString("host"))
	factory.setPort(host.getInt("port"))
	factory.setUsername(host.getString("user"))
	factory.setPassword(host.getString("password"))
	factory.setVirtualHost(host.getString("vhost"))

	val connection = factory.newConnection()
	this.channel = connection.createChannel()

	this.channel.exchangeDeclare(
		exchange.getString("exchangeName"),
		exchange.getString("exchangeType"),
		exchange.getBoolean("durable")
		)
	
	this.channel.queueDeclare(queue.getString("queueName"),
		queue.getBoolean("durable"),
		queue.getBoolean("exclusive"),
		queue.getBoolean("autodelete"),
		null
		)

	this.channel.queueBind(
		queue.getString("queueName"),
		exchange.getString("exchangeName"),
		queue.getString("routingKey")
		)

	channel.basicQos(3)

	val consumer = new DefaultConsumer(this.channel) {
		override def handleDelivery(
			consumerTag: String,
			envelope: Envelope,
			properties: AMQP.BasicProperties,
			body: Array[Byte]) = {
			log.info("handle delivery {}, {}, {}", envelope.getDeliveryTag, envelope.isRedeliver, channel.hashCode())

			self ! new RabbitMessage(envelope.getDeliveryTag, body)
		}
	}

	this.channel.basicConsume(queue.getString("queueName"), false, consumer)
	
	def receive = {
		case RabbitMessage(deliveryTag: Long, body: Array[Byte]) =>
			log.info("RabbitConsumer received {}", new String(body, "utf-8"))
		case msg =>
			log.error("RabbitConsumer has received a message it cannot match against on: {}", msg)
	}

	override def preStart() = {
		val reconnectionDelay: FiniteDuration = 10.seconds
	}
}
