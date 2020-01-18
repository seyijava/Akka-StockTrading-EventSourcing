package com.bigdataconcept.akka.stock.trade.portfolio.api

import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Commands.ClosePortfolioCommand
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Commands.PlaceOrderCommand
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Commands.OpenPortfolioCommand
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Commands.ReceiveFundsCommand
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Domain.OrderDetails
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Domain.OrderType
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Domain.Trade
import com.bigdataconcept.akka.stock.trade.portfolio.domain.TradeType.TradeType
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Domain.Limit
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Domain.Market
import com.bigdataconcept.akka.stock.trade.portfolio.domain.TradeType
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Commands.SendFundsCommand
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Commands.AcceptRefundCommand
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Domain.OrderDetails
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Domain.OrderType
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Domain.Market
import spray.json
import com.bigdataconcept.akka.stock.trade.portfolio.domain.ApiPayload.OpenPortfolioRequest
import com.bigdataconcept.akka.stock.trade.portfolio.domain.ApiPayload.OpenPortfolioResponse
import com.bigdataconcept.akka.stock.trade.portfolio.domain.ApiPayload.RefundRequest
import com.bigdataconcept.akka.stock.trade.portfolio.domain.ApiPayload.DepositRequest
import com.bigdataconcept.akka.stock.trade.portfolio.domain.ApiPayload.WithdrawalRequest
import com.bigdataconcept.akka.stock.trade.portfolio.domain.ApiPayload.PlaceOrderRequest
import com.bigdataconcept.akka.stock.trade.portfolio.domain.ApiPayload.PortfolioViewResponse
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Commands.CommandResponse
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Commands.PortfolioView

class EnumJsonConverter[T <: scala.Enumeration](enu: T) extends RootJsonFormat[T#Value] {

  def write(obj: T#Value) = JsString(obj.toString)

  def read(json: JsValue) = {
    json match {
      case JsString(txt) => enu.withName(txt)
      case something => deserializationError(s"Expected a value from enum $enu instead of $something")
    }
  }
}



class PortfolioJsonSupport extends SprayJsonSupport {

  import DefaultJsonProtocol._

   implicit  val commandResponse = jsonFormat2(CommandResponse)
   implicit  val portfolioView  = jsonFormat2(PortfolioView)
   implicit  val openPortfolioRequest = jsonFormat1(OpenPortfolioRequest)
   implicit  val openPortfolioResponse = jsonFormat1(OpenPortfolioResponse)
   implicit  val refundRequest = jsonFormat2(RefundRequest)
   implicit  val withdrawalRequest = jsonFormat2(WithdrawalRequest)
   implicit  val depositRequest = jsonFormat2(DepositRequest)
   implicit  val placeOrderRequest = jsonFormat4(PlaceOrderRequest)
   implicit val  portfolioViewResponse = jsonFormat1(PortfolioViewResponse)


   implicit  val limit = jsonFormat1(Limit)
   implicit  val tradeType = new EnumJsonConverter(TradeType)
   //implicit  val orderType =  jsonFormat(OrderType)

//   implicit  val orderDetails = jsonFormat4(OrderDetails)

   implicit val closePortfolioCommand = jsonFormat1(ClosePortfolioCommand)
   implicit val receiveFundCommand = jsonFormat3(ReceiveFundsCommand)
   implicit val sendFundsCommand = jsonFormat3(SendFundsCommand)
   implicit val acceptRefundCommand = jsonFormat3(AcceptRefundCommand)

}
