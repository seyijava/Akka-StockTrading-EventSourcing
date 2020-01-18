package com.bigdataconcept.akka.stock.trade.portfolio.api

import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.actor.ActorSystem
import akka.actor.CoordinatedShutdown
import scala.concurrent.duration._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.Await
import akka.http.scaladsl.server.Directives._

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Route._
import akka.http.scaladsl.server.RouteResult._


class HttpServerRoutes(route: Route, port: Int,host: String)(implicit system: ActorSystem)  {

  implicit val dispatcher = system.dispatcher

  private val shutdown = CoordinatedShutdown(system)

  def start(): Unit = {
    System.out.println(port)
    val bindingFuture: Future[ServerBinding] = Http().bindAndHandle(route, host, port)



  }

}