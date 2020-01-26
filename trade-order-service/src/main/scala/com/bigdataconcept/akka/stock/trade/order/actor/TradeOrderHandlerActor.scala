package com.bigdataconcept.akka.stock.trade.order.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.bigdataconcept.akka.stock.trade.order.domain.Commands.CompleteOrderCommand
import com.bigdataconcept.akka.stock.trade.order.domain.Domains.{Order, OrderFulfilled, OrderResult, Trade}

import scala.concurrent.{ExecutionContext, Future}


object TradeOrderHandlerActor{

   def props(orderShardingRegion: ActorRef) : Props = Props(new TradeOrderHandlerActor(orderShardingRegion))
}


/**
 * @author Oluwaseyi Otun
 *
 * @param orderShardingRegion
 */
class TradeOrderHandlerActor(orderShardingRegion: ActorRef) extends Actor with ActorLogging{

  context.system.eventStream.subscribe(self, classOf[Order])

  /**
   *
   * @return Recieve
   *         This is where we processing incoming messages from this actor Inbox
   *         This process the following 1. Order
   *                                    2. OrderResult
   *
   */
  override def receive: Receive = {

    case order: Order => {
       log.info("Incoming Order Request {}", order)
      val tradeOrderActor = context.actorOf(TradeServiceActor.props(orderShardingRegion))
       tradeOrderActor ! order
       log.info("Order forwarded to {}", tradeOrderActor.path)

    }

  }



}
