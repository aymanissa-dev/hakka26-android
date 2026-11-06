package ke.droidcon.hakka

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import ke.droidcon.hakka.otp.OtpScreen
import ke.droidcon.hakka.otp.parseOtpPalette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val night = nightMode == Configuration.UI_MODE_NIGHT_YES
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = if (night) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            },
        )
        val coloursJson = assets.open("colours.json").bufferedReader().use { it.readText() }
        setContent {
            val dark = isSystemInDarkTheme()
            val palette = remember(coloursJson, dark) { parseOtpPalette(coloursJson, dark) }
            MaterialTheme {
                OtpScreen(palette = palette, dark = dark)
            }
        }
    }
}
