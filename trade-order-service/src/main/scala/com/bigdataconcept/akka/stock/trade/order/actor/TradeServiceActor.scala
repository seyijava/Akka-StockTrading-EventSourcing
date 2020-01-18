package com.bigdataconcept.akka.stock.trade.order.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.bigdataconcept.akka.stock.trade.order.domain.Domains.{Order, OrderFailed, OrderFulfilled, OrderResult, Trade}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import akka.pattern.pipe
import akka.pattern.ask
import com.bigdataconcept.akka.stock.trade.order.domain.Commands.CompleteOrderCommand


/**
 * @author Oluwaseyi Otun
 *         TradeService Actor model trade execution service.
 *         It get latest stock quote from the market and send to broker
 *         It check the tradeType if is Limit or Market Price
 *         if stock price is getter than limit orderfailed
 *         else OrderFulfilled but if Trade type is market price order
 *         is fulfilled.
 *
 */
object TradeServiceActor{
   def props(tradeOrderShardRegion: ActorRef) :Props = Props(new TradeServiceActor(tradeOrderShardRegion))
}
class TradeServiceActor(tradeOrderShardRegion: ActorRef) extends Actor with ActorLogging {

   context.system.eventStream.subscribe(self, classOf[Order])

  implicit val ec: ExecutionContext = context.dispatcher

  /**
   *
   * @return Recieve
   *         This is where we processing incoming messages from this actor Inbox
   *         This process the following 1. Order
   *                                    2. OrderResult
   *
   */
  override def receive: Receive = {

     case order: Order =>
      log.info("Processing Trade Order {} {}", order.orderId, order)
       val orderResponse = markMarketOrderCompleted(order)
       self ! orderResponse
     case orderResponse: OrderResult =>
       if(orderResponse.isInstanceOf[OrderFulfilled]){
         val orderFulfilled = orderResponse.asInstanceOf[OrderFulfilled]
         val completeOrderCommand = new CompleteOrderCommand(orderFulfilled.orderId, orderResponse)
         log.info("Sending Order complete to TraderOrderEntity  order Id {}  {}", completeOrderCommand.orderId,completeOrderCommand)
         tradeOrderShardRegion.forward(completeOrderCommand)
       }


  }


  /**
   *
   * @param order
   * @return Future[OrderResult]
   *         This is a blocking call so we span another thread
   *         to make the call to certify one of the principle of Actor Model
   */
     def completeMarketOrder(order: Order): Future[OrderResult]={
       log.info("Sending  Trade Order {} {}", order.orderId, order)
       Thread.sleep(10000)
       val orderDetails = order.orderDetails
       val trade = Trade(orderDetails.symbol,orderDetails.tradeType,BigDecimal(105.65).doubleValue(),orderDetails.shares)
       val orderFulfilled = OrderFulfilled(order.orderId,order.portfolioId,trade)
       return Future.successful(orderFulfilled)
     }


  def markMarketOrderCompleted(order: Order): OrderResult={
    log.info("Sending  Trade Order {} {}", order.orderId, order)
    //Thread.sleep(10000)
    val orderDetails = order.orderDetails
    val trade = Trade(orderDetails.symbol,orderDetails.tradeType,BigDecimal(105.65).doubleValue(),orderDetails.shares)
    val orderFulfilled = OrderFulfilled(order.orderId,order.portfolioId,trade)
     return orderFulfilled
  }
}
