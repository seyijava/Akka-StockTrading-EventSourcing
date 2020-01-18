package com.bigdataconcept.akka.stock.trade.order.kafka
import akka.Done
import akka.actor.{ActorRef, Props}
import com.bigdataconcept.akka.stock.trade.order.domain.Kafka.OrderPlacedEvent
import com.google.gson.Gson
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.Future
import com.bigdataconcept.akka.stock.trade.order.domain.Commands.{OrderCommand, PlaceOrderCommand}
import com.bigdataconcept.akka.stock.trade.order.domain.Domains.Order

/**
 * @author Oluwaseyi Otun
 *         Trade Order Subscriber class It implements handleEvent method of KafkaSubscriberActor
 */
object TradeOrderPubSubActor{
     def props(topic: String, groupId: String, orderShardingRegion: ActorRef) : Props = Props(new TradeOrderPubSubActor(topic,groupId,orderShardingRegion))
}
class TradeOrderPubSubActor(topic: String, groupId: String, orderShardingRegion: ActorRef) extends  KafkaSubscriberActor(topic,groupId)
{

  val PLACE_ORDER_EVENT_TYPE = "PLACEORDER"

  /**
   * Starting kafka consumer
   */
  override def preStart(): Unit = {
    log.info("Start consuming Trade Order Events")
    startConsumingEvent()
  }


  override def receive: Receive = {
    case _ => "Not Implemented"
  }

  /**
   *
   * @param event
   * @return
   *         Handle incoming event coming from kafka topic
   *
   */
  override def handleEvent(event: ConsumerRecord[String, String]): Future[Done] = {

    val payload = event.value()
    log.info("Handle  Incoming Payload Event {}  ", payload )

    val  orderPlacedEvent = new  Gson().fromJson(payload, classOf[OrderPlacedEvent])
    log.info("Handle  Event {}  ", orderPlacedEvent )
    val order = Order(orderPlacedEvent.orderId,orderPlacedEvent.portfolioId,orderPlacedEvent.orderDetails)
    val placeOrderCommand = PlaceOrderCommand(orderPlacedEvent.orderId,orderPlacedEvent.portfolioId,order)

    orderShardingRegion forward  placeOrderCommand
    log.info("Sent Place Order Command to OrderActorEntity {}  ", placeOrderCommand )
    return Future.successful(Done)
  }
}
