package com.bigdataconcept.akka.stock.trade.account.kafka

import akka.actor.{Actor, ActorLogging, Props}
import akka.kafka.ProducerSettings
import com.bigdataconcept.akka.stock.trade.account.domain.KafkaProtocol
import com.bigdataconcept.akka.stock.trade.account.domain.KafkaProtocol.KafkaMessage
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer


object KafkaPublisher{
   def prop(topic: String) : Props = Props(new KafkaPublisher(topic))
}
class KafkaPublisher(topic: String) extends Actor with ActorLogging {


  override def receive: Receive = {
    case kafkaMsg : KafkaMessage  => sendMessageToKafka(topic,kafkaMsg.payload, kafkaMsg.msgType)
  }


  def sendMessageToKafka(topic: String, payload: String, msgType: String) {

    log.info("Sending Message To Kafka Broker Payload Event{} Topic {}", payload, topic);

    val producerSettings = ProducerSettings.create(context.system, new StringSerializer, new StringSerializer)
    val kafkaProducer = producerSettings.createKafkaProducer()
    val messageRecord = new ProducerRecord[String, String](topic, payload)
     messageRecord.headers().add(KafkaProtocol.MSGTYPE, msgType.getBytes)
     kafkaProducer.send(messageRecord)
     kafkaProducer.close()

  }
}
