package com.bigdataconcept.akka.stock.trade.account.kafka

import akka.actor.{Actor, ActorLogging, Props}
import com.bigdataconcept.akka.stock.trade.account.domain.KafkaProtocol.KafkaMessage


object KafkaPublisher{
   def prop(topic: String) : Props = Props(new KafkaPublisher(topic))
}
class KafkaPublisher(topic: String) extends Actor with ActorLogging {


  override def receive: Receive = {
    case kafkaMsg : KafkaMessage =>
  }
}
