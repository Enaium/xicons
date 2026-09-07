import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cn.enaium.xicons.compose.icons.AntdIcons
import cn.enaium.xicons.compose.icons.CarbonIcons
import cn.enaium.xicons.compose.icons.FaIcons
import cn.enaium.xicons.compose.icons.FluentIcons
import cn.enaium.xicons.compose.icons.Ionicons4Icons
import cn.enaium.xicons.compose.icons.Ionicons5Icons
import cn.enaium.xicons.compose.icons.MaterialIcons
import cn.enaium.xicons.compose.icons.TablerIcons
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * @author Enaium
 */
public fun main() {
    val iconSets = buildIconSets()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "XIcons KMP",
        ) {
            App(iconSets)
        }
    }
}

/**
 * Enumerates the style grouping objects (Filled/Outlined/...) of each icon
 * collection via reflection and resolves every ImageVector property. No
 * generated `all` lists exist, so unused icons can be pruned — the sample
 * still discovers them at runtime.
 */
private fun buildIconSets(): List<IconSet> {
    val sets = mutableListOf<IconSet>()
    val collections = listOf(
        "Antd" to AntdIcons,
        "Carbon" to CarbonIcons,
        "Fa" to FaIcons,
        "Fluent" to FluentIcons,
        "Ionicons4" to Ionicons4Icons,
        "Ionicons5" to Ionicons5Icons,
        "Material" to MaterialIcons,
        "Tabler" to TablerIcons,
    )
    for ((libName, collection) in collections) {
        val styleObjects = collection::class.memberProperties
            .filterIsInstance<KProperty1<Any, *>>()
            .filter { it.returnType.classifier == Any::class || it.getter.returnType.toString().contains('.') }
        for (prop in styleObjects) {
            val styleName = prop.name
            val styleObj = prop.get(collection) ?: continue
            val icons = styleObj::class.memberProperties
                .filterIsInstance<KProperty1<Any, *>>()
                .mapNotNull { member ->
                    val value = member.get(styleObj) as? ImageVector ?: return@mapNotNull null
                    member.name to value
                }
            if (icons.isNotEmpty()) {
                sets.add(IconSet("$libName $styleName", icons))
            }
        }
    }
    return sets
}