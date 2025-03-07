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
package com.netflix.atlas.lwcapi

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.netflix.atlas.akka.CustomDirectives._
import com.netflix.atlas.akka.DiagnosticMessage
import com.netflix.atlas.akka.WebApi
import com.netflix.atlas.json.Json

import javax.inject.Inject

/**
  * Provides a summary of the current streams. This is to aide in debugging and can be
  * disabled without impacting the service.
  */
class StreamsApi @Inject() (sm: StreamSubscriptionManager) extends WebApi {

  def routes: Route = {
    endpointPathPrefix("api" / "v1" / "streams") {
      path(Remaining) { streamId =>
        sm.streamSummary(streamId) match {
          case Some(summary) => complete(Json.encode(summary))
          case None          => complete(notFound(streamId))
        }
      } ~
      pathEnd {
        complete(Json.encode(sm.streamSummaries.map(_.metadata)))
      }
    }
  }

  private def notFound(streamId: String): HttpResponse = {
    val msg = DiagnosticMessage.info(s"no stream with id: $streamId")
    HttpResponse(StatusCodes.NotFound, entity = Json.encode(msg))
  }
}
