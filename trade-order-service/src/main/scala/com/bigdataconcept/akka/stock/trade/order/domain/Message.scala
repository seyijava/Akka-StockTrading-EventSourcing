package com.bigdataconcept.akka.stock.trade.order.domain

import com.bigdataconcept.akka.stock.trade.order.domain.Domains.{Order, OrderDetails, OrderResult, OrderStatus, Trade}

/**
 * @author Oluwaseyi Otun
 */

object TradeType extends Enumeration  {
  type TradeType = Value
  val BUY,SELL = Value
}


/**
 * Kafka message protocol
 */
object KafkaProtocol{
  case class KafkaMessage(payload: String, msgType: String) extends Serializable

}


/**
 *   Order Entity Commands definations
 */
object Commands {

   trait OrderCommand extends  Serializable

   case class PlaceOrderCommand(orderId: String, portfolioId: String, order: Order) extends OrderCommand

   case class CompleteOrderCommand(orderId: String, orderResult: OrderResult) extends OrderCommand

   case class GetOrderStatusCommand(orderId: String) extends OrderCommand

   case class GetOrderSummaryCommand(orderId: String) extends  OrderCommand


}


/**
 *  RestAPI Value objects definations
 */

object ApiPayload{

  case class OrderSummaryResponse(orderId: String, portfolioId: String,symbol: String, shares: Int,orderStatus: String, price: BigDecimal) extends Serializable

  case class OpenPortfolioResponse(msg: String) extends  Serializable
  case class DepositRequest(portfolioId: String, funds: BigDecimal) extends  Serializable
  case class WithdrawalRequest(portfolioId: String,  funds: BigDecimal) extends Serializable
  case class RefundRequest(portfolioId: String,funds: BigDecimal) extends Serializable
  case class PlaceOrderRequest(portfolioId: String, symbol: String, numberShare: Int, tradeType: Int) extends Serializable
  case class PortfolioViewResponse(payload: String) extends Serializable
}

/**
 *  Domain models
 */
object Domains
{
      import TradeType.TradeType

      trait  OrderType extends Serializable

      case class Limit(limitPrice: BigDecimal) extends OrderType

      case class Market() extends OrderType

      case class OrderDetails(symbol:String,shares: Int, tradeType: String) extends  Serializable
      case class OrderSummary(orderId: String, portfolioId: String,symbol: String, shares: Int,orderStatus: OrderStatus) extends Serializable
      case class Order(orderId: String, portfolioId: String, orderDetails: OrderDetails) extends Serializable


       abstract class OrderResult(orderId: String, portfolioId: String) extends  Serializable
       case class OrderFulfilled(orderId: String, portfolioId: String, trade: Trade) extends OrderResult(orderId, portfolioId =portfolioId)
       case class OrderFailed(orderId:String,portfolioId: String) extends OrderResult(orderId, portfolioId = portfolioId)

       case class Trade(symbol:String,tradeType: String, sharePrice: Double,shares: Int) extends Serializable

       abstract class OrderStatus() extends Serializable
       case class Pending() extends OrderStatus()
       case class Fulfilled(price: BigDecimal) extends OrderStatus()
       case class Failed() extends  OrderStatus()

}

/**
 * Order State defininations
 */
object State{

     case class OrderState(orderId: Option[String] = None, portfolioId: Option[String] = None, orderDetails: Option[OrderDetails] = None, orderStatus: Option[OrderStatus] = None)
}


/**
 *  Events defininations
 */

object Events{
    trait  OrderEvent extends Serializable
    case class OrderReceivedEvent(order: Order) extends  OrderEvent
    case class OrderFulfilledEvent(orderId: String, portfolioId: String, trade: Trade) extends  OrderEvent
    case class OrderFailedEvent(orderId: String, portfolioId: String) extends  OrderEvent

}

object Kafka{
  case class OrderPlacedEvent(portfolioId: String, orderId: String, orderDetails: OrderDetails) extends Serializable

  case class TradeCompletedEvent() extends  Serializable
}
