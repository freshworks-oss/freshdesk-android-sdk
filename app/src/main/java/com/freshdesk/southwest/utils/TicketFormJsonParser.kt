package com.freshdesk.southwest.utils

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

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
        gson.fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
    } catch (error: JsonSyntaxException) {
        logd { "TicketFormJsonParser parseFieldValues :$error" }
        null
    }

    fun parseChoices(json: String): Map<String, Any>? = parseFieldValues(json)
}
