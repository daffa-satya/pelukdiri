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
import androidx.compose.ui.unit.dp
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
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
                    text = "Syarat & Ketentuan Penggunaan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Terakhir diperbarui: 7 Agustus 2026",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                TermsSection(
                    title = "1. Penerimaan Ketentuan",
                    content = "Dengan mengunduh atau menggunakan aplikasi PELUKDIRI, Anda setuju untuk terikat oleh syarat dan ketentuan ini. Jika Anda tidak setuju, harap jangan gunakan aplikasi ini."
                )
            }
            
            item {
                TermsSection(
                    title = "2. Penggunaan Aplikasi",
                    content = "PELUKDIRI dirancang untuk membantu Anda mengelola waktu layar. Anda bertanggung jawab penuh atas penggunaan aplikasi ini dan segala keputusan yang Anda buat berdasarkan informasi yang disediakan."
                )
            }
            
            item {
                TermsSection(
                    title = "3. Privasi Data",
                    content = "Privasi Anda sangat penting bagi kami. Semua data penggunaan aplikasi diproses secara lokal di perangkat Anda dan tidak dikirim ke server kami, kecuali jika Anda secara eksplisit melakukan ekspor data."
                )
            }
            
            item {
                TermsSection(
                    title = "4. Hak Kekayaan Intelektual",
                    content = "Semua konten, logo, dan teknologi di dalam PELUKDIRI adalah milik MAKHP Studio dan dilindungi oleh undang-undang hak cipta yang berlaku."
                )
            }
            
            item {
                TermsSection(
                    title = "5. Perubahan Ketentuan",
                    content = "Kami dapat memperbarui syarat dan ketentuan ini dari waktu ke waktu. Kami akan memberi tahu Anda tentang perubahan apa pun dengan memperbarui tanggal di bagian atas halaman ini."
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
        Spacer(Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TermsHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = "Terms",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium
        )
        Box(Modifier.size(48.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun TermsPreview() {
    PELUKDIRITheme {
        TermsScreen(onBackClick = {})
    }
}
