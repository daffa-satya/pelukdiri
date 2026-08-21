package com.makhp.pelukdiri.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makhp.pelukdiri.features.dashboard.DashboardTokens
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
                    .padding(horizontal = Dimens.spaceExtraSmall, vertical = Dimens.spaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Informed Consent",
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
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "INFORMED CONSENT",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            item {
                Text(
                    text = buildAnnotatedString {
                        append("        Perkenalkan kami dari Tim “Mama, Aku Kecanduan HP” SMAN 70 Jakarta. Saat ini kami sedang melaksanakan penelitian dengan judul ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                            append("“PELUKDIRI (Perangkat Lunak Untuk Kesejahteraan Digital Remaja Indonesia): Sistem Pengaturan Waktu Layar Berbasis Teori Kontrol untuk Kesehatan Digital”")
                        }
                        append(".")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                Text(
                    text = buildAnnotatedString {
                        append("        Penelitian ini bertujuan untuk mengetahui apakah penggunaan aplikasi ")
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("PELUKDIRI")
                        }
                        append(" dengan intervensi tantangan kognitif adaptif dapat membantu mengurangi durasi penggunaan layar, khususnya pada aplikasi hiburan, tanpa meningkatkan tingkat stres pengguna secara signifikan. ")
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("PELUKDIRI")
                        }
                        append(" dirancang sebagai perangkat lunak kesejahteraan digital yang memberikan tantangan kognitif singkat berupa matematika mental dan pola ketika responden membuka aplikasi hiburan yang telah ditentukan.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                Text(
                    text = "        Manfaat penelitian ini adalah memberikan pengalaman kepada responden dalam menggunakan teknologi yang dirancang untuk membantu meningkatkan kesadaran terhadap penggunaan waktu layar. Selain itu, hasil penelitian diharapkan dapat menjadi dasar pengembangan perangkat lunak kesejahteraan digital yang lebih adaptif dan sesuai dengan kebutuhan remaja.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                Text(
                    text = buildAnnotatedString {
                        append("        Metode penelitian yang digunakan adalah penelitian kuantitatif eksperimental dengan desain ")
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append("repeated measures")
                        }
                        append(". Pengumpulan data dilakukan melalui aplikasi ")
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("PELUKDIRI")
                        }
                        append(" dan kuesioner. Responden akan menggunakan aplikasi selama periode penelitian, kemudian data penggunaan layar dan aktivitas intervensi akan dianalisis untuk melihat perubahan ")
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append("screen time")
                        }
                        append(" selama periode pengukuran.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                Text(
                    text = buildAnnotatedString {
                        append("        Penelitian ini dilakukan secara daring melalui ")
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append("smartphone")
                        }
                        append(" masing-masing responden. Responden akan menggunakan aplikasi ")
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("PELUKDIRI")
                        }
                        append(" selama periode penelitian yang telah ditentukan. Selama penelitian berlangsung, responden diminta menggunakan ")
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append("smartphone")
                        }
                        append(" seperti biasa dan tidak perlu sengaja mengubah kebiasaan penggunaan perangkatnya.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                Text(
                    text = buildAnnotatedString {
                        append("        Pada saat responden membuka aplikasi hiburan yang telah dipilih untuk diberikan intervensi, ")
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("PELUKDIRI")
                        }
                        append(" akan menampilkan tantangan kognitif singkat sebelum aplikasi dapat digunakan. Tingkat intervensi dapat disesuaikan berdasarkan performa responden dalam menyelesaikan tantangan dengan batas tingkat kesulitan yang telah ditentukan.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                Text(
                    text = buildAnnotatedString {
                        append("        Selama penelitian, aplikasi akan mengumpulkan data yang diperlukan untuk penelitian, seperti durasi ")
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append("screen time")
                        }
                        append(", durasi penggunaan aplikasi hiburan, jumlah pembukaan aplikasi, serta aktivitas dan performa intervensi. Responden juga akan diminta mengisi kuesioner untuk memberikan ")
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append("feedback")
                        }
                        append(" mengenai pengalaman menggunakan aplikasi.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("PELUKDIRI")
                        }
                        append(" tidak digunakan untuk membaca isi pesan, foto, video, kontak, kata sandi, maupun isi percakapan pribadi responden. Data yang dikumpulkan hanya digunakan untuk keperluan penelitian.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                Text(
                    text = "        Proses penggunaan aplikasi dapat menimbulkan ketidaknyamanan ringan, seperti merasa terganggu ketika intervensi muncul atau merasa kurang nyaman karena harus berhenti sejenak dari penggunaan aplikasi hiburan. Responden juga mungkin mengalami kesulitan ketika mengerjakan tantangan matematika mental atau pola. Intervensi dirancang dalam bentuk tantangan singkat dengan batas tingkat kesulitan yang telah ditentukan.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                Text(
                    text = "        Data yang diperoleh akan dijaga kerahasiaannya dan hanya dapat diakses oleh peneliti. Data penelitian akan disimpan secara lokal pada laptop peneliti. Identitas responden akan menggunakan kode dalam proses pengolahan data dan tidak akan dicantumkan dalam penyajian hasil penelitian. Hasil penelitian akan disajikan dalam bentuk data kelompok, tabel, grafik, atau analisis statistik sehingga identitas responden tidak dapat diketahui.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                Text(
                    text = "        Partisipasi dalam penelitian ini dilakukan sesuai prosedur penelitian dan persetujuan responden serta orang tua atau wali. Responden akan mendapatkan kompensasi sebesar Rp50.000,00 sebagai penghargaan atas waktu dan partisipasinya dalam penelitian. Kompensasi tersebut bukan merupakan paksaan untuk mengikuti penelitian.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            item {
                Text(
                    text = buildAnnotatedString {
                        append("        Apabila terdapat pertanyaan, kendala dalam penggunaan aplikasi, atau hal yang ingin disampaikan berkaitan dengan penelitian ini, Anda dapat menghubungi: ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("0821-1479-9930")
                        }
                        append(" (Joshua Leonardo Hilman Hutajulu) dan ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("0881-0118-64140")
                        }
                        append(" (Daffa Satya Alif Siregar).")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }
            
            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
