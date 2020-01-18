package com.bigdataconcept.akka.stock.trade.account

import akka.actor.{ActorRef, ActorSystem}
import com.bigdataconcept.akka.stock.trade.account.api.{AccountServiceAPI, HttpServerRoutes}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.bigdataconcept.akka.stock.trade.account.actor.AccountShardingManager.setupClusterSharding
import com.bigdataconcept.akka.stock.trade.account.kafka.KafkaPublisher

object AccountServiceMain {

    def main(args: Array[String]): Unit ={
        startupClusterNodes()
    }

    def startupClusterNodes(): Unit = {
        val config = ConfigFactory.load()
        implicit val actorSystem = ActorSystem.create("Account-Cluster", config)
        val inboundAccountTopic = config.getString("kafka.inboundAccountTopic")
        val outboundAccountTopic = config.getString("outboundAccountTopic")
        val kafkaPublisher = actorSystem.actorOf(KafkaPublisher.prop(outboundAccountTopic), "KafkaPublisher")
        val accountShardingRegion = setupClusterSharding(actorSystem,kafkaPublisher)
        val
        val httpPort = config.getInt("rest.port")
        val ippAddress = config.getString("rest.host")
        startHttpServer(accountShardingRegion,httpPort,ippAddress)(actorSystem)
    }



    def startHttpServer(accountShardingRegion: ActorRef, httpPort: Int, ipAddress:String)(implicit actorSystem: ActorSystem):Unit={
        val routes = new AccountServiceAPI(accountShardingRegion)
        new HttpServerRoutes(route= routes.accountRoutes,port = httpPort,host = ipAddress).start()
    }
}
