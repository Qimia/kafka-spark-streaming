package io.qimia.kafka.simulator.actors

import akka.actor.{Actor, ActorLogging, Props}
import io.qimia.kafka.models.VehicleData
import io.qimia.kafka.simulator.utils.KafkaClient
import org.apache.kafka.clients.producer.KafkaProducer

import scala.util.Random

object WorkerActor {
  def props(geoStore: Seq[(Double, Double)], brokers: String, topic: String, schemaUrl: String): Props =
    Props(new WorkerActor(geoStore, brokers, topic, schemaUrl))

  def name = "worker"

  case class Move(event: CarEvent)

  case class Book(event: CarEvent)

  case class Notify(event: CarEvent)

  case class Block(event: CarEvent)

  case class Response(event: CarEvent)

}

class WorkerActor(geoStore: Seq[(Double, Double)], brokers: String, topic: String, schemaUrl: String)
    extends Actor
    with ActorLogging {

  import CarEvent._
  import WorkerActor._

  implicit val kproducer: KafkaProducer[String, VehicleData] = KafkaClient.producer(brokers, schemaUrl)
  val random: Random.type                                    = Random
  val geolocs: Int                                           = geoStore.size

  def receive: Receive = {
    case Move(e) => {
      implicit val event: CarEvent = e
      if (event.expire > 0) {
        val loc = geoStore(random.nextInt(geolocs))
        log.debug("### Clock: " + event.clock + ", moved vin:" + event.vin + " to:" + loc)
        send(move(loc), topic)
      } else {
        log.debug("### Clock: " + event.clock + ", available vin:" + event.vin)
        send(transition("available", 0), topic)
      }
    }
    case Book(event) => {
      log.debug("### Clock: " + event.clock + ", booked vin:" + event.vin)
      send(transition("booked", random.nextInt(50))(event), topic)
      log.debug("*** Clock: " + event.clock + ", booked vin:" + event.vin)
    }
    case Notify(e) => {
      implicit val event: CarEvent = e
      if (event.block > 0) {
        sender ! Response(block(event.block - 1))
      } else {
        send(clock, topic)
      }
    }
    case Block(event) => {
      sender ! Response(block(random.nextInt(5))(event))
    }

  }

  def send(event: CarEvent, topic: String)(implicit producer: KafkaProducer[String, VehicleData]): Unit = {
    KafkaClient.send(event, topic, producer)
    sender ! Response(event)
    log.debug("### Clock: " + event.clock + ", RESPONSE:" + event.vin)
  }

}
