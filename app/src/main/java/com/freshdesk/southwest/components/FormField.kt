package com.freshdesk.southwest.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.freshdesk.southwest.R
import com.freshdesk.southwest.ui.theme.FormFieldTheme
import com.freshdesk.southwest.ui.theme.SouthWestTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormField(
    @StringRes labelId: Int,
    value: String,
    config: FieldConfig = FieldConfig(),
    onValueChange: (String) -> Unit
) {
    val error = rememberSaveable { mutableStateOf(false) }
    FormFieldTheme {
        OutlinedTextField(
            value = value,
            readOnly = config.isReadOnly,
            onValueChange = {
                onValueChange.invoke(it)
                error.value = config.isRequired && it.isBlank()
            },
            label = { Text(text = stringResource(id = labelId)) },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = config.keyboardType
            ),
            supportingText = {
                if (error.value || (config.isBlank && config.isRequired)) {
                    Text(text = stringResource(id = R.string.mandatory_field))
                }
            },
            isError = error.value || (config.isBlank && config.isRequired),
            singleLine = true,
            trailingIcon = if (!config.isBlank) config.trailingIcon else null,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiLineFormField(
    @StringRes labelId: Int,
    value: String,
    config: FieldConfig = FieldConfig(),
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    val error = rememberSaveable { mutableStateOf(false) }
    val isConfigBlankAndRequired = config.isBlank && config.isRequired

    FormFieldTheme {
        OutlinedTextField(
            value = value,
            readOnly = config.isReadOnly,
            onValueChange = {
                onValueChange.invoke(it)
                error.value = config.isRequired && it.isBlank()
            },
            label = { Text(text = stringResource(id = labelId)) },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = config.keyboardType
            ),
            placeholder = {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            },
            supportingText = {
                if (error.value || isConfigBlankAndRequired) {
                    Text(text = stringResource(id = R.string.mandatory_field))
                }
            },
            isError = error.value || isConfigBlankAndRequired,
            singleLine = false,
            trailingIcon = if (!config.isBlank) config.trailingIcon else null,
            modifier = Modifier
                .fillMaxSize()
                .height(200.dp)
        )
    }
}

data class FieldConfig(
    val isRequired: Boolean = false,
    val isReadOnly: Boolean = false,
    val isBlank: Boolean = false,
    val isEnabled: Boolean = true,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val trailingIcon: @Composable (() -> Unit)? = null,
)

@Preview
@Composable
fun ShowFormField() {
    SouthWestTheme {
        FormField(
            labelId = R.string.ref_id,
            value = "Test",
            onValueChange = { }
        )
    }
}