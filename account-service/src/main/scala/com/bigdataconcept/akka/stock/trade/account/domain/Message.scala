package com.bigdataconcept.akka.stock.trade.account.domain

import java.util.Date

import com.bigdataconcept.akka.stock.trade.account.domain.Domain.{Address, Contact, Profile}

object Commands {


        trait  AccountCommand extends  Serializable

        case class OpenAccountCommand(accountId: String, name: String, profile: Profile, contact: Contact, address: Address, openingBalance: BigDecimal, openingDate: Date) extends  AccountCommand

        case class AddFundCommand(accountId: String, funds:  BigDecimal) extends  AccountCommand

        case class WithdrawalFundCommand(accountId: String, funds: BigDecimal) extends  AccountCommand

        case class UpdateContactInfoCommand(accountId: String, contact: Contact) extends  AccountCommand

        case class UpdateAddressCommand(accountId: String, address: Address) extends  AccountCommand

        case class GetBalanceCommand(accountId: String) extends  AccountCommand



}

object Domain{

       case class Address(street: String, postalCode: String, provide: String) extends  Serializable
       case class Contact(email: String, phoneNumber: String) extends  Serializable
       case class Profile(name: String, surname: String, address: Address, contact: Contact) extends Serializable
}

object Event{

        trait AccountEvent extends  Serializable

        case class OpenAccountEvent(accountId: String, portfolioId: String, name: String, profile: Profile, contact: Contact, address: Address, openingBalance: BigDecimal, openingDate: Date) extends AccountEvent

        case class AddFundEvent(accountId: String, funds : BigDecimal) extends  AccountEvent

        case class WithdrawalEvent(accountId: String, fund: BigDecimal) extends  AccountEvent

        case class  UpdateContactInfoEvent(accountId: String, contact: Contact) extends  AccountEvent

        case class  UpdateAddressEvent(accountId: String, address: Address) extends  AccountEvent


}

object State{
       case class AccountState(accountId: Option[String] = None, portfolioId: Option[String] = None, name: Option[String] = None,  accountBalance: BigDecimal = BigDecimal(0.0d) ,
                               profile: Option[Profile] = None, contact: Option[Contact]=None, address: Option[Address] = None)
}


object KafkaProtocol{
  case class KafkaMessage(payload: String, msgType: String) extends Serializable
}



object ApiPayload{
  case class OpenAccountRequest(name: String, profile: Profile, contact: Contact, address: Address,openBalance: Double) extends Serializable
  case class DepositRequest(accountId: String, funds: Double) extends  Serializable
  case class WithdrawalRequest(accountId: String,  funds: Double) extends Serializable
  case class ChangeContactRequest(accountId: String, contact: Contact) extends  Serializable
  case class ChangeAddressRequest(accountId: String, address: Address) extends  Serializable
  case class GetAccountViewRequest(accountId: String) extends  Serializable
  case class CommandResponse(accountId: String, responseMessage: String) extends  Serializable
}

object KafkaEvent{
      case class CreatePortfolioEvent(accountId: String, portfolio: String, name: String) extends  Serializable
      case class DepositFundEvent(accountId: String ,portfolioId: String,  amount: Double) extends Serializable
      case class WithDrawFundEvent(accountId: String ,portfolioId: String,  amount: Double) extends  Serializable
}
