package com.bigdataconcept.akka.stock.trade.order.api


import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.bigdataconcept.akka.stock.trade.order.domain.ApiPayload.OrderSummaryResponse
import spray.json.DefaultJsonProtocol


/**
 * @author Oluwaseyi Otun
 *
 */
class TradeOrderJsonSupport extends SprayJsonSupport {

   import DefaultJsonProtocol._

    implicit  val orderSummaryResponse = jsonFormat6(OrderSummaryResponse)


}