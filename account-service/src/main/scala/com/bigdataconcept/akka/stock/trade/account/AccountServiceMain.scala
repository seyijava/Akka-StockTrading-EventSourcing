package com.bigdataconcept.akka.stock.trade.account

import akka.actor.{ActorRef, ActorSystem}
import com.bigdataconcept.akka.stock.trade.account.api.{AccountServiceAPI, HttpServerRoutes}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.bigdataconcept.akka.stock.trade.account.actor.AccountShardingManager.setupClusterSharding
import com.bigdataconcept.akka.stock.trade.account.domain.ApiPayload.OpenAccountRequest
import com.bigdataconcept.akka.stock.trade.account.domain.Domain.{Address, Contact, Profile}
import com.bigdataconcept.akka.stock.trade.account.kafka.KafkaPublisher
import com.bigdataconcept.akka.stock.trade.account.util.SequenceIDGenerator
import com.google.gson.Gson
import com.bigdataconcept.akka.stock.trade.account.kafka.PortfolioSubscriberActor

object AccountServiceMain {

    def main(args: Array[String]): Unit ={

        startupClusterNodes()
        //val generator = new SequenceIDGenerator()
        //System.out.println(generator.getId())

        /**
        val address = new Address("70 Gambia Street", "EIGS57", "NB")
        val contact = new Contact("seyijava@gmail.com", "5063238810")
        val profile = new Profile("oluwaseyi", "otun")

        val openAccountRequest = new OpenAccountRequest("BullTrader", profile, contact,address, 8000.00);
        System.out.println(new Gson().toJson(openAccountRequest))*/
    }

    def startupClusterNodes(): Unit = {
        val config = ConfigFactory.load()
        implicit val actorSystem = ActorSystem.create("Account-Cluster", config)
        val inboundAccountTopic = config.getString("kafka.inboundAccountTopic")
        val inboundAccountTopicGroupId= config.getString("kafka.inboundAccountGroupId")
        val outboundAccountTopic = config.getString("kafka.outboundAccountTopic")
        val kafkaPublisher = actorSystem.actorOf(KafkaPublisher.prop(outboundAccountTopic), "KafkaPublisher")
        val accountShardingRegion = setupClusterSharding(actorSystem,kafkaPublisher)
        val portfolioSubscriberActor = actorSystem.actorOf(PortfolioSubscriberActor.prop(accountShardingRegion,inboundAccountTopic,inboundAccountTopicGroupId), name =  "PortfolioSubscriberActor")
        val httpPort = config.getInt("rest.port")
        val ippAddress = config.getString("rest.host")
        startHttpServer(accountShardingRegion,httpPort,ippAddress)(actorSystem)
    }



    def startHttpServer(accountShardingRegion: ActorRef, httpPort: Int, ipAddress:String)(implicit actorSystem: ActorSystem):Unit={
        val routes = new AccountServiceAPI(accountShardingRegion)
        new HttpServerRoutes(route= routes.accountRoutes,port = httpPort,host = ipAddress).start()
    }
}
