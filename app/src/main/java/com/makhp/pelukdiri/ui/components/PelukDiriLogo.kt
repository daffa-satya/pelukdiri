package com.makhp.pelukdiri.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.makhp.pelukdiri.R

@Composable
fun PelukDiriLogo(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    Image(
        painter = painterResource(id = R.drawable.pelukdiri_icon_ui),
        contentDescription = androidx.compose.ui.res.stringResource(id = R.string.common_logo_desc),
        modifier = modifier.size(size)
    )
}
