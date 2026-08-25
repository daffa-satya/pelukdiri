package com.makhp.pelukdiri.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.ui.components.PelukDiriLogo
import com.makhp.pelukdiri.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformedConsentScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = Dimens.spaceExtraSmall, vertical = Dimens.spaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
                Text(
                    text = stringResource(R.string.informed_consent_header),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium
                )
                Box(Modifier.size(Dimens.minTouchTarget), contentAlignment = Alignment.Center) {
                    PelukDiriLogo(size = 28.dp)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = Dimens.spaceExtraLarge),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.informed_consent_title),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            item {
                val p1 = stringResource(R.string.informed_consent_p1)
                Text(
                    text = buildAnnotatedString {
                        append("        ")
                        val parts = p1.split("“PELUKDIRI (Perangkat Lunak Untuk Kesejahteraan Digital Remaja Indonesia): Sistem Pengaturan Waktu Layar Berbasis Teori Kontrol untuk Kesehatan Digital”")
                        append(parts[0])
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                            append("“PELUKDIRI (Perangkat Lunak Untuk Kesejahteraan Digital Remaja Indonesia): Sistem Pengaturan Waktu Layar Berbasis Teori Kontrol untuk Kesehatan Digital”")
                        }
                        if (parts.size > 1) append(parts[1])
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                val p2 = stringResource(R.string.informed_consent_p2)
                Text(
                    text = buildAnnotatedString {
                        append("        ")
                        val segments = p2.split("PELUKDIRI")
                        segments.forEachIndexed { index, segment ->
                            append(segment)
                            if (index < segments.size - 1) {
                                withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                                    append("PELUKDIRI")
                                }
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                Text(
                    text = "        " + stringResource(R.string.informed_consent_p3),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                val p4 = stringResource(R.string.informed_consent_p4)
                Text(
                    text = buildAnnotatedString {
                        append("        ")
                        val parts1 = p4.split("repeated measures")
                        append(parts1[0])
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append("repeated measures")
                        }
                        val remaining1 = parts1.getOrElse(1) { "" }
                        val parts2 = remaining1.split("PELUKDIRI")
                        append(parts2[0])
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("PELUKDIRI")
                        }
                        val remaining2 = parts2.getOrElse(1) { "" }
                        val parts3 = remaining2.split("waktu layar")
                        append(parts3[0])
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append("waktu layar")
                        }
                        if (parts3.size > 1) append(parts3[1])
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                val p5 = stringResource(R.string.informed_consent_p5)
                Text(
                    text = buildAnnotatedString {
                        append("        ")
                        // Simplified replacement for multiple occurrences and different tags
                        var current = p5
                        fun appendStyled(target: String, style: SpanStyle) {
                            val index = current.indexOf(target)
                            if (index != -1) {
                                append(current.substring(0, index))
                                withStyle(style = style) { append(target) }
                                current = current.substring(index + target.length)
                            }
                        }
                        appendStyled("smartphone", SpanStyle(fontStyle = FontStyle.Italic))
                        appendStyled("PELUKDIRI", SpanStyle(textDecoration = TextDecoration.Underline))
                        appendStyled("smartphone", SpanStyle(fontStyle = FontStyle.Italic))
                        append(current)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                val p6 = stringResource(R.string.informed_consent_p6)
                Text(
                    text = buildAnnotatedString {
                        append("        ")
                        val parts = p6.split("PELUKDIRI")
                        append(parts[0])
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("PELUKDIRI")
                        }
                        if (parts.size > 1) append(parts[1])
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                val p7 = stringResource(R.string.informed_consent_p7)
                Text(
                    text = buildAnnotatedString {
                        append("        ")
                        var current = p7
                        fun appendStyled(target: String, style: SpanStyle) {
                            val index = current.indexOf(target)
                            if (index != -1) {
                                append(current.substring(0, index))
                                withStyle(style = style) { append(target) }
                                current = current.substring(index + target.length)
                            }
                        }
                        appendStyled("waktu layar", SpanStyle(fontStyle = FontStyle.Italic))
                        appendStyled("feedback", SpanStyle(fontStyle = FontStyle.Italic))
                        append(current)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                val p8 = stringResource(R.string.informed_consent_p8)
                Text(
                    text = buildAnnotatedString {
                        append("        ")
                        val parts = p8.split("PELUKDIRI")
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("PELUKDIRI")
                        }
                        if (parts.size > 1) append(parts[1])
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                Text(
                    text = "        " + stringResource(R.string.informed_consent_p9),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                Text(
                    text = "        " + stringResource(R.string.informed_consent_p10),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                Text(
                    text = "        " + stringResource(R.string.informed_consent_p11),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                val p12 = stringResource(R.string.informed_consent_p12)
                Text(
                    text = buildAnnotatedString {
                        append("        ")
                        var current = p12
                        fun appendStyled(target: String, style: SpanStyle) {
                            val index = current.indexOf(target)
                            if (index != -1) {
                                append(current.substring(0, index))
                                withStyle(style = style) { append(target) }
                                current = current.substring(index + target.length)
                            }
                        }
                        appendStyled("josuaku091010@gmail.com", SpanStyle(fontWeight = FontWeight.Bold))
                        appendStyled("daffa.satya.alif@gmail.com", SpanStyle(fontWeight = FontWeight.Bold))
                        append(current)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }
        }
    }
}
