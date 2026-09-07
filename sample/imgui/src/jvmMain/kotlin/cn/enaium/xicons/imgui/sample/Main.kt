package cn.enaium.xicons.imgui.sample

import cn.enaium.xicons.imgui.Icon
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * Scans the generated icon packages on the classpath by reflection and
 * groups icons by their style suffix (Filled/Outlined/Twotone/Default/...).
 * No generated collection objects exist, so unused icon classes can be
 * pruned by R8/ProGuard-style shrinking.
 */
internal fun scanIconSets(): List<Pair<String, List<Pair<String, Icon>>>> {
    val styleSuffixes = listOf("Filled", "Outlined", "Twotone", "Default", "Regular", "Sharp", "Round")
    val libs = listOf("antd", "carbon", "fa", "fluent", "ionicons4", "ionicons5", "material", "tabler")
    val classNames = scanClassNames("cn/enaium/xicons/imgui/icons")
    val sets = mutableListOf<Pair<String, List<Pair<String, Icon>>>>()

    for (lib in libs) {
        val prefix = "cn.enaium.xicons.imgui.icons.$lib."
        val libClasses = classNames.filter { it.startsWith(prefix) && !it.contains('$') }
        val byStyle = LinkedHashMap<String, MutableList<Pair<String, Icon>>>()
        for (name in libClasses) {
            // Kotlin top-level properties live in `<Name>Kt` classes with a
            // `<Name>` field; strip the Kt suffix for the display name.
            val simple = name.substringAfterLast('.')
            val baseName = simple.removeSuffix("Kt")
            val style = styleSuffixes.firstOrNull { baseName.endsWith(it) } ?: "Default"
            val displayName = baseName.removeSuffix(style)
            try {
                val iconClass = Class.forName(name)
                val iconField = iconClass.declaredFields.firstOrNull { it.type == Icon::class.java }
                    ?: iconClass.getDeclaredField(baseName)
                iconField.isAccessible = true
                val icon = iconField.get(null) as? Icon ?: continue
                byStyle.getOrPut(style) { mutableListOf() }.add(displayName to icon)
            } catch (e: Throwable) {
                // class without a public Icon value (helpers, data holders): skip
            }
        }
        byStyle.forEach { (style, icons) ->
            sets.add("${lib.replaceFirstChar { it.uppercase() }} $style" to icons)
        }
    }
    return sets
}

/** Enumerates class names under [pkgPath] from every classpath entry. */
private fun scanClassNames(pkgPath: String): List<String> {
    val found = LinkedHashSet<String>()
    val cp = System.getProperty("java.class.path").split(File.pathSeparator)
    for (entry in cp) {
        val f = File(entry)
        if (f.isDirectory) {
            val root = f.toPath()
            java.nio.file.Files.walk(root).use { stream ->
                stream.filter { it.toString().endsWith(".class") && it.toString().contains(pkgPath) }
                    .forEach { p ->
                        val rel = root.relativize(p).toString().removeSuffix(".class")
                            .replace(File.separatorChar, '.')
                        found.add(rel)
                    }
            }
        } else if (f.isFile && f.name.endsWith(".jar")) {
            try {
                JarFile(f).use { jar ->
                    jar.entries().asSequence()
                        .filter { it.name.startsWith("$pkgPath/") && it.name.endsWith(".class") }
                        .forEach { found.add(it.name.removeSuffix(".class").replace('/', '.')) }
                }
            } catch (e: Exception) {
                // unreadable jar: skip
            }
        }
    }
    return found.toList()
}

/** Parses `--frames N` (exit after N frames, for headless CI runs). */
private fun parseFrames(args: Array<String>): Int {
    var frames = Int.MAX_VALUE
    var i = 0
    while (i < args.size) {
        if (args[i] == "--frames" && i + 1 < args.size) {
            frames = args[i + 1].toIntOrNull() ?: Int.MAX_VALUE
            i++
        }
        i++
    }
    return frames
}

public fun main(args: Array<String>) {
    val frames = parseFrames(args)
    println("xicons imgui sample (frames=$frames)")
    val iconSets = scanIconSets()
    println("scanned ${iconSets.sumOf { it.second.size }} icons in ${iconSets.size} sets")
    runSample(frames, iconSets)
}
