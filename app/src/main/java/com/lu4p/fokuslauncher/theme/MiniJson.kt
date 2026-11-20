package com.lu4p.fokuslauncher.theme

/**
 * Tiny strict JSON reader/writer. Pure Kotlin (no org.json, so it behaves identically in unit
 * tests) and data-only: the result is nested Map/List/String/Long/Double/Boolean/null and nothing
 * is ever evaluated. Rejects trailing content, duplicate keys, excessive depth and size.
 */
internal object MiniJson {
    const val MAX_INPUT_CHARS = 64 * 1024
    private const val MAX_DEPTH = 12
    private const val MAX_ARRAY = 1000

    class JsonException(message: String) : Exception(message)

    fun parse(text: String): Any? {
        if (text.length > MAX_INPUT_CHARS) throw JsonException("input too large")
        val p = Parser(text)
        p.skipWs()
        val v = p.value(0)
        p.skipWs()
        if (!p.atEnd()) throw JsonException("trailing content")
        return v
    }

    private class Parser(val s: String) {
        var i = 0

        fun atEnd() = i >= s.length

        fun skipWs() {
            while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r' || s[i] == '\t')) i++
        }

        fun value(depth: Int): Any? {
            if (depth > MAX_DEPTH) throw JsonException("nesting too deep")
            if (atEnd()) throw JsonException("unexpected end")
            val c = s[i]
            return when {
                c == '{' -> obj(depth)
                c == '[' -> arr(depth)
                c == '"' -> str()
                c == 't' -> lit("true", true)
                c == 'f' -> lit("false", false)
                c == 'n' -> lit("null", null)
                c == '-' || c in '0'..'9' -> num()
                else -> throw JsonException("unexpected character")
            }
        }

        fun lit(word: String, v: Any?): Any? {
            if (!s.startsWith(word, i)) throw JsonException("bad literal")
            i += word.length
            return v
        }

        fun num(): Any {
            val start = i
            if (s[i] == '-') i++
            while (i < s.length && (s[i] in '0'..'9' || s[i] in ".eE+-")) i++
            val t = s.substring(start, i)
            t.toLongOrNull()?.let { return it }
            val d = t.toDoubleOrNull() ?: throw JsonException("bad number")
            if (d.isNaN() || d.isInfinite()) throw JsonException("bad number")
            return d
        }

        fun str(): String {
            i++ // opening quote
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) throw JsonException("unterminated string")
                val c = s[i++]
                if (c == '"') return sb.toString()
                if (c == '\\') {
                    if (atEnd()) throw JsonException("bad escape")
                    when (s[i++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (i + 4 > s.length) throw JsonException("bad escape")
                            val code =
                                    s.substring(i, i + 4).toIntOrNull(16)
                                            ?: throw JsonException("bad escape")
                            sb.append(code.toChar())
                            i += 4
                        }
                        else -> throw JsonException("bad escape")
                    }
                } else if (c < ' ') {
                    throw JsonException("control character in string")
                } else {
                    sb.append(c)
                }
            }
        }

        fun arr(depth: Int): List<Any?> {
            i++
            val out = ArrayList<Any?>()
            skipWs()
            if (!atEnd() && s[i] == ']') {
                i++
                return out
            }
            while (true) {
                skipWs()
                out.add(value(depth + 1))
                if (out.size > MAX_ARRAY) throw JsonException("array too large")
                skipWs()
                if (atEnd()) throw JsonException("unterminated array")
                when (s[i++]) {
                    ',' -> Unit
                    ']' -> return out
                    else -> throw JsonException("expected , or ]")
                }
            }
        }

        fun obj(depth: Int): Map<String, Any?> {
            i++
            val out = LinkedHashMap<String, Any?>()
            skipWs()
            if (!atEnd() && s[i] == '}') {
                i++
                return out
            }
            while (true) {
                skipWs()
                if (atEnd() || s[i] != '"') throw JsonException("expected key")
                val k = str()
                if (out.containsKey(k)) throw JsonException("duplicate key")
                skipWs()
                if (atEnd() || s[i++] != ':') throw JsonException("expected :")
                skipWs()
                out[k] = value(depth + 1)
                skipWs()
                if (atEnd()) throw JsonException("unterminated object")
                when (s[i++]) {
                    ',' -> Unit
                    '}' -> return out
                    else -> throw JsonException("expected , or }")
                }
            }
        }
    }

    fun quote(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c < ' ' -> sb.append("\\u%04x".format(c.code))
                else -> sb.append(c)
            }
        }
        return sb.append('"').toString()
    }
}
