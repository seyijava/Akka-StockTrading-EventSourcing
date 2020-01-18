package com.bigdataconcept.akka.stock.trade.account.api
import spray.json.DefaultJsonProtocol
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.bigdataconcept.akka.stock.trade.account.domain.ApiPayload.{ChangeAddressRequest, ChangeContactRequest, CommandResponse, DepositRequest, GetAccountViewRequest, OpenAccountRequest, WithdrawalRequest}
import com.bigdataconcept.akka.stock.trade.account.domain.Domain.{Address, Contact, Profile}

class AccountJsonSupport extends  SprayJsonSupport{

   import DefaultJsonProtocol._
   implicit val contact = jsonFormat2(Contact)
   implicit val address = jsonFormat3(Address)
   implicit val profile = jsonFormat4(Profile)
   implicit val openAccountRequest = jsonFormat5(OpenAccountRequest)
   implicit val depositRequest = jsonFormat2(DepositRequest)
   implicit val withdrawalRequest = jsonFormat2(WithdrawalRequest)
   implicit  val changeAddressRequest = jsonFormat2(ChangeAddressRequest)
   implicit val changeContactRequest = jsonFormat2(ChangeContactRequest)
   implicit val getAccountViewRequest = jsonFormat1(GetAccountViewRequest)
   implicit val commandResponse = jsonFormat2(CommandResponse)

}
