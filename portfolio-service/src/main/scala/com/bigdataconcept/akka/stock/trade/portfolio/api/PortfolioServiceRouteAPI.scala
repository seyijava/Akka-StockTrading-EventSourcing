package com.bigdataconcept.akka.stock.trade.portfolio.api

import com.bigdataconcept.akka.stock.trade.portfolio.domain.Commands.{AcceptRefundCommand, CommandResponse, GetPortfolioView, OpenPortfolioCommand, PlaceOrderCommand, PortfolioView, ReceiveFundsCommand, SendFundsCommand}
import akka.actor.ActorRef
import akka.util.Timeout

import scala.concurrent.duration._
import akka.http.scaladsl.server.Route
import com.bigdataconcept.akka.stock.trade.portfolio.domain.ApiPayload.{DepositRequest, OpenPortfolioRequest, OpenPortfolioResponse, PlaceBuyOrderRequest, PlaceSellOrderRequest, PortfolioViewResponse, RefundRequest, WithdrawalRequest}
import akka.pattern.ask
import akka.http.scaladsl.server.Directives._

import scala.util.Failure
import scala.util.Success
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Domain.{Market, OrderDetails}
import com.bigdataconcept.akka.stock.trade.portfolio.domain.TradeType
import com.bigdataconcept.akka.stock.trade.portfolio.util.{EntityIdGenerator, SequenceIDGenerator}


/**
 * @author Oluwaseyi Otun
 * @param portfolioShardingRegion
 * @param actorSystem
 */
class PortfolioServiceRouteAPI(portfolioShardingRegion: ActorRef)(implicit actorSystem: ActorSystem) extends PortfolioJsonSupport {

       implicit val timeout: Timeout = Timeout(15 seconds)

       val portfolioRoute: Route =
        concat(
          post{
             path("placeBuyOrder"){
                actorSystem.log.info("Place Buy Order Portfolio API Call")
                entity(as[PlaceBuyOrderRequest]){ placeBuyOrderRequest =>
                   val tradeType =  "BUY"
                   val mktOrder = Market()
                   val orderId = "ORD-" + new SequenceIDGenerator().getId
                  actorSystem.log.info("Symbol {}" , placeBuyOrderRequest.symbol)
                   val orderDetail =  OrderDetails(placeBuyOrderRequest.symbol,placeBuyOrderRequest.numberShare, mktOrder, tradeType)
                   val placeOrderCommand = PlaceOrderCommand(placeBuyOrderRequest.portfolioId, orderId, orderDetail)
                   onComplete((portfolioShardingRegion ? placeOrderCommand).mapTo[CommandResponse]){
                     case Success(r) => complete(r)
                     case Failure(ex) => complete(StatusCodes.BadRequest,ex.toString)
                   }
                }
             }
          },
          post{
            path("placeSellOrder"){
              actorSystem.log.info("Place Sell Order Portfolio API Call")
              entity(as[PlaceSellOrderRequest]){ placeSellOrderRequest =>
                val tradeType =   "SELL"
                val mktOrder = Market()
                val orderId = "ORD-" + new SequenceIDGenerator().getId
                val purchaseOrderId = placeSellOrderRequest.purchaseOrderId
                actorSystem.log.info("Symbol {}" , placeSellOrderRequest.symbol)
                val orderDetail =  OrderDetails(placeSellOrderRequest.symbol,placeSellOrderRequest.numberShare, mktOrder, tradeType,Some(purchaseOrderId))
                val placeOrderCommand = PlaceOrderCommand(placeSellOrderRequest.portfolioId, orderId, orderDetail)
                onComplete((portfolioShardingRegion ? placeOrderCommand).mapTo[CommandResponse]){
                  case Success(r) => complete(r)
                  case Failure(ex) => complete(StatusCodes.BadRequest,ex.toString)
                }
              }
            }
          },
          get{
            path("view" ) {
              parameters("id") {
                (id) =>{
                  val getPortfolioView = GetPortfolioView(id)
                  onComplete((portfolioShardingRegion ? getPortfolioView ).mapTo[PortfolioViewResponse]){
                    case Success(r) => complete(r)
                    case Failure(ex) => complete(StatusCodes.BadRequest,ex.toString)
                  }
                }
              }
            }

          }


        )

}
