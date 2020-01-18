package com.bigdataconcept.akka.stock.trade.portfolio

import akka.actor.ActorSystem
import akka.actor.ActorRef
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.bigdataconcept.akka.stock.trade.portfolio.actor.PortfolioShardingManager.setupClusterSharding
import com.bigdataconcept.akka.stock.trade.portfolio.api.{HttpServerRoutes, PortfolioServiceRouteAPI}
import com.bigdataconcept.akka.stock.trade.portfolio.kafka.TradeOrderPubSubActor
import com.bigdataconcept.akka.stock.trade.portfolio.kafka.KafkaPublisher


/**
 *
 */
object PortfolioServiceMain {

   def main(args: Array[String]): Unit={

      startupClusterNodes()
   }


  def startupClusterNodes(): Unit = {
    val config = ConfigFactory.load()
    implicit val actorSystem = ActorSystem.create("Portfolio-Cluster", config)
    val inboundTradeOrderTopic = config.getString("kafka.inboundTradeOrderTopic")
    val outboundTradeOrderTopic = config.getString("kafka.outboundTradeOrderTopic")
    val inboundTradeOrderTopicGroupId = config.getString("kafka.inboundTradeOrderTopicGroupId")
    val portfolioKafkaProducer = actorSystem.actorOf(KafkaPublisher.prop(outboundTradeOrderTopic))
    val portfolioShardingRegion = setupClusterSharding(actorSystem, portfolioKafkaProducer)
    val tradeOrderPubSubActor =  actorSystem.actorOf(TradeOrderPubSubActor.prop(inboundTradeOrderTopic,inboundTradeOrderTopicGroupId,portfolioShardingRegion),"TradeOrderPubSubActor")
    val httpPort = config.getInt("rest.port")
    val ippAddress = config.getString("rest.host")
    startHttpServer(portfolioShardingRegion,httpPort,ippAddress)

  }


  def startHttpServer(portfolioShardingRegion: ActorRef, httpPort: Int, ipAddress:String)(implicit actorSystem: ActorSystem):Unit={
    val routes = new PortfolioServiceRouteAPI(portfolioShardingRegion)
    new HttpServerRoutes(route= routes.portfolioRoute,port = httpPort,host = ipAddress).start()
  }

}
