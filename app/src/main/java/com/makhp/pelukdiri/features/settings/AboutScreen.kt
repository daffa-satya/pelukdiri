package com.makhp.pelukdiri.features.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.ui.theme.Dimens
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme

private const val REPOSITORY_URL = "https://github.com/daffa-satya/pelukdiri/"

@Composable
fun AboutScreen(onBackClick: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AboutHeader(onBackClick) },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            TextButton(onClick = { uriHandler.openUri(REPOSITORY_URL) }) {
                Text(REPOSITORY_URL, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun AboutHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Dimens.spaceExtraSmall, vertical = Dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
        }
        Text(
            text = stringResource(R.string.settings_about_header),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
        )
        Box(Modifier.size(Dimens.minTouchTarget))
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutPreview() {
    PELUKDIRITheme { AboutScreen(onBackClick = {}) }
}
