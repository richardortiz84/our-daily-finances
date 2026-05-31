package com.daytimegaming.ourdailyfinances

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.plaid.link.OpenPlaidLink
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidEventBus
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val plaidEventBus: PlaidEventBus by inject()

    private val plaidLauncher = registerForActivityResult(OpenPlaidLink()) { result ->
        when (result) {
            is LinkSuccess -> lifecycleScope.launch {
                plaidEventBus.accountLinked(
                    publicToken = result.publicToken,
                    institutionName = result.metadata.institution?.name,
                )
            }
            is LinkExit -> {
                result.error?.let { error ->
                    android.util.Log.e("PlaidLink", "LinkExit error: ${error.errorCode} - ${error.errorMessage}")
                }
            }
            else -> Unit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            App(
                darkTheme = isSystemInDarkTheme(),
                dynamicColor = false,
                onPlaidTokenReady = { linkToken ->
                    val config = LinkTokenConfiguration.Builder()
                        .token(linkToken)
                        .build()
                    plaidLauncher.launch(config)
                },
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        darkTheme = false,
        dynamicColor = false,
    )
}
