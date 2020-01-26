package com.bigdataconcept.akka.stock.trade.account.api

import java.util.Date
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, concat, entity, get, onComplete, parameters, path, post}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.bigdataconcept.akka.stock.trade.account.domain.ApiPayload.{ChangeAddressRequest, ChangeContactRequest, CommandResponse, DepositRequest, OpenAccountRequest, WithdrawalRequest}
import com.bigdataconcept.akka.stock.trade.account.domain.Commands.{AddFundCommand, GetAccountDetailCommand, GetBalanceCommand, OpenAccountCommand, UpdateAddressCommand, UpdateContactInfoCommand, WithdrawalFundCommand}
import com.bigdataconcept.akka.stock.trade.account.util.EntityIdGenerator
import akka.pattern.ask
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import com.bigdataconcept.akka.stock.trade.account.util.SequenceIDGenerator


class AccountServiceAPI(accountShardingRegion: ActorRef)(implicit  actorSystem: ActorSystem) extends AccountJsonSupport {

     implicit  val timeout:Timeout = Timeout(20 seconds)


     val accountRoutes : Route = {
       concat(
         post {
           path("openAccount") {
             actorSystem.log.info("Open Account API")
             entity(as[OpenAccountRequest]) { openAccountRequest =>
               actorSystem.log.info("Open Account Request {}", openAccountRequest)
               val contact = openAccountRequest.contact
               val address = openAccountRequest.address
               val profile = openAccountRequest.profile
               val name = openAccountRequest.name
               val amount = BigDecimal(openAccountRequest.openBalance)
               val openAccountCmd = OpenAccountCommand("A-" + new SequenceIDGenerator().getId(), name, profile, contact, address, amount, new Date())
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
             entity(as[ChangeContactRequest]) { changeContactInfoRequest =>
               actorSystem.log.info("Change Contact Request{}", changeContactInfoRequest)
               val contact = changeContactInfoRequest.contact
               val changeContactCmd = UpdateContactInfoCommand(changeContactInfoRequest.accountId, contact)
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
             actorSystem.log.info("View Balance API")
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
         },
         get {
           path("viewAcct") {
             actorSystem.log.info("View Account Details API")
             parameters("id") {
               (id) => {
                 val acctDetailsCmd = GetAccountDetailCommand(id)
                 onComplete((accountShardingRegion ? acctDetailsCmd).mapTo[CommandResponse]) {
                   case Success(r) => complete(r)
                   case Failure(ex) => complete(StatusCodes.BadRequest, ex.toString)
                 }
               }
             }
           }
         }

       )}


}
