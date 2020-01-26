package com.bigdataconcept.akka.stock.trade.portfolio.actor

import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.actor.{ActorLogging, ActorRef, PoisonPill, Props, ReceiveTimeout}
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Commands.{AcceptRefundCommand, AcknowledgeOrderFailureCommand, ClosePortfolioCommand, CommandFailed, CommandResponse, CompleteTradeCommand, GetPortfolioView, OpenPortfolioCommand, PlaceOrderCommand, ReceiveFundsCommand, SendFundsCommand}
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Events.{AccountCreditedEvent, AccountDebitedEvent, ClosedEvent, FundsCreditedEvent, FundsDebitedEvent, OpenEvent, OrderCompletedEvent, OrderFailedEvent, OrderFulfilledEvent, OrderPlacedEvent, PortfolioEvent, RefundAcceptedEvent, SharesCreditedEvent, SharesDebitedEvent, SharesPurchaseCreditedEvent, SharesSaleDebitedEvent}
import com.bigdataconcept.akka.stock.trade.portfolio.domain.View.PortfolioView

import scala.collection.immutable.Seq
import com.bigdataconcept.akka.stock.trade.portfolio.domain.State.PortfolioState
import com.bigdataconcept.akka.stock.trade.portfolio.domain.{KafkaProtocol, TradeType}
import com.bigdataconcept.akka.stock.trade.portfolio.domain.ApiPayload.PortfolioViewResponse
import com.bigdataconcept.akka.stock.trade.portfolio.domain.KafkaProtocol.{CreditAccountEvent, DebitAccountEvent, KafkaMessage, KafkaMessageWithHeader}
import com.google.gson.Gson
import akka.persistence.journal.Tagged
import akka.cluster.sharding.ShardRegion
import java.util.Date

import com.bigdataconcept.akka.stock.trade.portfolio.domain.Domain.SharePurchasedOrder

/**
 * @author Otun Oluwaseyi
 *
 * PortfolioEntity  Actor represents Portfolio Aggregate root.
 *
 */
object PortfolioEntityActor {

  def props(tradeOrderEventPublisherActor: ActorRef, accountEventPublisherActor: ActorRef): Props = Props(new PortfolioEntityActor(tradeOrderEventPublisherActor,accountEventPublisherActor))
}

class PortfolioEntityActor(tradeOrderEventPublisherActor: ActorRef,accountEventPublisherActor: ActorRef) extends PersistentActor with ActorLogging {

  val snapShotInterval = 2

  var portfolioState = PortfolioState()

  override def persistenceId = self.path.name //if (portfolioState.portfolioId == None) self.path.name else portfolioState.portfolioId.get

  override def receiveCommand: Receive = {

    case cmd: OpenPortfolioCommand           => openCommandHandler(cmd) //api

    case cmd: GetPortfolioView               =>  getPortfolioView(cmd)

    case cmd: PlaceOrderCommand              => placeOrderCommandHandler(cmd) //api to kafka producer

    case cmd: SendFundsCommand               => sendFundsCommandHandler(cmd)

    case cmd: ReceiveFundsCommand            => receiveFundsCommandHandler(cmd)

    case cmd: CompleteTradeCommand           => completeTradeCommandHandler(cmd) //kakfa consumer

    case cmd: AcknowledgeOrderFailureCommand => handleFailedOrder(cmd) //kakfa consumer

    case cmd: ClosePortfolioCommand          => closePortfolioCommandHandler(cmd) //api

    case cmd: CompleteTradeCommand           => completeTradeCommandHandler(cmd) //kakfa consumer

    case "snapshot" =>
      log.info("saving snapshot")
      saveSnapshot(portfolioState)

    case timeout: ReceiveTimeout => passivate()


  }



  /**
   *
   * @return
   */

  override def receiveRecover: Receive = {
    case event: OpenEvent =>
      log.info("Recovered Open Event")
     portfolioState =  applyEvent(event)

    case event: OrderPlacedEvent => applyEvent(event)

    case event: FundsCreditedEvent => applyEvent(event)

    case event: SharesSaleDebitedEvent => applyEvent(event)

    case event: FundsDebitedEvent => applyEvent(event)

    case event: SharesPurchaseCreditedEvent => applyEvent(event)

    case event: OrderCompletedEvent  => applyEvent(event)

    case event: OrderFailedEvent => applyEvent(event)

    case event: OrderFulfilledEvent => applyEvent(event)

    case SnapshotOffer(_, snapshot: PortfolioState) => portfolioState = snapshot


  }

  /**
   *
   * @param event
   * @return portfolioState
   *         Applying event to the states
   *
   */
  def applyEvent(event: PortfolioEvent): PortfolioState = event match {
    case OpenEvent(portfolioId, name,accountId,fund) => {
      log.info("Applying Open Event Name {} PortfolioId {} Fund {} " , name, portfolioId, fund)
      portfolioState = portfolioState.copy(name = Some(name), portfolioId = Some(portfolioId), accountId = Some(accountId),
      funds = Some(BigDecimal.apply(fund)))
      return portfolioState
    }
    case OrderPlacedEvent(portfolioId, orderId, orderDetails) => {
      log.info("Apply OrderPlacement Event order Id {} portfolio {}", orderId,portfolioId)
      val orderPlaceEvent = event.asInstanceOf[OrderPlacedEvent]
      portfolioState = portfolioState.copy(activeOrders = Some(portfolioState.activeOrders.get.plus(orderId, orderPlaceEvent)))
      return portfolioState
    }
    case FundsCreditedEvent(portfolioId, amount) => {
      log.info("Apply Fund Credit Event Portfolio Id {} Amount {} ", portfolioId, amount)
      portfolioState = portfolioState.copy(funds = Some(portfolioState.funds.get + amount))

      return portfolioState
    }
    case FundsDebitedEvent(portfolioId, amount) => {
      log.info("Apply Funds Debit Event {} Amount {} ", portfolioId, amount)
      portfolioState = portfolioState.copy(funds = Some(portfolioState.funds.get - amount))
      return portfolioState
    }
    case SharesCreditedEvent(portfolioId, symbol, shares) => {
      log.info("Apply Shares Credit Event Portfolio Id {}, Symbol {} No of Share {}", portfolioId, symbol,shares)
      portfolioState = portfolioState.copy(holdings = Some(portfolioState.holdings.get.add(symbol, shares)))
      return portfolioState
    }
    case SharesDebitedEvent(portfolioId, symbol, shares) => {
      log.info("Apply Share Debit Event Portfolio Id {} Symbol {} , No of Share {} " , portfolioId, symbol,shares)
      portfolioState = portfolioState.copy(holdings = Some(portfolioState.holdings.get.remove(symbol, shares)))
      return portfolioState
    }
    case OrderCompletedEvent(orderId,orderCompletedDate) => {
      log.info("Apply Order Completed  Event Order {}" , orderId)
      portfolioState = portfolioState.copy(activeOrders = Some(portfolioState.activeOrders.get.minus(orderId)))
      portfolioState = portfolioState.copy(completedOrders = Some(portfolioState.completedOrders.get.minus(orderId)))
      return portfolioState
    }
    case ClosedEvent(portfolioId, closeDate) => {
      portfolioState =  portfolioState.copy(portfolioId = Some(portfolioId))
      return portfolioState
    }
      case OrderFulfilledEvent(portfolioId, orderId, orderFulfilmentDate) =>{
        log.info("Apply Order Fulfilment  Event Portfolio Id {} Order Id {} Fulfilment Date {} " , portfolioId,orderId,new Date(orderFulfilmentDate))
        portfolioState =  portfolioState.copy(portfolioId = Some(portfolioId))
      return portfolioState
    }
    case AccountCreditedEvent(accountId, amount, transDate) =>{
      log.info("Apply Account Credit Event Account Id  {}  Amount {} " , accountId,amount)
      val creditAccountEvent = CreditAccountEvent(accountId,amount)
      val message = new Gson().toJson(creditAccountEvent)
      publishAccountEvent(message,KafkaProtocol.DEPOSITEVENT)
       return portfolioState
    }
    case AccountDebitedEvent(accountId,amount,transDate) =>{
      log.info("Apply Account Debited Event Account Id  {}  Amount {} " , accountId,amount)
      val debitAccountEvent =  DebitAccountEvent(accountId, amount)
      val message = new Gson().toJson(debitAccountEvent)
      publishAccountEvent(message,KafkaProtocol.WITHDRAWEVENT)
      return portfolioState
    }

    case SharesPurchaseCreditedEvent(portfolioId,symbol, orderId, sharePurchasedOrder) => {
      log.info("Apply Shares Credit Event Portfolio Id {}, Symbol {} No of Share {} OrderId {}", portfolioId, symbol,sharePurchasedOrder.quantity.get, orderId)
      portfolioState = portfolioState.copy(shareHoldings = Some(portfolioState.shareHoldings.get.add(orderId, sharePurchasedOrder)))
      return portfolioState
    }
    case SharesSaleDebitedEvent(portfolioId,symbol, purchaseOrderId,shares) =>{
      log.info("Apply Share Debit Event Portfolio Id {} Symbol {} , No of Share {}  OrderId {}" , portfolioId, symbol,shares, purchaseOrderId)
      portfolioState = portfolioState.copy(shareHoldings = Some(portfolioState.shareHoldings.get.remove(purchaseOrderId, shares)))
      return portfolioState
    }

  }

  /**
   *
   * @param cmd
   *            Handle order placement and raise OrderPlace Event
   *            publish orderPlace event to Kafka to TradeOrder for
   *            onward trade execution
   */
  def placeOrderCommandHandler(cmd: PlaceOrderCommand) {
    log.info("Placing order {}", cmd)
    val orderDetails = cmd.orderDetails
    orderDetails.tradeType match {
      case "SELL" => {
        val events = List(OrderPlacedEvent(cmd.portfolioId, cmd.orderId, cmd.orderDetails), SharesSaleDebitedEvent(cmd.portfolioId, orderDetails.symbol,orderDetails.purchaseOrderId.get, orderDetails.shares))
        persistAll(events) { evts =>{
          val orderPlaceEvent = OrderPlacedEvent(cmd.portfolioId, cmd.orderId, cmd.orderDetails)
         portfolioState =  applyEvent(evts)
          val payload = new Gson().toJson(orderPlaceEvent)
          publishTradeOrderEvent(payload)
          val cmdResponse = CommandResponse(cmd.portfolioId, String.format("Sell Order has been received will be processed order Id %s ",cmd.orderId))
          sender ! cmdResponse
          if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
            saveSnapshot(portfolioState)
          }
        }
      }
      case "BUY"=> {
        val orderPlaceEvent = OrderPlacedEvent(cmd.portfolioId, cmd.orderId, cmd.orderDetails)
        persist(orderPlaceEvent) { evt =>
          log.info("BUY OrderPlacedEvents")
          portfolioState = applyEvent(evt)
          val payload = new Gson().toJson(orderPlaceEvent)
          publishTradeOrderEvent(payload)
          val cmdResponse = CommandResponse(cmd.portfolioId, String.format("Buy Order has been received will be processed order Id %s ",cmd.orderId))
          sender ! cmdResponse
        }
      }

    }
  }

  /**
   *
   * @param cmd
   *            Open Portfolio This is for step on workflow
   *            The creation of portfolio start from this method.
   */
  def openCommandHandler(cmd: OpenPortfolioCommand) {
    log.info("Opening Portfolio order {}", cmd)
    persist(OpenEvent(cmd.portfolioId, cmd.name, cmd.accountId, cmd.fund)) { evt =>
      portfolioState = applyEvent(evt)
    }
  }

  /**
   *
   * @param cmd
   */
  def sendFundsCommandHandler(cmd: SendFundsCommand) {
    persist(FundsDebitedEvent(cmd.portfolioId, cmd.amount)) { evt =>
      portfolioState = applyEvent(evt)
    }
  }


  /**
   *
   * @param cmd
   */

  def receiveFundsCommandHandler(cmd: ReceiveFundsCommand) {
    persist(FundsCreditedEvent(cmd.portfolioId, cmd.amount)) { evt =>
      portfolioState = applyEvent(evt)
    }
  }

  /**
   *
   * @param cmd
   */
  def closePortfolioCommandHandler(cmd: ClosePortfolioCommand) {
    if (isEmpty()) {
      persist(ClosedEvent(cmd.portfolioId,  new Date().getTime)) { evt =>
        portfolioState = applyEvent(evt)
      }
    } else {
      self ! CommandFailed("Portfolio is not empty")
    }

  }

  /**
   *
   * @param cmd
   *            Trade Order completion . It re-calculate the portfolio
   *            once order is completed. If order type is BUY is re-calculate the
   *            shares and funds .
   */
  def completeTradeCommandHandler(cmd: CompleteTradeCommand) {

    val trade = cmd.trade
    val orderId = cmd.orderId
    val portfolioId = cmd.portfolioId
    log.info("Processing Trace Completed {}", trade)
    if (portfolioState.activeOrders.get.get(orderId) == null) {

    } else {
      log.info("Processing trading {}", trade)
      trade.tradeType match {
        case "SELL" => {
          //SharesDebitedEvent(cmd.portfolioId, trade.symbol, trade.shares),
          val accountId = portfolioState.accountId.get
          val events = List(FundsCreditedEvent(portfolioId, trade.sharePrice),  OrderFulfilledEvent(portfolioId, orderId,new Date().getTime), AccountCreditedEvent(accountId,trade.sharePrice.doubleValue(),new Date().getTime))
          persistAll(events) { evt=>
            applyEvent(evt)

          }
        }
        case "BUY" => {
          log.info("Buy Order Completed Processing Funds Debit, Order Fulfilled and Shares Credit Events")
          val accountId = portfolioState.accountId.get

          val sharePurchasedOrder = SharePurchasedOrder(Some(orderId), Some(trade.symbol),Some(trade.shares),Some(trade.sharePrice), Some(new Date().getTime()))
          val events = List(FundsDebitedEvent(portfolioId, trade.sharePrice), OrderFulfilledEvent(portfolioId, orderId,new Date().getTime), SharesPurchaseCreditedEvent(portfolioId,trade.symbol,orderId,sharePurchasedOrder),
                          AccountDebitedEvent(accountId,trade.sharePrice.doubleValue(),new Date().getTime))
          persistAll(events) { evt =>
            applyEvent(evt)


          }
        }

      }
    }
  }

  /**
   *
   * @param cmd
   *            Process failed trade order and re-calculates portfolio .
   */

  def handleFailedOrder(cmd: AcknowledgeOrderFailureCommand) {
    log.info(String.format("Order %s failed for PortfolioModel %s.", cmd.orderId, cmd.portfolioId));
    val orderPlaced = portfolioState.activeOrders.get.get(cmd.orderId)
    if (orderPlaced == null) {

    } else {
      orderPlaced.orderDetails.tradeType match {
        case "BUY" => {
          val event = OrderFailedEvent(cmd.portfolioId, cmd.orderId)
          persist(event) { evt =>
            portfolioState = applyEvent(evt)
          }
        }
        case "SELL" => {
          val events = List(SharesCreditedEvent(cmd.portfolioId, orderPlaced.orderDetails.symbol, orderPlaced.orderDetails.shares), OrderFailedEvent(cmd.portfolioId, cmd.orderId))
          persistAll(events) { evt =>
           applyEvent(evt)
          }
        }
      }
    }

  }

  /**
   *
   * @param cmd
   *            Retrieve the present state of Portfolio
   */
  def getPortfolioView(cmd: GetPortfolioView): Unit ={
    log.info("Retrive Portfolio" + portfolioState.name.get)

     var portfolioView = PortfolioView(portfolioState.portfolioId.get,portfolioState.name.get,portfolioState.funds.get.doubleValue(),
       portfolioState.shareHoldings.get.asSequence())
      //val portfolioViews = PortfolioView(portfolioState.portfolioId.get, portfolioState.name.get)
      //var holdings =  portfolioState.holdings.get
      //holdings = holdings.add("GOOGL",900)
      //portfolioView = portfolioView.copy(holding = holdings)
      val payload = new Gson().toJson(portfolioView)
      val portfolioViewResponse = PortfolioViewResponse(payload)
      sender ! portfolioViewResponse
  }


  private def isEmpty(): Boolean = {
    return (portfolioState.funds.get.compareTo(BigDecimal.apply(0)) == 0
      && portfolioState.activeOrders.get.isEmpty() && portfolioState.holdings.get.asSequence().isEmpty())

  }

  /**
   *
   * @param payload
   *                   Publish orderPlace event and push kafka for onward delivery to Kafka.
   */
  def publishTradeOrderEvent(payload: String): Unit ={
     log.info("Publishing Message To Kafka Trade Order Outbound Topic")
       val kafkaMessage = KafkaMessage(payload)
       tradeOrderEventPublisherActor ! kafkaMessage

   }


  def publishAccountEvent(payload: String,msgType: String): Unit ={
    log.info("Publishing Message To kafka Account Outbound Topic")
    val kafkaMessage = KafkaMessageWithHeader(payload,msgType)
     accountEventPublisherActor ! kafkaMessage

  }

  def passivate(): Unit ={
    context.parent.tell(new ShardRegion.Passivate(PoisonPill.getInstance), self)
  }

  override def postStop(): Unit ={
    var msg = ""
    if (portfolioState.portfolioId == None) {
      msg = String.format("(Portfolio Entity %s not initialized)", self.path.name)
    } else {
      msg = "Portfolio ID: " + portfolioState.portfolioId.get
    }
    log.info("Stop passivate {} {} ", msg, portfolioState.shareHoldings.get)
  }
}
