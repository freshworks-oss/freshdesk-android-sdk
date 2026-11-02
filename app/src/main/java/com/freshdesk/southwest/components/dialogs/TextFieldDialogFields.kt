package com.freshdesk.southwest.components.dialogs

import com.freshdesk.southwest.components.FieldConfig

data class TextFieldDialogFields(
    val textField1: Pair<Int, String>,
    val textField2: Pair<Int, String>? = null,
    val textField1Config: FieldConfig = FieldConfig(),
    val textField2Config: FieldConfig = FieldConfig(),
)
