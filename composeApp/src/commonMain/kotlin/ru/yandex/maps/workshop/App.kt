package ru.yandex.maps.workshop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import ru.yandex.maps.workshop.common.CommonApp
import ru.yandex.maps.workshop.common.additional.context.PlatformContext

@Composable
expect fun rememberPlatformContext(): PlatformContext

@Composable
@Preview
fun App() {
    val platformContext = rememberPlatformContext()
    val app = remember {
        CommonApp(
            iamToken = BuildKonfig.gptToken,
            folderId = BuildKonfig.folderId,
            context = platformContext,
        )
    }

    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Workhop Sample")
        }
    }
}
