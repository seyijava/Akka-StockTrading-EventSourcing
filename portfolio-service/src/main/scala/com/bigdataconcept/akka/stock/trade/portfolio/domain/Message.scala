package com.bigdataconcept.akka.stock.trade.portfolio.domain

import com.bigdataconcept.akka.stock.trade.portfolio.domain.Domain.{Holding, SharePurchasedOrder, Trade}
import org.pcollections.HashTreePMap
import org.pcollections.PMap
import org.pcollections.PSet
import org.pcollections.HashTreePSet
import org.pcollections.PSequence
import org.pcollections.ConsPStack
import org.pcollections.HashTreePMap
import org.pcollections.PMap
import org.pcollections.PSequence
import java.util.Date

import scala.collection.JavaConverters._
import java.util.stream._
import java.util.ArrayList
import java.util.List

import com.bigdataconcept.akka.stock.trade.portfolio.domain.State.{Holdings, ShareHoldings}




object kafkaEvent{
  case class TradeCompletedEvent(portfolioId: String,orderId: String,trade: Trade)
  case class TradeOrdeFailedEvent(portfolioId: String,orderId: String)
  case class CreatePortfolioEvent(accountId: String, portfolio: String, name: String, fund: Double) extends  Serializable
  case class DepositFundEvent(accountId: String ,portfolioId: String,  amount: Double) extends Serializable
  case class WithDrawFundEvent(accountId: String ,portfolioId: String,  amount: Double) extends  Serializable

}

object View{
   case class PortfolioView(portfolioId: String, name: String, funds: Double, holdings: PSequence[Holding])
}
object TradeType extends Enumeration  {
  type TradeType = Value
  val BUY,SELL = Value
}


object LoyaltyLevel extends Enumeration {
  type LoyaltyLevel = Value
  val BRONZE, SILVER, GOLD = Value
}

object KafkaProtocol{
  case class KafkaMessage(payload: String) extends Serializable

  case class KafkaMessageWithHeader(payload: String, msgType: String) extends Serializable


  val DEPOSITEVENT = "DEPOSIT_FUNDS_EVENT"
  val WITHDRAWEVENT= "WITHDRAWAL_EVENT"
  val OPENPORTFOLIOEVENT = "OPENPORTFOLIO_EVENT"
  val MSGTYPE = "EVENT_TYPE"

  case class CreditAccountEvent(accountId: String, amount: Double) extends  Serializable
  case class DebitAccountEvent(accountId: String, amount: Double) extends  Serializable

}

object ApiPayload{
     case class OpenPortfolioRequest(name: String) extends Serializable
     case class OpenPortfolioResponse(msg: String) extends  Serializable
     case class DepositRequest(portfolioId: String, funds: BigDecimal) extends  Serializable
     case class WithdrawalRequest(portfolioId: String,  funds: BigDecimal) extends Serializable
     case class RefundRequest(portfolioId: String,funds: BigDecimal) extends Serializable
     case class PlaceBuyOrderRequest(portfolioId: String, symbol: String, numberShare: Int) extends Serializable
     case class PlaceSellOrderRequest(portfolioId: String, symbol: String, numberShare: Int, purchaseOrderId: String) extends Serializable
     case class PortfolioViewResponse(payload: String) extends Serializable

}
object Commands {

  trait PortfolioCommand extends Serializable

  import Domain.{OrderDetails,Trade}
  import TradeType.TradeType



  case class OpenPortfolioCommand(portfolioId: String,name: String, accountId: String, fund: Double) extends PortfolioCommand

  case class PlaceOrderCommand(portfolioId: String,orderId: String, orderDetails: OrderDetails) extends PortfolioCommand

  case class CompleteTradeCommand(portfolioId: String,orderId: String,trade: Trade) extends PortfolioCommand

  case class ReceiveFundsCommand(portfolioId: String, amount: BigDecimal) extends PortfolioCommand

  case class SendFundsCommand(portfolioId: String, amount: BigDecimal) extends PortfolioCommand

  case class AcceptRefundCommand(portfolioId: String,transferId: String, amount: BigDecimal) extends PortfolioCommand

  case class AcknowledgeOrderFailureCommand(portfolioId: String, orderId: String) extends PortfolioCommand

  case class ClosePortfolioCommand(portfolioId: String) extends PortfolioCommand

  case class CommandFailed(message: String)

  case class GetPortfolioView(portfolioId: String) extends PortfolioCommand

  case class CommandResponse(portfolioId: String, responseMessage: String) extends  Serializable

  case class PortfolioView(name: String, portfolioId:String) extends  Serializable



}

object Domain {

  import TradeType.TradeType

  trait  OrderType extends Serializable

  case class Limit(limitPrice: BigDecimal) extends OrderType

  case class Market() extends OrderType

  case class OrderDetails(symbol:String,shares: Int, orderType: OrderType, tradeType: String, purchaseOrderId: Option[String] = Some("")) extends Serializable

  case class Holding(symbol:String, purchasedOrderId: String,  shareCount: Int, purchasePrice: Double, purchaseDate: Long) extends Serializable

  case class Trade(symbol:String,tradeType: String, sharePrice: Double,shares: Int) extends Serializable

  case class SharePurchasedOrder(orderId: Option[String] = None,symbol: Option[String] = None, quantity: Option[Int] = Some(0), price: Option[Double] = Some(0.0d), purchaseDate: Option[Long] = None )  extends Serializable
}

object Events {

  import Domain.OrderDetails

  trait PortfolioEvent

  case class OpenEvent(portfolioId: String, name: String, accountId: String,fund: Double) extends PortfolioEvent

  case class LiquidationStartedEvent(portfolioId: String) extends PortfolioEvent

  case class SharesCreditedEvent(portfolioId: String, symbol: String, shares: Int) extends PortfolioEvent

  case class SharesDebitedEvent(portfolioId: String, symbol: String,shares: Int) extends PortfolioEvent

  case class FundsDebitedEvent(portfolioId: String, sharePrice: BigDecimal) extends PortfolioEvent

  case class FundsCreditedEvent(portfolioId: String, sharePrice: BigDecimal) extends PortfolioEvent

  case class RefundAcceptedEvent(portfolioId: String, transferId: String, amount: BigDecimal) extends PortfolioEvent

  case class OrderPlacedEvent(portfolioId: String, orderId: String, orderDetails: OrderDetails) extends PortfolioEvent

  case class OrderFulfilledEvent(portfolioId: String, orderId: String, fulfillmentDate: Long) extends PortfolioEvent

  case class OrderFailedEvent(portfolioId: String, orderId: String) extends PortfolioEvent

  case class ClosedEvent(portfolioId: String, closeDate: Long) extends PortfolioEvent

  case class OrderCompletedEvent(orderId: String, orderCompletedDate: Long) extends PortfolioEvent

  case class AccountCreditedEvent(accountId: String, amount: Double, transDate: Long) extends  PortfolioEvent

  case class AccountDebitedEvent(accountId: String, amount: Double,  transDate: Long) extends  PortfolioEvent

  case class SharesPurchaseCreditedEvent(portfolioId: String, symbol: String ,orderId: String, sharePurchasedOrder: SharePurchasedOrder) extends PortfolioEvent

  case class SharesSaleDebitedEvent(portfolioId: String, symbol: String, purchaseOrderId: String,shares: Int) extends PortfolioEvent


}


object State{

  import Events.{OrderPlacedEvent,FundsCreditedEvent,FundsDebitedEvent, SharesCreditedEvent, SharesDebitedEvent}
  import Domain.Holding

  case class PortfolioState(accountId: Option[String] = None, portfolioId: Option[String]=None, name: Option[String]=None, loyaltyLevel: Option[LoyaltyLevel.LoyaltyLevel] = Some(LoyaltyLevel.BRONZE), holdings: Option[Holdings] = Some(new Holdings()),
                            funds: Option[BigDecimal] = Some(BigDecimal.valueOf(0)),
                            activeOrders: Option[PMap[String, OrderPlacedEvent]] = Some(HashTreePMap.empty()),
                            completedOrders: Option[PSet[String]] = Some(HashTreePSet.empty()),
                            shareHoldings:  Option[ShareHoldings] = Some(new ShareHoldings())) extends Serializable {

    def update(evt: FundsCreditedEvent) : PortfolioState={
      this.copy(funds= Some(this.funds.get + evt.sharePrice))
    }

    def update(evt: FundsDebitedEvent): PortfolioState={
      this.copy(funds = Some(this.funds.get - evt.sharePrice))
    }

    def update(evt: OrderPlacedEvent): PortfolioState={
      this.copy(activeOrders = Some(activeOrders.get.plus(evt.orderId, evt)))
    }

    def orderCompleted(orderId: String): PortfolioState={
      this.copy(completedOrders = Some(this.completedOrders.get.plus(orderId)), activeOrders=Some(this.activeOrders.get.minus(orderId)))
    }

    def update(evt: SharesCreditedEvent): PortfolioState= {
      this.copy(holdings = Some(this.holdings.get.add(evt.symbol, evt.shares)))
    }

    def update(evt: SharesDebitedEvent): PortfolioState= {
      this.copy(holdings = Some(this.holdings.get.remove(evt.symbol, evt.shares)))
    }

    def initialState(name: String,portfolioId: String): PortfolioState={
      this.copy(portfolioId=Some(portfolioId), name = Some(name))
    }
  }

  case class Holdings(holdings: PMap[String, Int] = HashTreePMap.empty()) {

    def add(symbol: String, newShares: Int): Holdings = {
      var currentShares = 0
      if (holdings.containsKey(symbol)) {
        currentShares = holdings.get(symbol);
      }
      return new Holdings(holdings.plus(symbol, currentShares + newShares))
    }

    def remove(symbol: String, sharesToRemove: Int): Holdings = {
      if (sharesToRemove <= 0) {
        throw new IllegalArgumentException("Number of shares to remove from Holdings must be positive.");
      }
      if (holdings.containsKey(symbol)) {
        var currentShares = holdings.get(symbol)
        var remainingShares = currentShares - sharesToRemove
        if (remainingShares > 0) {
          return new Holdings(holdings.plus(symbol, remainingShares))
        } else if (remainingShares == 0) {
          return new Holdings(holdings.minus(symbol))
        } else {
          throw new IllegalStateException("Attempt to remove more shares from Holdings than are currently available.")
        }
      }
      else {
        throw new IllegalStateException(
          String.format("Attempt to remove shares for symbol %s not contained in Holdings.", symbol))
      }
    }

    def getShareCount(symbol: String): Int= {
      return holdings.getOrDefault(symbol, 0)
    }



    def asSequence(): PSequence[Holding] ={
      var holdingSeq: java.util.List[Holding] = new java.util.ArrayList[Holding]()
      val symbolsSet = holdings.keySet()
      symbolsSet.forEach(symbol => {
     //   holdingSeq.add(new Holding(symbol, holdings.get(symbol)))
      })
      return  ConsPStack.from(holdingSeq)

    }

  }





  case class ShareHoldings(holdings: PMap[String, SharePurchasedOrder] = HashTreePMap.empty()) {

    def add(orderId: String, newShares: SharePurchasedOrder): ShareHoldings = {
      var sharePurchasedOrder = newShares
      if (holdings.containsKey(orderId)) {
        var currentShares = holdings.get(orderId).quantity.get;
        val newQuantity = sharePurchasedOrder.quantity.get + currentShares
        sharePurchasedOrder = sharePurchasedOrder.copy(quantity = Some(newQuantity))
      }
      return new ShareHoldings(holdings.plus(orderId, sharePurchasedOrder))
    }

    def remove(orderId: String, sharesToRemove: Int): ShareHoldings = {
      if (sharesToRemove <= 0) {
        throw new IllegalArgumentException("Number of shares to remove from Holdings must be positive.");
      }
      if (holdings.containsKey(orderId)) {
        var sharePurchasedOrder = holdings.get(orderId)
        var currentShares = sharePurchasedOrder.quantity.get
        var remainingShares = currentShares - sharesToRemove
        if (remainingShares > 0) {
          sharePurchasedOrder = sharePurchasedOrder.copy(quantity = Some(remainingShares))
          return new ShareHoldings(holdings.plus(orderId, sharePurchasedOrder))
        } else if (remainingShares == 0) {
          return new ShareHoldings(holdings.minus(orderId))
        } else {
          throw new IllegalStateException("Attempt to remove more shares from Holdings than are currently available.")
        }
      }
      else {
        throw new IllegalStateException(
          String.format("Attempt to remove shares for symbol %s not contained in Holdings.", orderId))
      }
    }

    def getShareCount(symbol: String): Int= {
      return 0 //holdings.getOrDefault(symbol, 0).quantity.get
    }



    def asSequence(): PSequence[Holding] ={
      var holdingSeq: java.util.List[Holding] = new java.util.ArrayList[Holding]()
      val symbolsSet = holdings.keySet()
      symbolsSet.forEach(orderId => {
        var sharePurchasedOrder = holdings.get(orderId)
        var symbol = sharePurchasedOrder.symbol.get
        var quantity = sharePurchasedOrder.quantity.get
        var price = sharePurchasedOrder.price.get
        var purchaseDate = sharePurchasedOrder.purchaseDate.get
        holdingSeq.add(new Holding(symbol,orderId, quantity, price, purchaseDate))
      })
      return  ConsPStack.from(holdingSeq)

    }

  }




}




