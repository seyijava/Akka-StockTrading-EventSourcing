package com.bigdataconcept.akka.stock.trade.account.api

import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteResult._

import scala.concurrent.Future


class HttpServerRoutes(route: Route, port: Int, host: String)(implicit system: ActorSystem)  {

  implicit val dispatcher = system.dispatcher

  private val shutdown = CoordinatedShutdown(system)

  def start(): Unit = {
    System.out.println(port)
    val bindingFuture: Future[ServerBinding] = Http().bindAndHandle(route, host, port)
  }

}