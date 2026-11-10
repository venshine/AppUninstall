/*
 * Copyright (C) 2016 venshine.cn@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wx.appuninstall.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonTest {

    @Test
    fun parsesNestedStructures() {
        val m = Json.parseObject("""{"a":1,"b":[true,null,"x\nA"],"c":{"d":-2.5}}""")
        assertEquals(1L, m["a"])
        assertEquals(listOf(true, null, "x\nA"), m["b"])
        assertEquals(-2.5, (m["c"] as Map<*, *>)["d"])
    }

    @Test
    fun roundTripsEscapes() {
        val text = Json.stringify(mapOf("k" to "a\"b\\c\n", "n" to 3L, "l" to listOf(1L, "x")))
        assertEquals("""{"k":"a\"b\\c\n","n":3,"l":[1,"x"]}""", text)
        assertEquals("a\"b\\c\n", Json.parseObject(text)["k"])
    }

    @Test
    fun rejectsMalformedInput() {
        for (bad in listOf("", "{", """{"a":}""", """{"a":1} x""", "[1,]", """{"a" 1}""", "nul")) {
            val failed = runCatching { Json.parse(bad) }.exceptionOrNull()
            assertTrue("应当拒绝: $bad", failed is Json.JsonException)
        }
    }

    @Test
    fun rootMustBeObjectForParseObject() {
        assertTrue(runCatching { Json.parseObject("[1]") }.exceptionOrNull() is Json.JsonException)
    }
}
