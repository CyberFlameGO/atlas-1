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
package com.netflix.atlas.webapi

import com.netflix.atlas.core.model.Datapoint
import com.netflix.atlas.json.Json
import munit.FunSuite

class PublishApiJsonSuite extends FunSuite {

  test("encode and decode datapoint") {
    val original = Datapoint(Map("name" -> "foo", "id" -> "bar"), 42L, 1024.0)
    val decoded = PublishApi.decodeDatapoint(PublishApi.encodeDatapoint(original))
    assertEquals(original, decoded)
  }

  test("encode and decode batch") {
    val commonTags = Map("id" -> "bar")
    val original = List(Datapoint(Map("name" -> "foo"), 42L, 1024.0))
    val decoded = PublishApi.decodeBatch(PublishApi.encodeBatch(commonTags, original))
    assertEquals(original.map(d => d.copy(tags = d.tags ++ commonTags)), decoded)
  }

  test("decode batch empty") {
    val decoded = PublishApi.decodeBatch("{}")
    assertEquals(decoded.size, 0)
  }

  test("decode with legacy array value") {
    val expected = Datapoint(Map("name" -> "foo"), 42L, 1024.0)
    val decoded =
      PublishApi.decodeDatapoint("""{"tags":{"name":"foo"},"timestamp":42,"values":[1024.0]}""")
    assertEquals(expected, decoded)
  }

  test("decode legacy batch empty") {
    val decoded = PublishApi.decodeBatch("""
      {
        "tags": {},
        "metrics": []
      }
      """)
    assertEquals(decoded.size, 0)
  }

  test("decode legacy batch no tags") {
    val decoded = PublishApi.decodeBatch("""
      {
        "metrics": []
      }
      """)
    assertEquals(decoded.size, 0)
  }

  test("decode legacy batch with tags before") {
    val decoded = PublishApi.decodeBatch("""
      {
        "tags": {
          "foo": "bar"
        },
        "metrics": [
          {
            "tags": {"name": "test"},
            "start": 123456789,
            "values": [1.0]
          }
        ]
      }
      """)
    assertEquals(decoded.size, 1)
    assertEquals(decoded.head.tags, Map("name" -> "test", "foo" -> "bar"))
  }

  test("decode legacy batch with tags after") {
    val decoded = PublishApi.decodeBatch("""
      {
        "metrics": [
          {
            "tags": {"name": "test"},
            "start": 123456789,
            "values": [1.0]
          }
        ],
        "tags": {
          "foo": "bar"
        }
      }
      """)
    assertEquals(decoded.size, 1)
    assertEquals(decoded.head.tags, Map("name" -> "test", "foo" -> "bar"))
  }

  test("decode legacy batch no tags metric") {
    val decoded = PublishApi.decodeBatch("""
      {
        "metrics": [
          {
            "tags": {"name": "test"},
            "start": 123456789,
            "values": [1.0]
          }
        ]
      }
      """)
    assertEquals(decoded.size, 1)
  }

  test("decode legacy batch with empty name") {
    val decoded = PublishApi.decodeBatch("""
      {
        "metrics": [
          {
            "tags": {"name": ""},
            "start": 123456789,
            "values": [1.0]
          }
        ]
      }
      """)
    assertEquals(decoded.size, 1)
    decoded.foreach { d =>
      assertEquals(d.tags, Map("name" -> ""))
    }
  }

  test("decode legacy batch with null name") {
    val decoded = PublishApi.decodeBatch("""
      {
        "metrics": [
          {
            "tags": {"name": null},
            "start": 123456789,
            "values": [1.0]
          }
        ]
      }
      """)
    assertEquals(decoded.size, 1)
    decoded.foreach { d =>
      assertEquals(d.tags, Map.empty[String, String])
    }
  }

  test("decode list empty") {
    val decoded = PublishApi.decodeList("""
      []
      """)
    assertEquals(decoded.size, 0)
  }

  test("decode list") {
    val decoded = PublishApi.decodeList("""
      [
        {
          "tags": {"name": "test"},
          "timestamp": 123456789,
          "values": 1.0
        }
      ]
      """)
    assertEquals(decoded.size, 1)
  }

  test("decode list with unknown key") {
    val decoded = PublishApi.decodeList("""
      [
        {
          "tags": {"name": "test"},
          "timestamp": 123456789,
          "unknown": {},
          "values": 1.0
        },
        {
          "tags": {"name": "test"},
          "timestamp": 123456789,
          "unknown": {"a":{"b":"c"},"b":[1,2,3]},
          "values": 1.0
        },
        {
          "tags": {"name": "test"},
          "timestamp": 123456789,
          "unknown": [1,2,3],
          "values": 1.0
        },
        {
          "tags": {"name": "test"},
          "timestamp": 123456789,
          "unknown": "foo",
          "values": 1.0
        }
      ]
      """)
    assertEquals(decoded.size, 4)
  }

  test("decode batch bad object") {
    intercept[IllegalArgumentException] {
      PublishApi.decodeBatch("""{"foo":"bar"}""")
    }
  }

  test("decode list from encoded datapoint") {
    val vs = List(Datapoint(Map("a" -> "b"), 0L, 42.0))
    val decoded = PublishApi.decodeList(Json.encode(vs))
    assertEquals(decoded.size, 1)
  }

  test("decode list from PublishApi.encoded datapoint") {
    val vs = "[" + PublishApi.encodeDatapoint(Datapoint(Map("a" -> "b"), 0L, 42.0)) + "]"
    val decoded = PublishApi.decodeList(vs)
    assertEquals(decoded.size, 1)
  }
}
