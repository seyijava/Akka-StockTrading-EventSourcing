package com.bigdataconcept.akka.stock.trade.portfolio.util
import java.util.UUID
import  com.hazelcast.core.Hazelcast
object EntityIdGenerator {

       def generatorEntityId(): String={
        return UUID.randomUUID().toString()
       }

       def generateOrderId(): String={
         return UUID.randomUUID().toString()
       }

      def generateTransferId(): String={
        return UUID.randomUUID().toString()
      }
}
