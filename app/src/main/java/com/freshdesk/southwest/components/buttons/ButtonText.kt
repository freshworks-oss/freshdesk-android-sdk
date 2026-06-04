package com.freshdesk.southwest.components.buttons

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.freshdesk.southwest.R
import com.freshdesk.southwest.ui.theme.SouthWestTheme

@Composable
fun ButtonText(
    @StringRes textId: Int,
    modifier: Modifier = Modifier.padding(top = 8.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.surface
    ),
    onClick: () -> Unit = {}
) {
    ButtonText(
        text = stringResource(id = textId),
        modifier = modifier,
        colors = colors,
        onClick = onClick,
    )
}

@Composable
fun ButtonText(
    text: String,
    modifier: Modifier = Modifier.padding(top = 8.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.surface
    ),
    onClick: () -> Unit = {},
) {
    Button(
        onClick = { onClick.invoke() },
        shape = ShapeDefaults.Large,
        colors = colors,
        modifier = modifier,
    ) {
        Text(text = text)
    }
}

@Preview
@Composable
fun ShowButtonText() {
    SouthWestTheme {
        ButtonText(textId = R.string.open_support)
    }
}