package com.bigdataconcept.akka.stock.trade.account.actor

import akka.actor.{ActorLogging, ActorRef, PoisonPill, Props, ReceiveTimeout}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import com.bigdataconcept.akka.stock.trade.account.domain.Commands.{AddFundCommand, CreditAccountCommand, DebitAccountCommand, GetAccountDetailCommand, GetBalanceCommand, OpenAccountCommand, UpdateAddressCommand, UpdateContactInfoCommand, WithdrawalFundCommand}
import com.bigdataconcept.akka.stock.trade.account.domain.Event.{AccountCreditedEvent, AccountDebitedEvent, AccountEvent, AddFundEvent, BalanceEvent, OpenAccountEvent, UpdateAddressEvent, UpdateContactInfoEvent, WithdrawalEvent}
import com.bigdataconcept.akka.stock.trade.account.domain.State.AccountState
import java.util.Date
import akka.cluster.sharding.ShardRegion
import com.bigdataconcept.akka.stock.trade.account.domain.ApiPayload.CommandResponse
import com.bigdataconcept.akka.stock.trade.account.domain.Domain.AccountDetails
import com.bigdataconcept.akka.stock.trade.account.domain.KafkaEvent.{CreatePortfolioEvent, DepositFundEvent, WithDrawFundEvent}
import com.bigdataconcept.akka.stock.trade.account.domain.KafkaProtocol
import com.bigdataconcept.akka.stock.trade.account.domain.KafkaProtocol.KafkaMessage
import com.bigdataconcept.akka.stock.trade.account.domain.State.AccountState
import com.bigdataconcept.akka.stock.trade.account.util.{EntityIdGenerator, SequenceIDGenerator}
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

  override def persistenceId: String = self.path.name

  override def receiveCommand: Receive = {

    case openAccountCommand: OpenAccountCommand => handleOpenAccountCommand(openAccountCommand)

    case addFundCommand: AddFundCommand => handleAddFundCommand(addFundCommand)

    case withdrawalFundCommand: WithdrawalFundCommand => handleWithdrawalFundCommand(withdrawalFundCommand)

    case balanceCmd: GetBalanceCommand => handleGetBalance(balanceCmd)

    case updateContactInfoCommand: UpdateContactInfoCommand => handleUpdateContactInfoCommand(updateContactInfoCommand)

    case updateAddressCommand: UpdateAddressCommand => handleUpdateAddressCommand(updateAddressCommand)

    case  accountDetailCmd : GetAccountDetailCommand => handleGetAccountDetails(accountDetailCmd)

    case creditAccountCmd: CreditAccountCommand => handleCreditAccountCommand(creditAccountCmd)

    case debitAccountCmd: DebitAccountCommand =>  handleDebitAccountCommand(debitAccountCmd)

    case "snapshot" =>
      log.info("saving snapshot")
      saveSnapshot(accountState)

    case ReceiveTimeout => passivate()

  }

  override def receiveRecover: Receive =  {
    case openAcctEvent: OpenAccountEvent =>
      log.info("Recover Open Event")
      applyEvent(openAcctEvent)
    case event: AddFundEvent =>
      log.info("Recover AddFund Event")
      applyEvent(event)
    case event: WithdrawalEvent =>
      log.info("Recover Withdrawal Event")
      applyEvent(event)
     case event: BalanceEvent =>
      log.info("Recover Balance Event")
      applyEvent(event)
    case event: UpdateAddressEvent =>
      log.info("Recover Update Address Event")
      applyEvent(event)
    case event: UpdateContactInfoEvent =>
      log.info("Recover Contact Info Event")
      applyEvent(event)
    case event: AccountCreditedEvent =>
      log.info("Recover Account Credited Event")
      applyEvent(event)
    case event: AccountDebitedEvent =>
      log.info("Recover Account Debited Event")
      applyEvent(event)
    case offer@SnapshotOffer(_, snapshot: AccountState) =>
      log.info("recovering with snapshot: {}", offer)
      accountState = snapshot
    case RecoveryCompleted => log.info("Recover Completed")
  }

  def handleOpenAccountCommand(openAcctCmd: OpenAccountCommand): Unit ={
    log.info("Handle Open Account Command {}", openAcctCmd.openingBalance)
    val portfolioId =  "P-" + new SequenceIDGenerator().getId()
    val openAccountEvent = OpenAccountEvent(openAcctCmd.accountId,portfolioId,openAcctCmd.name, openAcctCmd.profile,openAcctCmd.contact, openAcctCmd.address, openAcctCmd.openingBalance, new Date())
    persist(openAccountEvent){
      evt =>{

        accountState =   applyEvent(evt)
        val createPortfolioEvent = CreatePortfolioEvent(openAccountEvent.accountId,portfolioId,openAcctCmd.name,openAcctCmd.openingBalance.doubleValue())
        val eventMsg = new Gson().toJson(createPortfolioEvent)
        val kafkaMessage = KafkaMessage(eventMsg, KafkaProtocol.OPENPORTFOLIOEVENT)
        publishKafkaEvent(kafkaMessage)
        sender ! CommandResponse(openAcctCmd.accountId, String.format("Account Opened Successfully Account Id %s Portfolio Id %s " , openAcctCmd.accountId, portfolioId))
      }
    }
  }

  def handleAddFundCommand(addFundCommand: AddFundCommand): Unit ={
    log.info("Handle Add Fund Command {}", addFundCommand)
    val addFundEvent = AddFundEvent(addFundCommand.accountId,addFundCommand.funds.doubleValue())
    persist(addFundEvent){
      evt => {
        accountState = applyEvent(evt)
        val accountId = accountState.accountId.get
        val portfolioId = accountState.portfolioId.get
        val depositFundEvent = DepositFundEvent(accountId, portfolioId, addFundEvent.funds.doubleValue())
        val eventMsg = new Gson().toJson(depositFundEvent)
        val kafkaMessage = KafkaMessage(eventMsg, KafkaProtocol.DEPOSITEVENT)
        publishKafkaEvent(kafkaMessage)
        sender ! CommandResponse(addFundCommand.accountId, String.format("Fund Add Account Id %s Amount %s ", addFundCommand.accountId,String.valueOf(addFundCommand.funds.doubleValue())))
      }
    }
  }

  def handleWithdrawalFundCommand(withdrawalFundCmd: WithdrawalFundCommand): Unit = {
     log.info("Handle Withdrawal Fund Command {}", withdrawalFundCmd)
     val withdrawalEvent = WithdrawalEvent(withdrawalFundCmd.accountId, withdrawalFundCmd.funds.doubleValue())
     persist(withdrawalEvent){
        evt =>{
             accountState = applyEvent(evt)
             val accountId = accountState.accountId.get
             val portfolioId = accountState.portfolioId.get
             val withdrawalFundEvent = WithDrawFundEvent(accountId, portfolioId,withdrawalEvent.fund.doubleValue())
             val eventMsg = new Gson().toJson(withdrawalFundEvent)
             val kafkaMessage = KafkaMessage(eventMsg, KafkaProtocol.WITHDRAWEVENT)
             publishKafkaEvent(kafkaMessage)
          sender ! CommandResponse(withdrawalFundCmd.accountId, String.format("WithDraw Fund Account Id %s Amount %s ",withdrawalFundCmd.accountId, String.valueOf(withdrawalFundCmd.funds)))
    }
     }

  }

  def handleUpdateContactInfoCommand(updateContactInfoCommand: UpdateContactInfoCommand ): Unit ={
    log.info("handle Update Contact Info {}", updateContactInfoCommand)
    val contactInfoEvent = UpdateContactInfoEvent(updateContactInfoCommand.accountId, updateContactInfoCommand.contact)
    persist(contactInfoEvent){
      evt => {
         accountState =   applyEvent(contactInfoEvent)
        sender ! CommandResponse(updateContactInfoCommand.accountId,"Contact Info updated")
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
         sender ! CommandResponse(updateAddressCommand.accountId, "Address updated")
     }
  }

   def handleGetAccountDetails(acctDetailCmd: GetAccountDetailCommand): Unit ={
       log.info("Handle Account Details")
       val profile = accountState.profile.get
       val contactInfo = accountState.contact.get
       val address = accountState.address.get
       val balance = accountState.accountBalance.doubleValue()
       val accountDetails = AccountDetails(profile, contactInfo, address, balance)
       val payload = new Gson().toJson(accountDetails);
        sender ! CommandResponse(acctDetailCmd.accountId,payload)
   }

   def handleDebitAccountCommand(debitAccountCommand: DebitAccountCommand): Unit ={
     log.info("Handle Debit Account Command")
     persist(AccountDebitedEvent(debitAccountCommand.accountId,debitAccountCommand.amount.doubleValue(),new Date().getTime)){
       evt => {
            applyEvent(evt)
       }
     }
   }

   def handleCreditAccountCommand(creditAccountCmd: CreditAccountCommand): Unit ={
     log.info("Handle Credit Account Command")
     persist(AccountCreditedEvent(creditAccountCmd.accountId,creditAccountCmd.amount.doubleValue(),new Date().getTime)) {
        evt =>{
              applyEvent(evt)
        }
     }
   }


  def applyEvent(accountEvent: AccountEvent): AccountState =  accountEvent match {
    case OpenAccountEvent(accountId,portfolioId,name, profile, contact, address, openingBalance, openingDate) =>{
      log.info("Applying Open Account Event")
      accountState = accountState.copy(accountId = Some(accountId), portfolioId = Some(portfolioId),
        accountBalance = openingBalance,address = Some(address),
        contact =  Some(contact), name = Some(name), profile = Some(profile))
      return accountState
    }
    case AddFundEvent(accountId, funds) =>{
      log.info("Applying  Add Fund Event")
      accountState = accountState.copy(accountId = Some(accountId), accountBalance = accountState.accountBalance + funds)
      return accountState
    }
    case WithdrawalEvent(accountId, fund) =>{
      log.info("Applying  Withdrawal  Event")
      accountState = accountState.copy(accountId= Some(accountId), accountBalance = accountState.accountBalance - fund)
      return accountState
    }
    case BalanceEvent(accountId) =>{
      log.info("Balance Event")
      accountState = accountState.copy(accountId = Some(accountId))
      return accountState
    }
    case UpdateAddressEvent(accountId, address) =>{
        log.info("Update Address Event")
      accountState = accountState.copy(accountId = Some(accountId), address = Some(address))
      return accountState
    }
    case UpdateContactInfoEvent(accountId,contact) =>{
      log.info("Update Contact Info")
       accountState = accountState.copy(accountId = Some(accountId), contact = Some(contact))
      return accountState
    }
    case AccountCreditedEvent(accountId,amount,transDate) =>{
      log.info("Applying Account Credit Event Account Id {}, Amount{}", accountId,amount)
      accountState = accountState.copy(accountId = Some(accountId), accountBalance = accountState.accountBalance + amount)
       return accountState
    }
    case AccountDebitedEvent(accountId, amount, transDate) =>{
      log.info("Applying Account Debit Event Account Id {}, Amount{} ",accountId,amount)
      accountState = accountState.copy(accountId= Some(accountId), accountBalance = accountState.accountBalance - amount)
      return accountState
    }
    case _ => System.out.println("Unknown")
      return accountState
  }

  def handleGetBalance(balanceCmd: GetBalanceCommand): Unit ={
    log.info("Handle Account Balance {} ", balanceCmd.accountId)
       val balance = String.valueOf(accountState.accountBalance.doubleValue())
    sender ! CommandResponse(balanceCmd.accountId, String.format("Account Id %s   Account Balance  %s  ",accountState.accountId, balance))

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
