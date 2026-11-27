package com.freshdesk.southwest.data

import androidx.annotation.StringRes
import com.freshdesk.southwest.R

data class TextFieldDialog(
    val field1: String,
    val field2: String,
    val toggle: Boolean = false
)

data class DialogConfig(
    @StringRes val title: Int = R.string.open_topic,
    @StringRes val positiveText: Int = R.string.open,
    val showDescription: Boolean = false,
    /** When set, a switch with this label is shown below the fields; its state is returned as `toggle`. */
    @StringRes val toggleLabel: Int? = null,
    val toggleInitial: Boolean = false
)