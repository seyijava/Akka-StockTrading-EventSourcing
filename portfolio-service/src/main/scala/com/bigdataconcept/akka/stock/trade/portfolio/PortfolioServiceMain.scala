package com.bigdataconcept.akka.stock.trade.portfolio

import akka.actor.ActorSystem
import akka.actor.ActorRef
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.bigdataconcept.akka.stock.trade.portfolio.actor.PortfolioShardingManager.setupClusterSharding
import com.bigdataconcept.akka.stock.trade.portfolio.api.{HttpServerRoutes, PortfolioServiceRouteAPI}
import com.bigdataconcept.akka.stock.trade.portfolio.kafka.{AccountEventSubscriberActor, KafkaPublisher, TradeOrderPubSubActor}


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

    val outboundAccountTopic = config.getString("kafka.outboundAccountTopic")


    val inboundAccountTopic = config.getString("kafka.inboundAccountTopic")
    val inboundAccountTopicGroupId = config.getString("kafka.inboundAccountTopicGroupId")

    val tradeOrderEventPublisher = actorSystem.actorOf(KafkaPublisher.prop(outboundTradeOrderTopic))
    val accountEventPublisher = actorSystem.actorOf(KafkaPublisher.prop(outboundAccountTopic))

    val portfolioShardingRegion = setupClusterSharding(actorSystem, tradeOrderEventPublisher, accountEventPublisher)
    val tradeOrderPubSubActor =  actorSystem.actorOf(TradeOrderPubSubActor.prop(inboundTradeOrderTopic,inboundTradeOrderTopicGroupId,portfolioShardingRegion),"TradeOrderPubSubActor")
    val accountPubSubActor = actorSystem.actorOf(AccountEventSubscriberActor.props(inboundAccountTopic, inboundAccountTopicGroupId,portfolioShardingRegion), name = "AccountSubActor")
    val httpPort = config.getInt("rest.port")
    val ippAddress = config.getString("rest.host")
    startHttpServer(portfolioShardingRegion,httpPort,ippAddress)

  }


  def startHttpServer(portfolioShardingRegion: ActorRef, httpPort: Int, ipAddress:String)(implicit actorSystem: ActorSystem):Unit={
    val routes = new PortfolioServiceRouteAPI(portfolioShardingRegion)
    new HttpServerRoutes(route= routes.portfolioRoute,port = httpPort,host = ipAddress).start()
  }

}
