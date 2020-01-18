package com.bigdataconcept.akka.stock.trade.portfolio.kafka

import akka.actor.{Actor, ActorLogging}
import akka.stream.ActorMaterializer
import akka.actor.Actor
import akka.kafka.ConsumerSettings
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import org.apache.kafka.common.serialization.StringDeserializer
import scala.concurrent.Future
import akka.Done
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerRecord
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContext



abstract class  KafkaSubcriberActor(topic: String, groupId: String) extends Actor with ActorLogging  {

  implicit val mat = ActorMaterializer()
  implicit val dispacter: ExecutionContext = context.system.dispatcher

  def startConsumingEvent(): Unit = {
    log.info("Start Consuming Events from Kakfa Topic {} GroupId {} ", topic, groupId)
    val consumerSettings = ConsumerSettings.create(context.system, new StringDeserializer, new StringDeserializer)
      .withGroupId(groupId)
    Consumer.plainSource(consumerSettings, Subscriptions.topics(topic)).mapAsync(5)(handleEvent)
      .runWith(Sink.ignore)
  }

  def handleEvent(event: ConsumerRecord[String, String]): Future[Done]
}
