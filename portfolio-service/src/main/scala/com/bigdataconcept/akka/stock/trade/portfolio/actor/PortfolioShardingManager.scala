package com.bigdataconcept.akka.stock.trade.portfolio.actor

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ClusterSharding
import akka.cluster.sharding.ClusterShardingSettings
import com.bigdataconcept.akka.stock.trade.portfolio.domain.Commands.{AcceptRefundCommand, AcknowledgeOrderFailureCommand, ClosePortfolioCommand, CompleteTradeCommand, GetPortfolioView, OpenPortfolioCommand, PlaceOrderCommand, PortfolioCommand, ReceiveFundsCommand, SendFundsCommand}
import com.bigdataconcept.akka.stock.trade.portfolio.actor.PortfolioEntityActor

/**
 *@author Oluwaseyi Otun
 */
object PortfolioShardingManager {
    val numberOfShards = 100



  /**
   *
   * @param system
   * @param messagePublisherActor
   * @return
   */
  def setupClusterSharding(system: ActorSystem, tradeOrderEventPublisher: ActorRef, accountEventPublisher: ActorRef): ActorRef = {
    val settings = ClusterShardingSettings.create(system)
    ClusterSharding(system).start(
      typeName = "PortfolioSharding",
      entityProps = PortfolioEntityActor.props(tradeOrderEventPublisher, accountEventPublisher),
      settings = settings,
      extractEntityId = extractEntityId,
      extractShardId = extractShardId)
  }

  /**
   *
   * @param command
   * @return
   */
  def extractShardIdFromCommand(command: Object): String = {
    if (command.isInstanceOf[OpenPortfolioCommand]) {
      return command.asInstanceOf[OpenPortfolioCommand].portfolioId.hashCode() % numberOfShards + ""
    }
    if (command.isInstanceOf[PlaceOrderCommand]) {
      return command.asInstanceOf[PlaceOrderCommand].portfolioId.hashCode() % numberOfShards + ""
    }
    if (command.isInstanceOf[SendFundsCommand]) {
      return command.asInstanceOf[SendFundsCommand].portfolioId.hashCode() % numberOfShards + ""
    }
    if (command.isInstanceOf[ReceiveFundsCommand]) {
      return command.asInstanceOf[ReceiveFundsCommand].portfolioId.hashCode() % numberOfShards + ""
    }
    if (command.isInstanceOf[AcceptRefundCommand]) {
      return command.asInstanceOf[AcceptRefundCommand].portfolioId.hashCode() % numberOfShards + ""
    }
    if (command.isInstanceOf[AcknowledgeOrderFailureCommand]) {
      return command.asInstanceOf[AcknowledgeOrderFailureCommand].portfolioId.hashCode() % numberOfShards + ""
    }
    if (command.isInstanceOf[ClosePortfolioCommand]) {
      return command.asInstanceOf[ClosePortfolioCommand].portfolioId.hashCode() % numberOfShards + ""
    }
    if(command.isInstanceOf[GetPortfolioView]){
       return command.asInstanceOf[GetPortfolioView].portfolioId.hashCode() % numberOfShards + ""
    }
    if(command.isInstanceOf[CompleteTradeCommand]){
      return command.asInstanceOf[CompleteTradeCommand].portfolioId.hashCode() % numberOfShards + ""
    }
    return null
  }

  /**
   *
   */
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case portfolioCommand: PortfolioCommand => (extractShardIdFromCommand(portfolioCommand), portfolioCommand)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case portfolioCommand: PortfolioCommand => extractShardIdFromCommand(portfolioCommand)
  }

}
