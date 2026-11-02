package com.freshdesk.southwest.components.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshdesk.southwest.R
import com.freshdesk.southwest.components.FieldConfig
import com.freshdesk.southwest.components.FormField
import com.freshdesk.southwest.components.buttons.ClearButton
import com.freshdesk.southwest.components.buttons.ConfirmButton
import com.freshdesk.southwest.components.buttons.DismissButton
import com.freshdesk.southwest.data.DialogConfig
import com.freshdesk.southwest.data.TextFieldDialog
import com.freshdesk.southwest.ui.theme.SouthWestTheme

@Composable
fun TextFieldDialog(
    config: DialogConfig,
    fields: TextFieldDialogFields,
    onConfirmed: (TextFieldDialog) -> Unit,
    onDismissed: () -> Unit,
) {
    val field1 = rememberSaveable { mutableStateOf(fields.textField1.second) }
    val field2 = rememberSaveable { mutableStateOf(fields.textField2?.second.orEmpty()) }
    AlertDialog(
        onDismissRequest = {
        },
        title = {
            Text(
                text = stringResource(id = config.title),
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            TextFieldDialogForm(config, fields, field1, field2)
        },
        confirmButton = {
            ConfirmButton(config.positiveText) {
                onConfirmed(
                    TextFieldDialog(
                        field1.value,
                        field2.value
                    )
                )
            }
        },
        dismissButton = {
            DismissButton { onDismissed.invoke() }
        }
    )
}

@Composable
private fun TextFieldDialogForm(
    config: DialogConfig,
    fields: TextFieldDialogFields,
    field1: MutableState<String>,
    field2: MutableState<String>,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        FormField(
            labelId = fields.textField1.first,
            value = field1.value,
            config = fields.textField1Config.copy(
                isBlank = field1.value.isBlank(),
                trailingIcon = { ClearButton { field1.value = "" } }
            ),
            onValueChange = { fields.textField1Config.filterInput(it)?.let { next -> field1.value = next } }
        )
        if (config.showDescription) {
            ShowDescription()
        }
        fields.textField2?.let { secondField ->
            FormField(
                labelId = secondField.first,
                value = field2.value,
                config = fields.textField2Config.copy(
                    isBlank = field2.value.isBlank(),
                    trailingIcon = { ClearButton { field2.value = "" } }
                ),
                onValueChange = { fields.textField2Config.filterInput(it)?.let { next -> field2.value = next } }
            )
        }
    }
}

private fun FieldConfig.filterInput(value: String): String? {
    return if (keyboardType == KeyboardType.Number) {
        // Reject mixed input (e.g. "12-34") instead of silently becoming "1234".
        value.takeIf { candidate -> candidate.all { it.isDigit() } }
    } else {
        value
    }
}

@Composable
fun ShowDescription(@StringRes description: Int = R.string.event_data_description) {
    Text(
        text = stringResource(id = description),
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
@Preview
fun ShowTextFieldDialog() {
    SouthWestTheme {
        TextFieldDialog(
            config = DialogConfig(R.string.open_topic, R.string.open),
            fields = TextFieldDialogFields(
                textField1 = Pair(R.string.topic_name, ""),
                textField2 = Pair(R.string.topic_id, ""),
            ),
            onConfirmed = {}
        ) {}
    }
}
