package com.bigdataconcept.akka.stock.trade.order

import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.config.ConfigFactory
import com.bigdataconcept.akka.stock.trade.order.actor.OrderShardingManager.setupClusterSharding
import com.bigdataconcept.akka.stock.trade.order.actor.TradeServiceActor
import com.bigdataconcept.akka.stock.trade.order.api.{HttpServerRoutes, TradeOrderRoutes}
import com.bigdataconcept.akka.stock.trade.order.kafka.{KafkaPublisherActor, TradeOrderPubSubActor}

object TradeOrderMainService{

 def main(args: Array[String]): Unit={

  startupClusterNodes()
 }


 def startupClusterNodes(): Unit = {
  val config = ConfigFactory.load()
  implicit val actorSystem = ActorSystem.create("TradeOrder-Cluster", config)
  val inboundTradeOrderTopic = config.getString("kafka.inboundTradeOrderTopic")
  val outboundTradeOrderTopic = config.getString( "kafka.outboundTradeOrderTopic")
  val inboundTradeOrderTopicGroupId = config.getString("kafka.inboundTradeOrderTopicGroupId")
  val messagePublisherActor = actorSystem.actorOf(KafkaPublisherActor.props(outboundTradeOrderTopic), name = "kafkaPublisherActor")
  val tradeOrderShardRegion = setupClusterSharding(actorSystem,messagePublisherActor)
  actorSystem.actorOf(TradeServiceActor.props(tradeOrderShardRegion), name = "TradeServiceActor")
  actorSystem.actorOf(TradeOrderPubSubActor.props(inboundTradeOrderTopic, inboundTradeOrderTopicGroupId,tradeOrderShardRegion), "TradeOrderPubSubActor")
  val httpPort = config.getInt("rest.port")
  val ippAddress = config.getString("rest.host")
  startHttpServer(tradeOrderShardRegion, httpPort,ippAddress)
 }


 def startHttpServer(tradeOrderShardRegion: ActorRef,httpPort: Int, ipAddress:String)(implicit actorSystem: ActorSystem):Unit={
         val routes = new TradeOrderRoutes(tradeOrderShardRegion).tradeOrderRoutes
         new HttpServerRoutes(routes, httpPort,ipAddress).start()
 }

 }
