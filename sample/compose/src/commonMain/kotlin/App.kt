import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import cn.enaium.xicons.compose.FluentIcons
import cn.enaium.xicons.compose.fluent.AnimalDog

/**
 * @author Enaium
 */
@Composable
public fun App() {
    MaterialTheme {
        Column {
            Icon(FluentIcons.Filled.AnimalDog, contentDescription = null)
        }
    }
}
