package com.bigdataconcept.akka.stock.trade.portfolio.kafka

import akka.Done
import akka.actor.{ActorLogging, ActorRef, Props}
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Commands.{OpenPortfolioCommand, ReceiveFundsCommand, SendFundsCommand}
import com.bigdataconcept.akka.stock.trade.portfolio.domain.KafkaProtocol
import com.bigdataconcept.akka.stock.trade.portfolio.domain.kafkaEvent.{CreatePortfolioEvent, DepositFundEvent,  WithDrawFundEvent}
import com.google.gson.Gson
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.Future

object AccountEventSubscriberActor{
     def props(topic: String, groupId: String, portfolioShardRegion: ActorRef) : Props = Props(new AccountEventSubscriberActor(topic, groupId,portfolioShardRegion))
}

class AccountEventSubscriberActor(topic: String, groupId: String, portfolioShardRegion: ActorRef) extends KafkaSubcriberActor(topic,groupId) with ActorLogging{

  override def receive: Receive = {
    case _ => "Nothing to Do"
  }


  /**
   * Starting kafka consumer
   */
  override def preStart(): Unit = {
    log.info("Start consuming Trade Order Events")
    startConsumingEvent()
  }

  override def handleEvent(event: ConsumerRecord[String, String]): Future[Done] = {

    val payload = event.value()
    val messageHeaders = event.headers()
    var msgType = ""
    messageHeaders.forEach(header => {
      if(header.key().equals("EVENT_TYPE")){
        msgType = new String(header.value())
      }
    })

    if(msgType.equals(KafkaProtocol.OPENPORTFOLIOEVENT)) {
      log.info("Handle  Incoming Payload Event [{}] Msg Type [{}] ", payload,msgType)
      val createPortfolioEvent = new Gson().fromJson(payload, classOf[CreatePortfolioEvent])
      val openPortfolioCommand = OpenPortfolioCommand(createPortfolioEvent.portfolio, createPortfolioEvent.name,
        createPortfolioEvent.accountId, createPortfolioEvent.fund)
        portfolioShardRegion.forward(openPortfolioCommand)
    }
    else if(msgType.equals(KafkaProtocol.DEPOSITEVENT)){
      log.info("Handle  Incoming Payload Event [{}] Msg Type [{}] ", payload,msgType)
      val depositFundEvent = new Gson().fromJson(payload,classOf[DepositFundEvent])
      val  receiveFundsCommand = ReceiveFundsCommand(depositFundEvent.portfolioId,BigDecimal.valueOf(depositFundEvent.amount))
      portfolioShardRegion.forward(receiveFundsCommand)
    }
    else if(msgType.equals(KafkaProtocol.WITHDRAWEVENT)){
      log.info("Handle  Incoming Payload Event [{}]  Msg Type [{}] ", payload,msgType)
      val withDrawFundEvent = new Gson().fromJson(payload, classOf[WithDrawFundEvent])
      val sendFundsCommand = SendFundsCommand(withDrawFundEvent.portfolioId,BigDecimal.valueOf(withDrawFundEvent.amount))
      portfolioShardRegion.forward(sendFundsCommand)

    }
    return Future.successful(Done)
  }


}
