package com.bigdataconcept.akka.stock.trade.order.kafka

import akka.actor.{Actor, ActorLogging, Props}
import akka.kafka.ProducerSettings
import com.bigdataconcept.akka.stock.trade.order.domain.KafkaProtocol.KafkaMessage
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

/**
 * @author Oluwaseyi Otun
 *         Kafka Publisher Actor. This class model kafka Producer.
 *         It is used to send event to kafka topic.
 */

object KafkaPublisherActor{
    def props(topic: String) : Props = Props(new KafkaPublisherActor(topic))
}
class KafkaPublisherActor(topic: String) extends Actor with ActorLogging{


  /**
   *
   * @return This method receive incoming messages from the Actor mail box.
   */
  override def receive : Receive = {

    case msg: KafkaMessage =>  sendMessageToKafka(topic,msg.payload,msg.msgType)
  }


  /**
   *
   * @param topic
   * @param payload
   * @param msgType
   *                This method send kafka message to topic.
   */

  def sendMessageToKafka(topic: String, payload: String, msgType: String) {

    log.info("Sending Message To Kafka Broker Payload Event{} Topic {}", payload, topic);

    val producerSettings = ProducerSettings.create(context.system, new StringSerializer, new StringSerializer)

    val kafkaProducer = producerSettings.createKafkaProducer()
    var messageRecord = new ProducerRecord[String, String](topic, payload)
    messageRecord.headers().add("EVENT_TYPE",msgType.getBytes)
    kafkaProducer.send(messageRecord)
    kafkaProducer.close()

  }
}



