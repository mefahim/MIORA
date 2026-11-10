package com.fahim.powerampclone.update

/**
 * Lightweight, dependency-free JSON AST for parsing GitHub API payloads in both
 * Android runtime and standard JVM unit tests without framework mocks.
 */
sealed class JsonElement {
    data object Null : JsonElement()
    data class Bool(val value: Boolean) : JsonElement()
    data class Num(val value: Double) : JsonElement()
    data class Str(val value: String) : JsonElement()
    data class Arr(val list: List<JsonElement>) : JsonElement()
    data class Obj(val map: Map<String, JsonElement>) : JsonElement()

    fun asString(): String? = (this as? Str)?.value
    fun asBoolean(): Boolean? = (this as? Bool)?.value
    fun asLong(): Long? = (this as? Num)?.value?.toLong()
    fun asInt(): Int? = (this as? Num)?.value?.toInt()
    fun asObject(): Obj? = this as? Obj
    fun asArray(): Arr? = this as? Arr

    operator fun get(key: String): JsonElement? = (this as? Obj)?.map?.get(key)
    operator fun get(index: Int): JsonElement? = (this as? Arr)?.list?.getOrNull(index)
}

object SimpleJsonParser {
    fun parse(json: String): JsonElement {
        val tokens = tokenize(json)
        val (element, _) = parseValue(tokens, 0)
        return element
    }

    private fun tokenize(input: String): List<String> {
        val tokens = ArrayList<String>()
        var i = 0
        val len = input.length

        while (i < len) {
            val c = input[i]
            when {
                c.isWhitespace() -> i++
                c in "{}[]:," -> {
                    tokens.add(c.toString())
                    i++
                }
                c == '"' -> {
                    val sb = StringBuilder()
                    sb.append('"')
                    i++
                    while (i < len) {
                        val ch = input[i]
                        if (ch == '\\' && i + 1 < len) {
                            sb.append(ch)
                            sb.append(input[i + 1])
                            i += 2
                        } else if (ch == '"') {
                            sb.append('"')
                            i++
                            break
                        } else {
                            sb.append(ch)
                            i++
                        }
                    }
                    tokens.add(sb.toString())
                }
                else -> {
                    val start = i
                    while (i < len && !input[i].isWhitespace() && input[i] !in "{}[]:,") {
                        i++
                    }
                    tokens.add(input.substring(start, i))
                }
            }
        }
        return tokens
    }

    private fun parseValue(tokens: List<String>, index: Int): Pair<JsonElement, Int> {
        if (index >= tokens.size) return JsonElement.Null to index
        val token = tokens[index]

        return when {
            token == "{" -> parseObject(tokens, index + 1)
            token == "[" -> parseArray(tokens, index + 1)
            token == "true" -> JsonElement.Bool(true) to index + 1
            token == "false" -> JsonElement.Bool(false) to index + 1
            token == "null" -> JsonElement.Null to index + 1
            token.startsWith('"') && token.endsWith('"') -> {
                JsonElement.Str(unescape(token.substring(1, token.length - 1))) to index + 1
            }
            else -> {
                val num = token.toDoubleOrNull()
                if (num != null) {
                    JsonElement.Num(num) to index + 1
                } else {
                    JsonElement.Str(token) to index + 1
                }
            }
        }
    }

    private fun parseObject(tokens: List<String>, startIndex: Int): Pair<JsonElement.Obj, Int> {
        val map = LinkedHashMap<String, JsonElement>()
        var i = startIndex
        if (i < tokens.size && tokens[i] == "}") {
            return JsonElement.Obj(map) to i + 1
        }

        while (i < tokens.size) {
            val keyToken = tokens[i]
            if (keyToken == "}") return JsonElement.Obj(map) to i + 1
            val key = if (keyToken.startsWith('"') && keyToken.endsWith('"')) {
                unescape(keyToken.substring(1, keyToken.length - 1))
            } else {
                keyToken
            }
            i++ // past key
            if (i < tokens.size && tokens[i] == ":") {
                i++ // past colon
            }
            val (value, nextIdx) = parseValue(tokens, i)
            map[key] = value
            i = nextIdx
            if (i < tokens.size && tokens[i] == ",") {
                i++
            } else if (i < tokens.size && tokens[i] == "}") {
                return JsonElement.Obj(map) to i + 1
            }
        }
        return JsonElement.Obj(map) to i
    }

    private fun parseArray(tokens: List<String>, startIndex: Int): Pair<JsonElement.Arr, Int> {
        val list = ArrayList<JsonElement>()
        var i = startIndex
        if (i < tokens.size && tokens[i] == "]") {
            return JsonElement.Arr(list) to i + 1
        }

        while (i < tokens.size) {
            if (tokens[i] == "]") return JsonElement.Arr(list) to i + 1
            val (value, nextIdx) = parseValue(tokens, i)
            list.add(value)
            i = nextIdx
            if (i < tokens.size && tokens[i] == ",") {
                i++
            } else if (i < tokens.size && tokens[i] == "]") {
                return JsonElement.Arr(list) to i + 1
            }
        }
        return JsonElement.Arr(list) to i
    }

    private fun unescape(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val next = s[i + 1]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000c')
                    'u' -> {
                        if (i + 5 < s.length) {
                            val hex = s.substring(i + 2, i + 6)
                            try {
                                sb.append(hex.toInt(16).toChar())
                                i += 4
                            } catch (_: NumberFormatException) {
                                sb.append("\\u")
                            }
                        } else {
                            sb.append("\\u")
                        }
                    }
                    else -> sb.append(next)
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
