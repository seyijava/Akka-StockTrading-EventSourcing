package com.bigdataconcept.akka.stock.trade.account.api

import java.util.Date

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, concat, entity, get, onComplete, parameters, path, post}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.bigdataconcept.akka.stock.trade.account.domain.ApiPayload.{ChangeAddressRequest, ChangeContactRequest, CommandResponse, DepositRequest, OpenAccountRequest, WithdrawalRequest}
import com.bigdataconcept.akka.stock.trade.account.domain.Commands.{AddFundCommand, GetBalanceCommand, OpenAccountCommand, UpdateAddressCommand, UpdateContactInfoCommand, WithdrawalFundCommand}
import com.bigdataconcept.akka.stock.trade.account.util.EntityIdGenerator
import akka.pattern.ask
import scala.concurrent.duration._
import scala.util.{Failure, Success}



class AccountServiceAPI(accountShardingRegion: ActorRef)(implicit  actorSystem: ActorSystem) extends AccountJsonSupport {

     implicit  val timeout:Timeout = Timeout(10 seconds)


     val accountRoutes : Route = {
       concat(
         post {
           path("open") {
             actorSystem.log.info("Open Account API")
             entity(as[OpenAccountRequest]) { openAccountRequest =>
               actorSystem.log.info("Open Account Request {}", openAccountRequest)
               val contact = openAccountRequest.contact
               val address = openAccountRequest.address
               val profile = openAccountRequest.profile
               val name = openAccountRequest.name
               val amount = BigDecimal(openAccountRequest.openBalance)
               val openAccountCmd = OpenAccountCommand(EntityIdGenerator.generatorEntityId(), name, profile, contact, address, amount, new Date())
               onComplete((accountShardingRegion ? openAccountCmd).mapTo[CommandResponse]) {
                 case Success(r) => complete(r)
                 case Failure(ex) => complete(StatusCodes.BadRequest, ex.toString)
               }
             }
           }
         },
         post {
           path(pm = "deposit") {
             actorSystem.log.info("Deposit Account API")
             entity(as[DepositRequest]) { depositRequest =>
               actorSystem.log.info("Deposit Account Request{}", depositRequest)
               val amount = BigDecimal(depositRequest.funds)
               val accountId = depositRequest.accountId
               val addFundCommand = AddFundCommand(accountId, amount)
               onComplete((accountShardingRegion ? addFundCommand).mapTo[CommandResponse]) {
                 case Success(r) => complete(r)
                 case Failure(ex) => complete(StatusCodes.BadRequest, ex.toString)
               }
             }
           }
         },
         post {
           path(pm = "withdraw") {
             actorSystem.log.info("WithDraw Account API")
             entity(as[WithdrawalRequest]) { withdrawalRequest =>
               actorSystem.log.info("Withdrawal Request{}", withdrawalRequest)
               val accountId = withdrawalRequest.accountId
               val amount = BigDecimal(withdrawalRequest.funds)
               val withdrawalFundCommand = WithdrawalFundCommand(accountId, amount)
               onComplete((accountShardingRegion ? withdrawalFundCommand).mapTo[CommandResponse]) {
                 case Success(r) => complete(r)
                 case Failure(ex) => complete(StatusCodes.BadRequest, ex.toString)
               }
             }
           }
         },
         post {
           path(pm = "updateContactInfo") {
             actorSystem.log.info("Change Contact Info API")
             entity(as[ChangeContactRequest]) { changeAddressRequest =>
               actorSystem.log.info("Change Contact Request{}", changeAddressRequest)
               val contact = changeAddressRequest.contact
               val changeContactCmd = UpdateContactInfoCommand(changeAddressRequest.accountId, contact)
               onComplete((accountShardingRegion ? changeContactCmd).mapTo[CommandResponse]) {
                 case Success(r) => complete(r)
                 case Failure(ex) => complete(StatusCodes.BadRequest, ex.toString)
               }
             }
           }
         },
         post {
           path(pm = "updateAddr") {
             actorSystem.log.info("Change Address API")
             entity(as[ChangeAddressRequest]) { changeAddressRequest =>
               val address = changeAddressRequest.address
               val accountId = changeAddressRequest.accountId
               val updateAddressCmd = UpdateAddressCommand(accountId, address)
               onComplete((accountShardingRegion ? updateAddressCmd).mapTo[CommandResponse]) {
                 case Success(r) => complete(r)
                 case Failure(ex) => complete(StatusCodes.BadRequest, ex.toString)
               }
             }
           }
         },
         get {
           path("viewBalance") {
             parameters("id") {
               (id) => {
                 val balanceCmd = GetBalanceCommand(id)
                 onComplete((accountShardingRegion ? balanceCmd).mapTo[CommandResponse]) {
                   case Success(r) => complete(r)
                   case Failure(ex) => complete(StatusCodes.BadRequest, ex.toString)
                 }
               }
             }
           }
         }

       )}


}
