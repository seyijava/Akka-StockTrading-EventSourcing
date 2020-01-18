package com.bigdataconcept.akka.stock.trade.portfolio.actor

import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.actor.{ActorLogging, ActorRef, PoisonPill, Props, ReceiveTimeout}
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Commands.{AcceptRefundCommand, AcknowledgeOrderFailureCommand, ClosePortfolioCommand, CommandFailed, CommandResponse, CompleteTradeCommand, GetPortfolioView, OpenPortfolioCommand, PlaceOrderCommand, PortfolioView, ReceiveFundsCommand, SendFundsCommand}
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Events.{ClosedEvent, FundsCreditedEvent, FundsDebitedEvent, OpenEvent, OrderCompletedEvent, OrderFailedEvent, OrderFulfilledEvent, OrderPlacedEvent, PortfolioEvent, RefundAcceptedEvent, SharesCreditedEvent, SharesDebitedEvent}
import scala.collection.immutable.Seq
import com.bigdataconcept.akka.stock.trade.portfolio.domain.State.PortfolioState
import com.bigdataconcept.akka.stock.trade.portfolio.domain.TradeType
import com.bigdataconcept.akka.stock.trade.portfolio.domain.ApiPayload.PortfolioViewResponse
import com.bigdataconcept.akka.stock.trade.portfolio.domain.KafkaProtocol.KafkaMessage
import com.google.gson.Gson
import akka.persistence.journal.Tagged
import akka.cluster.sharding.ShardRegion

/**
 * @author Otun Oluwaseyi
 *
 * PortfolioEntity  Actor represents Portfolio Aggregate root.
 *
 */
object PortfolioEntityActor {

  def props(messagePublisherActor: ActorRef): Props = Props(new PortfolioEntityActor(messagePublisherActor))
}

class PortfolioEntityActor(messagePublisherActor: ActorRef) extends PersistentActor with ActorLogging {



  var portfolioState = PortfolioState()

  override def persistenceId = self.path.name //if (portfolioState.portfolioId == None) self.path.name else portfolioState.portfolioId.get

  override def receiveCommand: Receive = {

    case cmd: OpenPortfolioCommand         => openCommandHandler(cmd) //api

    case cmd: GetPortfolioView                =>        getPortfolioView(cmd)

    case cmd: PlaceOrderCommand              => placeOrderCommandHandler(cmd) //api to kafka producer

    case cmd: SendFundsCommand               => sendFundsCommandHandler(cmd)

    case cmd: AcceptRefundCommand            => acceptRefundCommandHandler(cmd)

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


  def startTradingReceive: Receive = {
    case cmd: PlaceOrderCommand              => placeOrderCommandHandler(cmd) //api to kafka producer

    case cmd: SendFundsCommand               => sendFundsCommandHandler(cmd)

    case cmd: AcceptRefundCommand            => acceptRefundCommandHandler(cmd)

    case cmd: ReceiveFundsCommand            => receiveFundsCommandHandler(cmd)

    case cmd: CompleteTradeCommand           => completeTradeCommandHandler(cmd) //kakfa consumer

    case cmd: AcknowledgeOrderFailureCommand => handleFailedOrder(cmd) //kakfa consumer

    case cmd: ClosePortfolioCommand          => closePortfolioCommandHandler(cmd) //api



    //case cmd: GetPortfolioView             =>  getPortfolioView(cmd)  //api
  }
  /**
   *
   * @return
   */

  override def receiveRecover: Receive = {
    case event: OpenEvent =>
      log.info("Recovered Open Event")
      applyEvent(event)

    case event: OrderPlacedEvent => applyEvent(event)

    case event: FundsCreditedEvent => applyEvent(event)

    case event: SharesDebitedEvent => applyEvent(event)

    case event: FundsDebitedEvent => applyEvent(event)

    case event: SharesCreditedEvent => applyEvent(event)

    case event: OrderCompletedEvent  => applyEvent(event)

    case event: OrderFailedEvent => applyEvent(event)

    case event: OrderFulfilledEvent => applyEvent(event)

    case offer@SnapshotOffer(_, snapshot: PortfolioState) =>
      log.info("recovering with snapshot: {}", offer)
      portfolioState = snapshot
  }

  /**
   *
   * @param event
   * @return portfolioState
   *         Applying event to the states
   *
   */
  def applyEvent(event: PortfolioEvent): PortfolioState = event match {
    case OpenEvent(portfolioId, name) => {
      log.info("Applying Open Event" + name)
      portfolioState = portfolioState.copy(name = Some(name), portfolioId = Some(portfolioId))
      return portfolioState
    }
    case OrderPlacedEvent(portfolioId, orderId, orderDetails) => {
      log.info("Apply OrderPlacement Event")
      val orderPlaceEvent = event.asInstanceOf[OrderPlacedEvent]
      portfolioState = portfolioState.copy(activeOrders = Some(portfolioState.activeOrders.get.plus(orderId, orderPlaceEvent)))
      return portfolioState
    }
    case FundsCreditedEvent(portfolioId, amount) => {
      log.info("Apply Fund Credit Event")
      portfolioState = portfolioState.copy(funds = Some(portfolioState.funds.get + amount))
      return portfolioState
    }
    case FundsDebitedEvent(porfolioId, amount) => {
      log.info("Apply Funds Debit Event")
      portfolioState = portfolioState.copy(funds = Some(portfolioState.funds.get - amount))
      return portfolioState
    }
    case SharesCreditedEvent(portfolioId, symbol, shares) => {
      log.info("Apply Shares Credit Event")
      portfolioState = portfolioState.copy(holdings = Some(portfolioState.holdings.get.add(symbol, shares)))
      return portfolioState
    }
    case SharesDebitedEvent(portfolioId, symbol, shares) => {
      log.info("Apply Share Debit Event" + symbol)
      portfolioState = portfolioState.copy(holdings = Some(portfolioState.holdings.get.remove(symbol, shares)))
      return portfolioState
    }
    case OrderCompletedEvent(orderId) => {
      portfolioState = portfolioState.copy(activeOrders = Some(portfolioState.activeOrders.get.minus(orderId)))
      portfolioState = portfolioState.copy(completedOrders = Some(portfolioState.completedOrders.get.minus(orderId)))
      return portfolioState
    }
    case ClosedEvent(portfolioId) => {
      return portfolioState
    }
      case OrderFulfilledEvent(portfolioId, orderId) =>{
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
        val events = Seq(OrderPlacedEvent(cmd.portfolioId, cmd.orderId, cmd.orderDetails), SharesDebitedEvent(cmd.portfolioId, orderDetails.symbol, orderDetails.shares))
        persistAll(events) { evts =>{
          System.out.println(events.length)
          events.foreach(event => {
            if (event.isInstanceOf[OrderPlacedEvent]) {
              log.info("placessslslslsllssssssssssssssssssssssssssssssssssssssssssss")
              val orderPlaceEvent = event.asInstanceOf[OrderPlacedEvent]
              portfolioState = applyEvent(orderPlaceEvent)
              publishOrderPlaceEvent(orderPlaceEvent)
            }
            if (event.isInstanceOf[SharesDebitedEvent]) {
              log.info("deibiiitiitisisisisisi")
              val sharesDebitedEvent = event.asInstanceOf[SharesDebitedEvent]
              log.info("deibiiitiitisisisisisi" + sharesDebitedEvent.symbol)
              portfolioState = applyEvent(sharesDebitedEvent)

            }
            val cmdResponse = CommandResponse(cmd.portfolioId, String.format("Sell Order has been received will be processed order Id %s ",cmd.orderId))
            sender ! cmdResponse
          })}
        }
      }
      case "BUY"=> {
        val orderPlaceEvent = OrderPlacedEvent(cmd.portfolioId, cmd.orderId, cmd.orderDetails)
        persist(orderPlaceEvent) { evt =>
          portfolioState = applyEvent(orderPlaceEvent)
          publishOrderPlaceEvent(orderPlaceEvent)
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
    persist(OpenEvent(cmd.portfolioId, cmd.name)) { evt =>
      portfolioState = applyEvent(evt)
    }
    sender ! CommandResponse(cmd.portfolioId, cmd.name + " portfolio Successfully opened")

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
  def acceptRefundCommandHandler(cmd: AcceptRefundCommand) {
    persist(RefundAcceptedEvent(cmd.portfolioId, cmd.transferId, cmd.amount)) { evt =>
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
      persist(ClosedEvent(cmd.portfolioId)) { evt =>
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
          val events = Seq(FundsDebitedEvent(portfolioId, trade.sharePrice), SharesDebitedEvent(cmd.portfolioId, trade.symbol, trade.shares), OrderFulfilledEvent(portfolioId, orderId))
          persistAll(events) { evt =>
          }
        }
        case "BUY" => {
          val events = Seq(FundsDebitedEvent(portfolioId, trade.sharePrice), OrderFulfilledEvent(portfolioId, orderId), SharesCreditedEvent(portfolioId,trade.symbol,trade.shares))
          persistAll(events) { evts =>
            events.foreach(event => {
              if (event.isInstanceOf[FundsDebitedEvent]) {
                val fundsDebitedEvent = event.asInstanceOf[FundsDebitedEvent]
                portfolioState = applyEvent(fundsDebitedEvent)
              } else if (event.isInstanceOf[OrderFulfilledEvent]) {
                val orderFulfilledEvent = event.asInstanceOf[OrderFulfilledEvent]
                portfolioState = applyEvent(orderFulfilledEvent)
              }
             else if (event.isInstanceOf[SharesCreditedEvent]) {
                val shareCreditEvent = event.asInstanceOf[SharesCreditedEvent]
              portfolioState = applyEvent(shareCreditEvent)
            }
            })

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
          val events = Seq(SharesCreditedEvent(cmd.portfolioId, orderPlaced.orderDetails.symbol, orderPlaced.orderDetails.shares), OrderFailedEvent(cmd.portfolioId, cmd.orderId))
          persistAll(events) { evts =>
            evts.productIterator.foreach(event => {
              if (event.isInstanceOf[SharesCreditedEvent]) {
                val shareCreditEvent = event.asInstanceOf[SharesCreditedEvent]
                portfolioState = applyEvent(shareCreditEvent)
              }
              if (event.isInstanceOf[OrderFailedEvent]) {
                val orderFailedEvent = event.asInstanceOf[OrderFailedEvent]
                portfolioState = applyEvent(orderFailedEvent)
              }
            })

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
      val portfolioViews = PortfolioView(portfolioState.portfolioId.get, portfolioState.name.get)
      //val portfolioView = new Gson().toJson(portfolioViews)
      sender ! portfolioViews
  }


  private def isEmpty(): Boolean = {
    return (portfolioState.funds.get.compareTo(BigDecimal.apply(0)) == 0
      && portfolioState.activeOrders.get.isEmpty() && portfolioState.holdings.get.asSequence().isEmpty())

  }

  /**
   *
   * @param orderPlace
   *                   Publish orderPlace event and push kafka for onward delivery to Kafka.
   */
  def publishOrderPlaceEvent(orderPlace: OrderPlacedEvent): Unit ={
     log.info("Publishing Place Order")
      val payload = new Gson().toJson(orderPlace)
       val kafkaMessage = KafkaMessage(payload)
       messagePublisherActor ! kafkaMessage
        //context.system.eventStream.publish(kafkaMessage)
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
    log.info("Stop passivate {} ", msg)
  }
}
