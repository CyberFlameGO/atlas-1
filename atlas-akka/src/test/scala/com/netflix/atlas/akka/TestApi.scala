/*
 * Copyright 2014-2021 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.atlas.akka

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.CircuitBreaker
import akka.stream.scaladsl.Source

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

class TestApi(val system: ActorSystem) extends WebApi {

  import CustomDirectives._
  import scala.concurrent.duration._

  private implicit val ec: ExecutionContext = OpportunisticEC.ec

  private val breaker = new CircuitBreaker(
    system.scheduler,
    maxFailures = 1,
    callTimeout = 5.seconds,
    resetTimeout = 1.second
  )

  private def fail(): Future[Unit] = {
    Future.failed(new RuntimeException("circuit breaker test"))
  }

  def routes: Route = {
    path("jsonparse") {
      post {
        parseEntity(json[String]) { v =>
          complete(HttpResponse(status = StatusCodes.OK, entity = v))
        }
      }
    } ~
    path("query-parsing-directive") {
      get {
        parameter("regex") { v =>
          val entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, v)
          complete(HttpResponse(status = StatusCodes.OK, entity = entity))
        }
      }
    } ~
    path("query-parsing-explicit") {
      get {
        extractRequest { req =>
          val v = req.uri.query().get("regex").get
          val entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, v)
          complete(HttpResponse(status = StatusCodes.OK, entity = entity))
        }
      }
    } ~
    accessLog(Nil) {
      path("chunked") {
        get {
          val source = Source
            .single(ChunkStreamPart("start"))
            .concat(Source((1 until 42).map(i => ChunkStreamPart(i.toString)).toList))
          val entity = HttpEntity.Chunked(ContentTypes.`text/plain(UTF-8)`, source)
          complete(HttpResponse(status = StatusCodes.OK, entity = entity))
        }
      }
    } ~
    path("circuit-breaker") {
      get {
        onCompleteWithBreaker(breaker)(fail()) {
          case Success(_) => complete(StatusCodes.OK)
          case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
        }
      }
    } ~
    path("unauthorized") {
      authorize(false) {
        complete(StatusCodes.OK)
      }
    }
  }
}
