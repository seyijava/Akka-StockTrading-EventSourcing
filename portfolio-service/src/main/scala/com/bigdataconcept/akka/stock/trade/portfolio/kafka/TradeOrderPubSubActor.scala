package com.bigdataconcept.akka.stock.trade.portfolio.kafka

import akka.Done
import akka.actor.{ActorRef, Props}
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Commands.{AcknowledgeOrderFailureCommand, CompleteTradeCommand}
import com.bigdataconcept.akka.stock.trade.portfolio.domain.kafkaEvent.{TradeCompletedEvent, TradeOrdeFailedEvent}
import com.google.gson.Gson
import org.apache.kafka.clients.consumer.ConsumerRecord
import scala.concurrent.Future

/**
 * @author Oluwaseyi otun
 *
 */
object TradeOrderPubSubActor{
   def prop(topic: String, groupId: String, portfolioShardRegion: ActorRef) : Props = Props(new TradeOrderPubSubActor(topic,groupId,portfolioShardRegion))
}
class TradeOrderPubSubActor(topic: String, groupId: String, portfolioShardRegion: ActorRef) extends  KafkaSubcriberActor(topic,groupId)
{
  val ORDER_COMPLETE_EVENT_TYPE = "COMPLETE"


  val ORDER_FAILED_EVENT_TYPE = "FAILED"

  /**
   * Starting kafka consumer
   */
  override def preStart(): Unit = {
    log.info("Start consuming Trade Order Events")
    startConsumingEvent()
  }

  override def receive: Receive = {
    case _ => "Nothing to Do"
  }

  /**
   *
   * @param event
   * @return
   *         Subscribe to incoming topic from kafka listening to kafka failed
   *         and completed Trade order events. Processing incoming Trade Order Service
   */
  override def handleEvent(event: ConsumerRecord[String, String]): Future[Done] = {
    val payload = event.value()
    log.info("Handle  Incoming Payload Event {}  ", payload )
    val messageHeaders = event.headers()
    var msgType = ""
     messageHeaders.forEach(header => {
       if(header.key().equals("EVENT_TYPE")){
         msgType = new String(header.value())
       }
     })
    if(msgType.equals(ORDER_COMPLETE_EVENT_TYPE)){
      log.info("Process Incoming TradeOrder Complete Event")
      val tradeCompletedEvent = new Gson().fromJson(payload, classOf[TradeCompletedEvent])
      val completeTradeCommand = new CompleteTradeCommand(tradeCompletedEvent.portfolioId,tradeCompletedEvent.orderId,tradeCompletedEvent.trade)
      portfolioShardRegion forward(completeTradeCommand)
    }
    else if(msgType.equals(ORDER_FAILED_EVENT_TYPE)){
      log.info("Process Incoming Failed TradeOrder  Event")
      val tradeOrderFailedEvent = new Gson().fromJson(payload,classOf[TradeOrdeFailedEvent])
      val acknowledgeOrderFailureCommand = AcknowledgeOrderFailureCommand(tradeOrderFailedEvent.portfolioId,tradeOrderFailedEvent.orderId)
      portfolioShardRegion forward(acknowledgeOrderFailureCommand)
    }

    return Future.successful(Done)
  }
}
