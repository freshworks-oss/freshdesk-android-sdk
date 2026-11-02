package com.freshdesk.southwest.utils

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.math.BigDecimal

object TicketFormJsonParser {
    private val gson = Gson()

    fun parseFieldNames(json: String): List<String>? = try {
        gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
    } catch (error: JsonSyntaxException) {
        logd {
            "TicketFormJsonParser parseFieldNames :$error"
        }
        null
    }

    fun parseFieldValues(json: String): Map<String, Any>? = try {
        gson.fromJson(json, JsonObject::class.java)?.let { toMap(it) }
    } catch (error: JsonSyntaxException) {
        logd { "TicketFormJsonParser parseFieldValues :$error" }
        null
    } catch (error: IllegalArgumentException) {
        logd { "TicketFormJsonParser parseFieldValues :$error" }
        null
    }

    fun parseChoices(json: String): Map<String, Any>? = parseFieldValues(json)

    /**
     * Gson deserializes every JSON number as [Double] when the target type is [Any], so ids like
     * `123` reach the widget as `123.0` and the payload is rejected. Walking the tree instead keeps
     * whole numbers whole (including values written as `123.0`). Unsupported JSON nulls fail the
     * parse instead of being dropped silently.
     */
    private fun toMap(jsonObject: JsonObject): Map<String, Any> {
        val result = LinkedHashMap<String, Any>(jsonObject.size())
        for ((key, element) in jsonObject.entrySet()) {
            result[key] = toValue(element)
                ?: throw IllegalArgumentException("Unsupported JSON null for key=$key")
        }
        return result
    }

    private fun toValue(element: JsonElement): Any? = when {
        element.isJsonNull -> null
        element.isJsonObject -> toMap(element.asJsonObject)
        element.isJsonArray -> {
            element.asJsonArray.map { item ->
                toValue(item) ?: throw IllegalArgumentException("Unsupported JSON null in array")
            }
        }
        element.isJsonPrimitive -> element.asJsonPrimitive.toValue()
        else -> null
    }

    private fun JsonPrimitive.toValue(): Any = when {
        isBoolean -> asBoolean
        isNumber -> asString.toIntegralOrDouble()
        else -> asString
    }

    private fun String.toIntegralOrDouble(): Any {
        val decimal = BigDecimal(this)
        val stripped = decimal.stripTrailingZeros()
        return if (stripped.scale() <= 0) {
            try {
                stripped.longValueExact()
            } catch (_: ArithmeticException) {
                decimal.toDouble()
            }
        } else {
            decimal.toDouble()
        }
    }
}
