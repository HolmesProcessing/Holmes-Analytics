package group.holmes.analytics.actors

import java.util.UUID

import akka.actor._
import com.rabbitmq.client._
import scala.concurrent.duration.{ FiniteDuration, _ }
import com.typesafe.config.Config

case class RabbitMessage(deliveryTag: Long, body: Array[Byte])

object RabbitConsumer {
	def props(cfg: Config, scheduler: ActorRef): Props = Props(new RabbitConsumer(cfg, scheduler))
}

class RabbitConsumer(cfg: Config, scheduler: ActorRef) extends Actor with ActorLogging {
	//TODO: switch to https://github.com/NewMotion/akka-rabbitmq

	var channel: Channel = _

	val host = cfg.getConfig("host")
	val exchange = cfg.getConfig("exchange")
	val queue = cfg.getConfig("consumequeue")

	val factory: ConnectionFactory = new ConnectionFactory()
	factory.setHost(host.getString("server"))
	factory.setPort(host.getInt("port"))
	factory.setUsername(host.getString("username"))
	factory.setPassword(host.getString("password"))
	factory.setVirtualHost(host.getString("vhost"))

	val connection = factory.newConnection()
	this.channel = connection.createChannel()

	this.channel.exchangeDeclare(
		exchange.getString("name"),
		exchange.getString("type"),
		exchange.getBoolean("durable")
		)
	
	this.channel.queueDeclare(queue.getString("name"),
		queue.getBoolean("durable"),
		queue.getBoolean("exclusive"),
		queue.getBoolean("autodelete"),
		null
		)

	this.channel.queueBind(
		queue.getString("name"),
		exchange.getString("name"),
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

	this.channel.basicConsume(queue.getString("name"), false, consumer)
	
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
