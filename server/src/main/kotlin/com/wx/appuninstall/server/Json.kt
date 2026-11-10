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

/**
 * 最小 JSON 解析与序列化，避免为演示服务端引入第三方依赖。
 * 解析结果：对象为 Map、数组为 List、数字为 Long 或 Double、字符串、Boolean、null。
 *
 * @author venshine
 */
object Json {

    class JsonException(message: String) : RuntimeException(message)

    fun parse(text: String): Any? {
        val reader = Reader(text)
        reader.skipSpace()
        val value = reader.value()
        reader.skipSpace()
        if (!reader.end()) throw JsonException("多余内容，位置 ${reader.pos}")
        return value
    }

    @Suppress("UNCHECKED_CAST")
    fun parseObject(text: String): Map<String, Any?> =
        parse(text) as? Map<String, Any?> ?: throw JsonException("根节点不是对象")

    fun stringify(value: Any?): String = StringBuilder().also { write(it, value) }.toString()

    private fun write(out: StringBuilder, value: Any?) {
        when (value) {
            null -> out.append("null")
            is String -> quote(out, value)
            is Boolean, is Int, is Long -> out.append(value.toString())
            is Double -> out.append(if (value % 1.0 == 0.0 && Math.abs(value) < 1e15) value.toLong().toString() else value.toString())
            is Map<*, *> -> {
                out.append('{')
                var first = true
                for ((k, v) in value) {
                    if (!first) out.append(',')
                    first = false
                    quote(out, k.toString())
                    out.append(':')
                    write(out, v)
                }
                out.append('}')
            }
            is Iterable<*> -> {
                out.append('[')
                var first = true
                for (v in value) {
                    if (!first) out.append(',')
                    first = false
                    write(out, v)
                }
                out.append(']')
            }
            else -> throw JsonException("不支持的类型 ${value.javaClass.name}")
        }
    }

    private fun quote(out: StringBuilder, s: String) {
        out.append('"')
        for (c in s) {
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c == '\r' -> out.append("\\r")
                c == '\t' -> out.append("\\t")
                c < ' ' -> out.append(String.format("\\u%04x", c.code))
                else -> out.append(c)
            }
        }
        out.append('"')
    }

    private class Reader(val s: String) {
        var pos = 0

        fun end() = pos >= s.length

        fun skipSpace() {
            while (pos < s.length && s[pos].isWhitespace()) pos++
        }

        fun value(): Any? {
            if (end()) throw JsonException("意外结束")
            return when (val c = s[pos]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> if (c == '-' || c.isDigit()) num() else throw JsonException("意外字符 '$c'，位置 $pos")
            }
        }

        private fun literal(word: String, result: Any?): Any? {
            if (!s.startsWith(word, pos)) throw JsonException("非法字面量，位置 $pos")
            pos += word.length
            return result
        }

        private fun obj(): Map<String, Any?> {
            val map = LinkedHashMap<String, Any?>()
            pos++
            skipSpace()
            if (peek() == '}') { pos++; return map }
            while (true) {
                skipSpace()
                if (peek() != '"') throw JsonException("对象键必须是字符串，位置 $pos")
                val key = str()
                skipSpace()
                expect(':')
                skipSpace()
                map[key] = value()
                skipSpace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> { pos++; return map }
                    else -> throw JsonException("对象内应为 , 或 }，位置 $pos")
                }
            }
        }

        private fun arr(): List<Any?> {
            val list = ArrayList<Any?>()
            pos++
            skipSpace()
            if (peek() == ']') { pos++; return list }
            while (true) {
                skipSpace()
                list.add(value())
                skipSpace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> { pos++; return list }
                    else -> throw JsonException("数组内应为 , 或 ]，位置 $pos")
                }
            }
        }

        private fun str(): String {
            pos++
            val sb = StringBuilder()
            while (true) {
                if (end()) throw JsonException("字符串未结束")
                val c = s[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (end()) throw JsonException("转义未结束")
                        when (val e = s[pos++]) {
                            '"', '\\', '/' -> sb.append(e)
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000c')
                            'u' -> {
                                if (pos + 4 > s.length) throw JsonException("\\u 转义不完整")
                                sb.append(s.substring(pos, pos + 4).toInt(16).toChar())
                                pos += 4
                            }
                            else -> throw JsonException("非法转义 \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun num(): Any {
            val start = pos
            if (peek() == '-') pos++
            while (!end() && (s[pos].isDigit() || s[pos] in ".eE+-")) pos++
            val token = s.substring(start, pos)
            return token.toLongOrNull() ?: token.toDoubleOrNull() ?: throw JsonException("非法数字 $token")
        }

        private fun peek(): Char = if (end()) '\u0000' else s[pos]

        private fun expect(c: Char) {
            if (peek() != c) throw JsonException("应为 '$c'，位置 $pos")
            pos++
        }
    }
}
