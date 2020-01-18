package com.bigdataconcept.akka.stock.trade.portfolio.kafka

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.Props
import org.apache.kafka.common.serialization.StringSerializer
import akka.kafka.ProducerSettings
import org.apache.kafka.clients.producer.ProducerRecord
import com.bigdataconcept.akka.stock.trade.portfolio.domain.KafkaProtocol.KafkaMessage


object KafkaPublisher{
    def prop(topic: String) : Props = Props(new KafkaPublisher(topic))
}
class  KafkaPublisher(topic: String) extends Actor with ActorLogging{

    //context.system.eventStream.subscribe(self, classOf[KafkaMessage])

  override def receive : Receive = {

    case msg: KafkaMessage =>  sendMessageToKafka(topic,msg.payload)
  }



  def sendMessageToKafka(topic: String, payload: String) {

    log.info("Sending Message To Kafka Broker Payload Event{} Topic {}", payload, topic);

    val producerSettings = ProducerSettings.create(context.system, new StringSerializer, new StringSerializer)

    val kafkaProducer = producerSettings.createKafkaProducer()
    val messageRecord = new ProducerRecord[String, String](topic, payload)
    kafkaProducer.send(messageRecord)
    kafkaProducer.close()

  }
}
