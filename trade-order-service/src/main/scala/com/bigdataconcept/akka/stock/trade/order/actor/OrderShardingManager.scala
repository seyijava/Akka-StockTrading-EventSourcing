package com.bigdataconcept.akka.stock.trade.order.actor

import akka.cluster.sharding.ShardRegion
import com.bigdataconcept.akka.stock.trade.order.domain.Commands.{CompleteOrderCommand, OrderCommand, PlaceOrderCommand}
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.cluster.sharding.ClusterShardingSettings
import akka.cluster.sharding.ClusterSharding
import akka.cluster.sharding.ShardRegion
import com.bigdataconcept.akka.stock.trade.order.actor.OrderActorEntity

/**
 *  @author Oluwaseyi Otun
 *          Order Sharding Manager create the ShardingRegion Proxy
 */
object OrderShardingManager {

  val numberOfShards = 100

  /**
   *
   * @param command
   * @return String
   *
   *         Calculate sharding Id by extracting command ID
   */
  def extractShardIdFromCommand(command: Object): String = {
    if(command.isInstanceOf[PlaceOrderCommand]){
      return command.asInstanceOf[PlaceOrderCommand].orderId.hashCode % numberOfShards + ""
    }
    if(command.isInstanceOf[CompleteOrderCommand]){
      return command.asInstanceOf[CompleteOrderCommand].orderId.hashCode % numberOfShards + ""
    }
    return null
  }

  /**
   *  Extract shard Entity
   */
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case orderCommand: OrderCommand => (extractShardIdFromCommand(orderCommand), orderCommand)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case orderCommand: OrderCommand => extractShardIdFromCommand(orderCommand)
  }


  /**
   *
   * @param system
   * @param messagePublisherActor
   * @return ShardRegion Actor Ref
   *         set up  Cluster Sharding
   */
  def setupClusterSharding(system: ActorSystem,messagePublisherActor:ActorRef): ActorRef = {
    val settings = ClusterShardingSettings.create(system)

    ClusterSharding(system).start(
      typeName = "TradeOrderSharding",

      entityProps = OrderActorEntity.props(messagePublisherActor),
      settings = settings,
      extractEntityId = extractEntityId,
      extractShardId = extractShardId)
  }
}
