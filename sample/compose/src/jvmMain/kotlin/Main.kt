import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/**
 * @author Enaium
 */
public fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "XIcons KMP",
        ) {
            App()
        }
    }
}