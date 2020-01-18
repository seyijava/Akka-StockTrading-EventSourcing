package com.bigdataconcept.akka.stock.trade.account.actor

import akka.actor.{ActorLogging, ActorRef, PoisonPill, Props, ReceiveTimeout}
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.bigdataconcept.akka.stock.trade.account.domain.Commands.{AddFundCommand, OpenAccountCommand, UpdateAddressCommand, UpdateContactInfoCommand, WithdrawalFundCommand}
import com.bigdataconcept.akka.stock.trade.account.domain.Event.{AccountEvent, AddFundEvent, OpenAccountEvent, UpdateAddressEvent, UpdateContactInfoEvent, WithdrawalEvent}
import com.bigdataconcept.akka.stock.trade.account.domain.State.AccountState
import java.util.Date
import akka.cluster.sharding.ShardRegion
import com.bigdataconcept.akka.stock.trade.account.domain.ApiPayload.CommandResponse
import com.bigdataconcept.akka.stock.trade.account.domain.KafkaEvent.{CreatePortfolioEvent, DepositFundEvent, WithDrawFundEvent}
import com.bigdataconcept.akka.stock.trade.account.domain.KafkaProtocol.KafkaMessage
import com.bigdataconcept.akka.stock.trade.account.domain.State.AccountState
import com.bigdataconcept.akka.stock.trade.account.util.EntityIdGenerator
import com.fasterxml.jackson.databind.PropertyNamingStrategy.PropertyNamingStrategyBase
import com.google.gson.Gson


/**
 * @author Oluwaseyi Otun
 */
object AccountEntityActor{

  def props(kafkaPublisher: ActorRef ): Props = Props(new AccountEntityActor(kafkaPublisher))
}
class AccountEntityActor(kafkaPublisher: ActorRef) extends PersistentActor with ActorLogging{

  var accountState = AccountState()

  override def persistenceId: String = if(accountState.accountId == None) self.path.name else accountState.accountId.get

  override def receiveCommand: Receive = {

    case openAccountCommand: OpenAccountCommand => handleOpenAccountCommand(openAccountCommand)

    case addFundCommand: AddFundCommand => handleAddFundCommand(addFundCommand)

    case withdrawalFundCommand: WithdrawalFundCommand => handleWithdrawalFundCommand(withdrawalFundCommand)

    case updateContactInfoCommand: UpdateContactInfoCommand => handleUpdateContactInfoCommand(updateContactInfoCommand)

    case updateAddressCommand: UpdateAddressCommand => handleUpdateAddressCommand(updateAddressCommand)

    case "snapshot" =>
      log.info("saving snapshot")
      saveSnapshot(accountState)

    case ReceiveTimeout => passivate()

  }

  override def receiveRecover: Receive =  {
    case openAcctEvent: OpenAccountEvent =>
      accountState = applyEvent(openAcctEvent)
    case addFundEvent: AddFundEvent =>
      accountState = applyEvent(addFundEvent)
    case withdrawalEvent: WithdrawalEvent =>
      accountState = applyEvent(withdrawalEvent)
    case updateAddressEvent: UpdateAddressEvent =>
      accountState = applyEvent(updateAddressEvent)
    case updateContactInfoEvent: UpdateContactInfoEvent =>
      accountState = applyEvent(updateContactInfoEvent)
    case offer@SnapshotOffer(_, snapshot: AccountState) =>
      log.info("recovering with snapshot: {}", offer)
      accountState = snapshot
  }

  def handleOpenAccountCommand(openAcctCmd: OpenAccountCommand): Unit ={
    log.info("Handle Open Account Command {}", openAcctCmd)
    val portfolioId =  EntityIdGenerator.generatorEntityId()
    val openAccountEvent = OpenAccountEvent(openAcctCmd.accountId,portfolioId,openAcctCmd.name, openAcctCmd.profile,openAcctCmd.contact, openAcctCmd.address, openAcctCmd.openingBalance, new Date())
    persist(openAccountEvent){
      evt =>{

        accountState =   applyEvent(openAccountEvent)
        val createPortfolioEvent = CreatePortfolioEvent(openAccountEvent.accountId,portfolioId,openAcctCmd.name)
        val eventMsg = new Gson().toJson(createPortfolioEvent)
        val kafkaMessage = KafkaMessage(eventMsg, "OPEN_PORTFOLIO")
        publishKafkaEvent(kafkaMessage)
        sender ! CommandResponse(openAcctCmd.accountId, String.format("Account Opened Successfully Account Id %s Portfolio Id %s " , openAcctCmd.accountId, portfolioId))
      }
    }
  }

  def handleAddFundCommand(addFundCommand: AddFundCommand): Unit ={
    log.info("Handle Add Fund Command {}", addFundCommand)
    val addFundEvent = AddFundEvent(addFundCommand.accountId,addFundCommand.funds)
    persist(addFundEvent){
      evt => {
        accountState = applyEvent(addFundEvent)
        val accountId = accountState.accountId.get
        val portfolioId = accountState.portfolioId.get
        val depositFundEvent = DepositFundEvent(accountId, portfolioId, addFundEvent.funds.doubleValue())
        val eventMsg = new Gson().toJson(depositFundEvent)
        val kafkaMessage = KafkaMessage(eventMsg,"DEPOSIT_FUNDS")
        publishKafkaEvent(kafkaMessage)
      }
    }
  }

  def handleWithdrawalFundCommand(withdrawalFundCommand: WithdrawalFundCommand): Unit = {
     log.info("Handle Withdrawal Fund Command {}", withdrawalFundCommand)
     val withdrawalEvent = WithdrawalEvent(withdrawalFundCommand.accountId, withdrawalFundCommand.funds)
     persist(withdrawalEvent){
        evt =>{
             accountState = applyEvent(withdrawalEvent)
             val accountId = accountState.accountId.get
             val portfolioId = accountState.portfolioId.get
             val withdrawalFundEvent = WithDrawFundEvent(accountId, portfolioId,withdrawalEvent.fund.doubleValue())
             val eventMsg = new Gson().toJson(withdrawalFundEvent)
             val kafkaMessage = KafkaMessage(eventMsg, "WITHDRAWAL_FUNDS")
             publishKafkaEvent(kafkaMessage)

        }
     }

  }

  def handleUpdateContactInfoCommand(updateContactInfoCommand: UpdateContactInfoCommand ): Unit ={
    log.info("handle Update Contact Info {}", updateContactInfoCommand)
    val contactInfoEvent = UpdateContactInfoEvent(updateContactInfoCommand.accountId, updateContactInfoCommand.contact)
    persist(contactInfoEvent){
      evt => {
         accountState =   applyEvent(contactInfoEvent)
      }
    }
  }

  def handleUpdateAddressCommand(updateAddressCommand: UpdateAddressCommand): Unit ={
     log.info("Handle Address Update {}", updateAddressCommand)
     val updateAddressEvent = UpdateAddressEvent(updateAddressCommand.accountId,updateAddressCommand.address)
     persist(updateAddressEvent){
        evt =>{
            accountState =  applyEvent(updateAddressEvent)
        }
     }
  }


  def applyEvent(accountEvent: AccountEvent): AccountState =  accountEvent match {
    case OpenAccountEvent(accountId,portfolioId,name, profile, contact, address, openingBalance, openingDate) =>{
      accountState = accountState.copy(accountId = Some(accountId), portfolioId = Some(portfolioId),
        accountBalance = openingBalance,address = Some(address),
        contact =  Some(contact), name = Some(name), profile = Some(profile))
      return accountState
    }
    case AddFundEvent(accountId, funds) =>{
      accountState = accountState.copy(accountId = Some(accountId), accountBalance = accountState.accountBalance + funds)
      return accountState
    }
    case WithdrawalEvent(accountId, fund) =>{
      accountState = accountState.copy(accountId= Some(accountId), accountBalance = accountState.accountBalance - fund)
      return accountState
    }
  }

  def passivate(): Unit ={
    context.parent.tell(new ShardRegion.Passivate(PoisonPill.getInstance), self)
  }

  override def postStop(): Unit ={
    var msg = ""
    if (accountState.accountId == None) {
      msg = String.format("(Account Entity %s not initialized)", self.path.name)
    } else {
      msg = "Account ID: " + accountState.accountId.get
    }
    log.info("Stop passivate {} ", msg)
  }

  def publishKafkaEvent(msg: KafkaMessage): Unit ={
    kafkaPublisher ! msg
  }
}
