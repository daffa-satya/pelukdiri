package com.makhp.pelukdiri.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
import com.makhp.pelukdiri.ui.theme.Dimens
import com.makhp.pelukdiri.ui.theme.PELUKDIRITheme

@Composable
fun TermsScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TermsHeader(onBackClick)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(DashboardTokens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardTokens.MediumGap)
        ) {
            item {
                Text(
                    text = stringResource(R.string.terms_title_full),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.terms_last_updated),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                TermsSection(
                    title = stringResource(R.string.onboarding_terms_1_title),
                    content = stringResource(R.string.onboarding_terms_1_desc)
                )
            }
            
            item {
                TermsSection(
                    title = stringResource(R.string.onboarding_terms_2_title),
                    content = stringResource(R.string.onboarding_terms_2_desc)
                )
            }
            
            item {
                TermsSection(
                    title = stringResource(R.string.onboarding_terms_3_title),
                    content = stringResource(R.string.onboarding_terms_3_desc)
                )
            }
            
            item {
                TermsSection(
                    title = stringResource(R.string.onboarding_terms_4_title),
                    content = stringResource(R.string.onboarding_terms_4_desc)
                )
            }
            
            item {
                TermsSection(
                    title = stringResource(R.string.onboarding_terms_6_title), // No step 5 in main Res? Oh wait.
                    content = stringResource(R.string.onboarding_terms_6_desc)
                )
            }
            
            item { Spacer(Modifier.height(DashboardTokens.LargeGap)) }
        }
    }
}

@Composable
private fun TermsSection(title: String, content: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(Dimens.spaceExtraSmall))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Dimens.spaceSmall))
    }
}

@Composable
private fun TermsHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spaceExtraSmall, vertical = Dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
        }
        Text(
            text = stringResource(R.string.terms_header),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium
        )
        Box(Modifier.size(Dimens.minTouchTarget))
    }
}

@Preview(showBackground = true)
@Composable
private fun TermsPreview() {
    PELUKDIRITheme {
        TermsScreen(onBackClick = {})
    }
}
