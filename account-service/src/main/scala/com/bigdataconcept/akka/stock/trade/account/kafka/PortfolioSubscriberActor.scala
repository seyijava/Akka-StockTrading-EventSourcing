package com.bigdataconcept.akka.stock.trade.account.kafka
import akka.Done
import com.google.gson.Gson
import org.apache.kafka.clients.consumer.ConsumerRecord
import com.bigdataconcept.akka.stock.trade.account.domain.KafkaProtocol.MSGTYPE
import com.bigdataconcept.akka.stock.trade.account.domain.KafkaProtocol.WITHDRAWEVENT
import com.bigdataconcept.akka.stock.trade.account.domain.KafkaProtocol.DEPOSITEVENT

import scala.concurrent.Future
import com.bigdataconcept.akka.stock.trade.account.domain.KafkaProtocol
import com.bigdataconcept.akka.stock.trade.account.domain.KafkaProtocol.CreditAccountEvent
import com.bigdataconcept.akka.stock.trade.account.domain.KafkaProtocol.DebitAccountEvent
import akka.actor.{ActorRef, Props}
import com.bigdataconcept.akka.stock.trade.account.domain.Commands.{AddFundCommand, CreditAccountCommand, DebitAccountCommand, WithdrawalFundCommand}


object  PortfolioSubscriberActor{
     def prop(accountShardRegionProxy: ActorRef,topic:String, topicGroupId: String) : Props = Props(new PortfolioSubscriberActor(accountShardRegionProxy,topic,topicGroupId))
}

class PortfolioSubscriberActor(accountShardRegionProxy: ActorRef,topic:String, topicGroupId: String)  extends  KafkaSubscriberActor(topic, topicGroupId) {


  /**
   * Starting kafka consumer
   */
  override def preStart(): Unit = {
    log.info("Start consuming Portfolio Service  Events")
    startConsumingEvent()
  }

  override def receive: Receive = {
    case _ => "Nothing to Do"
  }


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

    if(msgType.equals(KafkaProtocol.WITHDRAWEVENT)){
      log.info("Handle  Incoming Payload Event [{}] Msg Type [{}] ", payload,msgType)
      val debitAccountEvent = new Gson().fromJson(payload,classOf[DebitAccountEvent])
      val debitAccountCmd = DebitAccountCommand(debitAccountEvent.accountId,BigDecimal.apply(debitAccountEvent.amount))
      accountShardRegionProxy forward debitAccountCmd
    }
    else if(msgType.equals(KafkaProtocol.DEPOSITEVENT)){
      log.info("Handle  Incoming Payload Event [{}]  Msg Type [{}] ", payload,msgType)
      val creditAccountEvent = new Gson().fromJson(payload,classOf[CreditAccountEvent])
      val creditAccountCmd = CreditAccountCommand(creditAccountEvent.accountId,BigDecimal.apply(creditAccountEvent.amount))
      accountShardRegionProxy forward creditAccountCmd

    }

     return Future.successful(Done)
  }


}
