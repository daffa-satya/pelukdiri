package com.makhp.pelukdiri

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.makhp.pelukdiri.core.database.dao.UsageSensorDao
import com.makhp.pelukdiri.core.database.entity.UsageSensorLogEntity
import com.makhp.pelukdiri.features.dashboard.MainStatsScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var usageSensorDao: UsageSensorDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Temp Trigger di MainActivity.kt untuk verifikasi data terisi
        lifecycleScope.launch(Dispatchers.IO) {
            usageSensorDao.insertLog(
                UsageSensorLogEntity(
                    timestamp = System.currentTimeMillis(),
                    packageName = "com.ss.android.ugc.trill",
                    rawScreenTimeMs = 3600000L, // 60 mins
                    appOpeningFrequency = 12,
                    ambientLightLux = 240.5f
                )
            )
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainStatsScreen()
                }
            }
        }
    }
}