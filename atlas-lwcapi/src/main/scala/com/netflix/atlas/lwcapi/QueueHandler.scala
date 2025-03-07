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

import com.netflix.atlas.akka.StreamOps
import com.netflix.atlas.json.JsonSupport
import com.typesafe.scalalogging.StrictLogging

/**
  * Message handler for use with the [SubscriptionManager].
  *
  * @param streamMeta
  *     Stream metadata for this handler. Used to provide context in the log messages and easily
  *     be able to grep for a given id.
  * @param queue
  *     Underlying queue that will receive the messsages.
  */
class QueueHandler(streamMeta: StreamMetadata, queue: StreamOps.SourceQueue[Seq[JsonSupport]])
    extends StrictLogging {

  private val id = streamMeta.streamId

  private def toJson(msgs: Seq[JsonSupport]): String = {
    msgs.map(_.toJson).mkString("[", ",", "]")
  }

  def offer(msgs: Seq[JsonSupport]): Unit = {
    logger.trace(s"enqueuing message for $id: ${toJson(msgs)}")
    if (!queue.offer(msgs)) {
      logger.debug(s"failed to enqueue message for $id: ${toJson(msgs)}")
      streamMeta.droppedMessages.addAndGet(msgs.size)
    } else {
      streamMeta.receivedMessages.addAndGet(msgs.size)
    }
  }

  def complete(): Unit = {
    logger.debug(s"queue complete for $id")
    queue.complete()
  }

  override def toString: String = s"QueueHandler($id)"
}
