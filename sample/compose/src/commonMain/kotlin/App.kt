import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.enaium.xicons.compose.AntdIcons
import cn.enaium.xicons.compose.CarbonIcons
import cn.enaium.xicons.compose.FaIcons
import cn.enaium.xicons.compose.FluentIcons
import cn.enaium.xicons.compose.Ionicons4Icons
import cn.enaium.xicons.compose.Ionicons5Icons
import cn.enaium.xicons.compose.MaterialIcons
import cn.enaium.xicons.compose.TablerIcons

/**
 * @author Enaium
 */
private data class IconSet(val title: String, val icons: List<Pair<String, ImageVector>>)

private val iconSets = listOf(
    IconSet("Fluent Regular", FluentIcons.Regular.all),
    IconSet("Fluent Filled", FluentIcons.Filled.all),
    IconSet("Antd Filled", AntdIcons.Filled.all),
    IconSet("Antd Outlined", AntdIcons.Outlined.all),
    IconSet("Antd Twotone", AntdIcons.Twotone.all),
    IconSet("Carbon Default", CarbonIcons.Default.all),
    IconSet("Carbon Filled", CarbonIcons.Filled.all),
    IconSet("Carbon Round", CarbonIcons.Round.all),
    IconSet("Fa Default", FaIcons.Default.all),
    IconSet("Fa Regular", FaIcons.Regular.all),
    IconSet("Ionicons4 Default", Ionicons4Icons.Default.all),
    IconSet("Ionicons5 Default", Ionicons5Icons.Default.all),
    IconSet("Ionicons5 Sharp", Ionicons5Icons.Sharp.all),
    IconSet("Material Filled", MaterialIcons.Filled.all),
    IconSet("Material Outlined", MaterialIcons.Outlined.all),
    IconSet("Material Round", MaterialIcons.Round.all),
    IconSet("Material Sharp", MaterialIcons.Sharp.all),
    IconSet("Material Twotone", MaterialIcons.Twotone.all),
    IconSet("Tabler Default", TablerIcons.Default.all),
    IconSet("Tabler Filled", TablerIcons.Filled.all),
    IconSet("Tabler Sharp", TablerIcons.Sharp.all),
)

/**
 * @author Enaium
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun App() {
    MaterialTheme {
        var selected by remember { mutableIntStateOf(0) }
        var query by remember { mutableStateOf("") }
        Column(Modifier.fillMaxSize()) {
            PrimaryScrollableTabRow(selectedTabIndex = selected, edgePadding = 0.dp) {
                iconSets.forEachIndexed { index, iconSet ->
                    Tab(
                        selected = selected == index,
                        onClick = { selected = index },
                        text = { Text(iconSet.title) }
                    )
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                singleLine = true,
                placeholder = { Text("Search icons") }
            )
            val icons = iconSets[selected].icons.filter { (name, _) ->
                query.isBlank() || name.contains(query.trim(), ignoreCase = true)
            }
            FlowRow(
                modifier = Modifier.fillMaxSize().padding(8.dp).verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icons.forEach { (name, icon) ->
                    Column(
                        modifier = Modifier.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(icon, contentDescription = name)
                        Text(
                            name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
