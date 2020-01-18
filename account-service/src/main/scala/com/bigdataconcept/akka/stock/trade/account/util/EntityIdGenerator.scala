package com.bigdataconcept.akka.stock.trade.account.util

import java.util.UUID

object EntityIdGenerator {

  def generatorEntityId(): String={
    return UUID.randomUUID().toString()
  }

}
