package com.bigdataconcept.akka.stock.trade.portfolio.api

import com.bigdataconcept.akka.stock.trade.portfolio.domain.Commands.{AcceptRefundCommand,CommandResponse,PortfolioView, GetPortfolioView, OpenPortfolioCommand, PlaceOrderCommand, ReceiveFundsCommand, SendFundsCommand}
import akka.actor.ActorRef
import akka.util.Timeout
import scala.concurrent.duration._
import akka.http.scaladsl.server.Route
import com.bigdataconcept.akka.stock.trade.portfolio.domain.ApiPayload.{DepositRequest, OpenPortfolioRequest, OpenPortfolioResponse, PlaceOrderRequest, RefundRequest, WithdrawalRequest}
import akka.pattern.ask
import akka.http.scaladsl.server.Directives._
import scala.util.Failure
import scala.util.Success
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Domain.{Market, OrderDetails}
import com.bigdataconcept.akka.stock.trade.portfolio.domain.TradeType
import com.bigdataconcept.akka.stock.trade.portfolio.domain.ApiPayload.PortfolioViewResponse
import com.bigdataconcept.akka.stock.trade.portfolio.util.EntityIdGenerator


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
             path("placeOrder"){
                actorSystem.log.info("Place Order Portfolio API Call")
                entity(as[PlaceOrderRequest]){ placeOrderRequest =>
                   val tradeType = if(placeOrderRequest.tradeType == 1)  "SELL"  else "BUY"
                   val mktOrder = Market()
                   val orderId = EntityIdGenerator.generateOrderId()
                   System.out.println("Symbol>>>>>>>>>>>>>>>>>>>>>>>>>>>" + placeOrderRequest.symbol)
                   val orderDetail =  OrderDetails(placeOrderRequest.symbol,placeOrderRequest.numberShare, mktOrder, tradeType)
                   val placeOrderCommand = PlaceOrderCommand(placeOrderRequest.portfolioId, orderId, orderDetail)
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
                  onComplete((portfolioShardingRegion ? getPortfolioView ).mapTo[PortfolioView]){
                    case Success(r) => complete(r)
                    case Failure(ex) => complete(StatusCodes.BadRequest,ex.toString)
                  }
                }
              }
            }

          }


        )

}
