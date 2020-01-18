package com.bigdataconcept.akka.stock.trade.order.api

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, concat, get, onComplete, parameters, path}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.bigdataconcept.akka.stock.trade.order.domain.ApiPayload.OrderSummaryResponse
import com.bigdataconcept.akka.stock.trade.order.domain.Commands.{ GetOrderStatusCommand, GetOrderSummaryCommand}
import spray.json.DefaultJsonProtocol
import scala.concurrent.duration._
import akka.pattern.ask
import scala.util.{Failure, Success}

/**
 * @author Oluwaseyi Otun
 */
class TradeOrderRoutes(tradeOrderShardingRegion: ActorRef)(implicit actorSystem: ActorSystem) extends TradeOrderJsonSupport {


  import DefaultJsonProtocol._
  implicit val timeout: Timeout = Timeout(15 seconds)

  val tradeOrderRoutes: Route =
    concat(
    get{
      path("orderStatus" ) {
        parameters("orderId") {
          (orderId) =>{
            val getOrderStatus = GetOrderStatusCommand(orderId)
            onComplete((tradeOrderShardingRegion ? getOrderStatus ).mapTo[OrderSummaryResponse]){
              case Success(r) => complete(r)
              case Failure(ex) => complete(StatusCodes.BadRequest,ex.toString)
            }
          }
        }
      }
      },
      get{
        path("orderSummary" ) {
          parameters("orderId") {
            (orderId) =>{
              val orderSummary = GetOrderSummaryCommand(orderId)
              onComplete((tradeOrderShardingRegion ? orderSummary ).mapTo[OrderSummaryResponse]){
                case Success(r) => complete(r)
                case Failure(ex) => complete(StatusCodes.BadRequest,ex.toString)
              }
            }
          }
        }
      })



}
