package com.bigdataconcept.akka.stock.trade.order.actor

import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.actor.{ActorLogging, ActorRef, PoisonPill, Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import com.bigdataconcept.akka.stock.trade.order.domain.Domains.{OrderFailed, OrderFulfilled, OrderSummary, Pending}
import com.bigdataconcept.akka.stock.trade.order.domain.Events.{OrderEvent, OrderFailedEvent, OrderFulfilledEvent, OrderReceivedEvent}
import com.bigdataconcept.akka.stock.trade.order.domain.State.OrderState
import akka.persistence.journal.Tagged
import com.bigdataconcept.akka.stock.trade.order.domain.ApiPayload.OrderSummaryResponse
import com.bigdataconcept.akka.stock.trade.order.domain.Commands.{CompleteOrderCommand, GetOrderStatusCommand, GetOrderSummaryCommand, PlaceOrderCommand}
import com.bigdataconcept.akka.stock.trade.order.domain.KafkaProtocol.KafkaMessage
import com.google.gson.Gson

/**
 * @author Oluwaseyi Otun
 *         This class model Trade Order Aggregate Root. It is an event sourcing
 *         entity. It has processed to command Place order event raise in Portfolio
 *         service via the Kafka event bus. It subscribe to place order topic in kafka
 *         and forward the Place order command to Order ShardRegion Proxy for processing
 * */
 *
object OrderActorEntity{

   def props(messagePublisherActor: ActorRef) : Props = Props(new OrderActorEntity(messagePublisherActor))
}

class  OrderActorEntity(messagePublisherActor: ActorRef)  extends PersistentActor  with ActorLogging{
  /**
   *
   * @return
   *         All messages sent to the OrderEntity Actor message box will be received in ths method.
   */
  override def receiveCommand:  Receive = {

    case placeOrderCommand : PlaceOrderCommand => handlePlaceOrderCommand(placeOrderCommand)

    case orderCompletedCommand :  CompleteOrderCommand => handleOrderCompletedCommand(orderCompletedCommand)

    case orderStatusCmd: GetOrderStatusCommand => handleGetOrderStatus(orderStatusCmd)

    case orderSummaryCmd: GetOrderSummaryCommand => handleGetOrderSummary(orderSummaryCmd)

    case "snapshot" =>
      log.info("saving snapshot")
      saveSnapshot(orderState)

    case timeout: ReceiveTimeout  => passivate()
  }


  var orderState = new OrderState()
  val ORDER_COMPLETE_EVENT_TYPE = "COMPLETE"
  val ORDER_FAILED_EVENT_TYPE = "FAILED"
  override def persistenceId: String = if (orderState.orderId == None ) self.path.name else orderState.orderId.get

  override def receiveRecover: Receive = {

    case orderReceivedEvent: OrderReceivedEvent =>  applyEvent(orderReceivedEvent)

    case orderFailedEvent: OrderFailedEvent => applyEvent(orderFailedEvent)

    case orderFulfilEvent: OrderFulfilledEvent  => applyEvent(orderFulfilEvent)

    case offer@SnapshotOffer(_, snapshot: OrderState) =>
      log.info("recovering with snapshot: {}", offer)
      orderState = snapshot
  }

  /***
   *
   * @param cmd
   *            Handle place order command and raise Order Receive
   *            event to TradeService Actor
   */
  def handlePlaceOrderCommand(cmd: PlaceOrderCommand): Unit ={
      val order = cmd.order
      val orderReceivedEvent = OrderReceivedEvent(order)
      persist(orderReceivedEvent){evt=>
        orderState = applyEvent(evt)
        context.system.eventStream.publish(orderReceivedEvent.order)
        //publishMessage(order)
      }
    }

  /**
   *
   * @param cmd
   *            Handles completed order command raise an order fulfilment or Order failed event
   *            depending on the status of the order and publish to kafka to notify Portfolio Service order had been completed
   *
   *
   */
  def handleOrderCompletedCommand(cmd: CompleteOrderCommand): Unit ={
      log.info("Handle and completed order")
       val orderResult = cmd.orderResult
       if(orderResult.isInstanceOf[OrderFulfilled]){
         log.info("Processed OrderFulfilment order")
         val orderFulfilled = orderResult.asInstanceOf[OrderFulfilled]
         val orderFulfilledEvent = OrderFulfilledEvent(orderFulfilled.orderId,orderFulfilled.portfolioId,orderFulfilled.trade)
         persist(orderFulfilledEvent){evt=>
           applyEvent(evt)
           publishMessage(orderFulfilledEvent)
       }
       if (orderResult.isInstanceOf[OrderFailed]){
         log.info("Processed failed order order")
          val orderFailed = orderResult.asInstanceOf[OrderFailed]
          val orderFailedEvent =  OrderFailedEvent(orderFailed.orderId, orderFailed.portfolioId)
         persist(orderFailedEvent) { evt =>
           orderState = applyEvent(evt)
           publishMessage(orderFailedEvent)
         }
       }}

    }

  /**
   *
   * @param cmd
   *            Handle order status command by get the status of the order
   *
   */
    def handleGetOrderStatus(cmd: GetOrderStatusCommand): Unit ={

    }

  /**
   *
   * @param cmd
   *            Handle Order Summary command. It message query the state of the order
   *            and send Summary Order to Service API
   */
   def handleGetOrderSummary(cmd: GetOrderSummaryCommand): Unit ={
      val orderDetails = orderState.orderDetails.get
      val portfolioId = orderState.portfolioId.get
      val orderId = orderState.orderId.get
      val orderStatus = orderState.orderStatus.get
      val orderSummary = OrderSummary(orderId,portfolioId, orderDetails.symbol ,orderDetails.shares, orderStatus)
      val price = BigDecimal(0.0)
      val orderSummaryResponse = OrderSummaryResponse(orderSummary.orderId,orderSummary.portfolioId,orderSummary.symbol,orderSummary.shares, "",price)
      sender ! orderSummaryResponse
   }


  /**
   *
    * @param orderEvent
   * @return orderState
   *                Apply event to order state to reconstruct current  the state of
   *                order with events from the jornal
   */
     def applyEvent(orderEvent: OrderEvent): OrderState = orderEvent match {
       case OrderReceivedEvent(order) => {
        log.info("Applying OrderReceived Event")
         orderState  = orderState.copy(orderId = Some(order.orderId), portfolioId= Some(order.portfolioId), orderDetails = Some(order.orderDetails),
           orderStatus = Some(new Pending()))
         return orderState
       }
       case OrderFulfilledEvent(orderId, portfolioId, trade) => {
         log.info("Applying OrderFulfilledEvent")
         orderState = orderState.copy(orderId = Some(orderId))
         return orderState
       }
       case OrderFailedEvent(orderId, portfolioId) => {
         log.info("Applying OrderFailed Event")
         orderState = orderState.copy(orderId = Some(orderId))
         return orderState
       }
     }

  /**
   *
   *
   * @param any
   */
  def publishMessage(any: Any): Unit ={
         if(any.isInstanceOf[OrderFulfilledEvent]){
           val orderEvent = any.asInstanceOf[OrderFulfilledEvent]
           val payload = new Gson().toJson(orderEvent)
           val kafkaMessage = KafkaMessage(payload,ORDER_COMPLETE_EVENT_TYPE)
           messagePublisherActor ! kafkaMessage
         }
        if(any.isInstanceOf[OrderFailedEvent]){
           val orderFailedEvent = any.asInstanceOf[OrderFailedEvent]
          val payload = new Gson().toJson(orderFailedEvent)
          val kafkaMessage = KafkaMessage(payload, ORDER_FAILED_EVENT_TYPE)
          messagePublisherActor ! kafkaMessage
        }
     }

  /**
   *  We passive the OrderEntity Actor after receiving timeout
   */
  def passivate(): Unit ={
    context.parent.tell(new ShardRegion.Passivate(PoisonPill.getInstance), self)
  }


  /**
   *  We call stop method when OrderEntity is being stopped or being passive
   */
  override def postStop(): Unit ={
    var msg = ""
    if (orderState.orderId == None) {
      msg = String.format("(Order Entity %s not initialized)", self.path.name)
    } else {
      msg = "Order ID: " + orderState.orderId.get
    }
    log.info("Stop passivate {} ", msg)
  }
}
