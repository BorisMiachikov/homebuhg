package ru.homebuhg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.homebuhg.core.designsystem.theme.HomeBuhgTheme
import ru.homebuhg.core.domain.RecurringWorker
import ru.homebuhg.core.domain.SessionManager
import ru.homebuhg.core.sync.SyncWorker
import ru.homebuhg.navigation.AppNavHost
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(0, 0),
            navigationBarStyle = SystemBarStyle.auto(0, 0)
        )
        setTheme(R.style.Theme_HomeBuhg)
        super.onCreate(savedInstanceState)

        lifecycleScope.launch { sessionManager.ensureLocalSession() }

        val wm = WorkManager.getInstance(this)

        wm.enqueueUniquePeriodicWork(
            "recurring_work",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RecurringWorker>(1, TimeUnit.DAYS).build()
        )

        wm.enqueueUniquePeriodicWork(
            SyncWorker.NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(
                    androidx.work.Constraints(
                        requiredNetworkType = androidx.work.NetworkType.CONNECTED
                    )
                )
                .build()
        )

        setContent {
            HomeBuhgTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost()
                }
            }
        }
    }
}
