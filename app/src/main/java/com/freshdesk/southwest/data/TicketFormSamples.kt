package com.freshdesk.southwest.data

import com.google.gson.GsonBuilder

object TicketFormSamples {
    const val SAMPLE_FORM_ID = "612371"
    const val SAMPLE_ARTICLE_ID = "4374493"
    private const val SAMPLE_GROUP_ID = 123
    private const val SAMPLE_REFERENCE_NUMBER = 223
    private const val SAMPLE_DECIMAL = 0.2
    private const val SAMPLE_HIDE_PRIORITY_LOW = "2"
    private const val SAMPLE_HIDE_PRIORITY_HIGH = "3"
    private val SAMPLE_HIDE_PRIORITY_IDS = listOf(SAMPLE_HIDE_PRIORITY_LOW, SAMPLE_HIDE_PRIORITY_HIGH)

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun defaultFieldsListJson(): String = gson.toJson(sampleFieldNames())

    fun defaultPrefillFieldValuesJson(): String = gson.toJson(samplePrefillFieldValues())

    fun defaultHideChoicesJson(): String = gson.toJson(sampleHideChoices())

    private fun sampleFieldNames(): List<String> = listOf(
        "subject",
        "description",
        "ticket_type",
        "custom_fields.cf_reference_number"
    )

    private fun samplePrefillFieldValues(): Map<String, Any> = mapOf(
        "subject" to "Damaged order",
        "description" to "I received a damaged product",
        "group" to SAMPLE_GROUP_ID,
        "ticket_type" to "Incident",
        "customFields" to mapOf(
            "cf_reference_number" to SAMPLE_REFERENCE_NUMBER,
            "cf_decimal" to SAMPLE_DECIMAL,
            "cf_date" to "2024-02-15"
        ),
    )

    private fun sampleHideChoices(): Map<String, Any> = mapOf(
        "priority" to SAMPLE_HIDE_PRIORITY_IDS,
        "custom_fields" to mapOf(
            "cf_dp" to listOf("Second Choice"),
        ),
    )
}
