package com.bigdataconcept.akka.stock.trade.account.actor

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ClusterShardingSettings
import akka.cluster.sharding.ClusterSharding
import com.bigdataconcept.akka.stock.trade.account.domain.Commands.{AccountCommand, AddFundCommand, OpenAccountCommand, UpdateAddressCommand, UpdateContactInfoCommand, WithdrawalFundCommand}


object AccountShardingManager {

   val numberOfShards = 100

  /**
   *
   * @param command
   * @return
   */
  def extractShardIdFromCommand(command: Object): String = {

    if(command.isInstanceOf[OpenAccountCommand]){
      return command.asInstanceOf[OpenAccountCommand].accountId.hashCode() % numberOfShards + ""
    }
    if(command.isInstanceOf[AddFundCommand]){
      return command.asInstanceOf[AddFundCommand].accountId.hashCode() % numberOfShards + ""
    }
    if(command.isInstanceOf[WithdrawalFundCommand]){
      return command.asInstanceOf[WithdrawalFundCommand].accountId.hashCode() % numberOfShards + ""
    }
    if(command.isInstanceOf[UpdateContactInfoCommand]){
      return command.asInstanceOf[UpdateContactInfoCommand].accountId.hashCode() % numberOfShards + ""
    }
    if(command.isInstanceOf[UpdateAddressCommand]){
      return command.asInstanceOf[UpdateAddressCommand].accountId.hashCode() % numberOfShards + ""
    }
    return null
  }

  /**
   *
   */
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case actCommand: AccountCommand => (extractShardIdFromCommand(actCommand), actCommand)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case acctCmd: AccountCommand => extractShardIdFromCommand(acctCmd)
  }


  /**
   *
   * @param system
   * @param messagePublisherActor
   * @return
   */
  def setupClusterSharding(system: ActorSystem, messagePublisherActor: ActorRef): ActorRef = {
    val settings = ClusterShardingSettings.create(system)
    ClusterSharding(system).start(
      typeName = "AccountSharding",
      entityProps = AccountEntityActor.props(messagePublisherActor),
      settings = settings,
      extractEntityId = extractEntityId,
      extractShardId = extractShardId)
  }
}
