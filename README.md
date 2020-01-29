# Domain-Driven Design With Event Sourcing, Akka Cluster Sharding, Cassandra, Kafka, Scala. Distributed & Reactive Trading System. FinTech Use Case.

# Use Case
The application is about a simple financial trading system. It has 3 main microservices and an API Gateway. Using Domain-Driven Design, Event Sourcing, Akka Cluster Sharding, Cassandra, Kafka, Event-Driven, Reactive microservice pattern.  Using the asynchronous, non-blocking style of systems development and Akka toolkits. Distributed and Reactive solution that can serve millions of users concurrently.

![alt text](https://github.com/seyijava/Akka-StockTrading-EventSourcing/blob/master/images/TradingSystem.jpg)


Akka makes building powerful concurrent & distributed applications simple. Akka strictly adheres to the Reactive Manifesto. Reactive applications aim at replacing traditional multithreaded applications with an architecture that satisfies one or more of the following requirements:

![alt text](https://github.com/seyijava/Akka-StockTrading-EventSourcing/blob/master/images/Reactive.png)


# StockTrading Service has 3 Bounded contexts

![alt text](https://github.com/seyijava/Akka-StockTrading-EventSourcing/blob/master/images/contextmapping.jpeg)

Portfolio Service
 Microservice is responsible for buying and selling and reconstructing our portfolio holdings to determine investor profit and                loss.

 ![alt text](https://github.com/seyijava/Akka-StockTrading-EventSourcing/blob/master/images/SequenceSellOrder.jpg)
 
Trade Order Service
   Microservice is responsible for the stock quote and executes trade orders via the exchange e.g NASDAQ
  
  ![alt text](https://github.com/seyijava/Akka-StockTrading-EventSourcing/blob/master/images/SequenceAccount.jpg)
  
Account Service
   Microservice is responsible for managing customer accounts like account balance, customer profile.
   
   ![alt text](https://github.com/seyijava/Akka-StockTrading-EventSourcing/blob/master/images/SequenceAccount.jpg)






