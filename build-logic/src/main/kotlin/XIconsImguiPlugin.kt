/*
 * Copyright (c) 2026 Enaium
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.gradle.api.Plugin
import org.gradle.api.Project
import task.GenerateImguiTask
import task.SyncXIconsTask

/**
 * Generates the ImGui icon sources for the project's own library submodule.
 * `generateImgui` writes per-library icon files into
 * `build/generated/commonMain/kotlin` (registered as a source dir by each
 * submodule's build script) and every `compileKotlin` task depends on it.
 *
 * Set `-PxiconsImguiLibs=material,tabler` to restrict generation to a
 * subset of the synced `@sicons` packages.
 *
 * @author Enaium
 */
class XIconsImguiPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val sync = project.tasks.register("syncXIcons", SyncXIconsTask::class.java)
        project.tasks.register("generateImgui", GenerateImguiTask::class.java) {
            group = "xicons"
            dependsOn(sync)
        }

        project.tasks.configureEach {
            if (name.startsWith("compileKotlin")) {
                dependsOn("generateImgui")
            }
        }
    }
}
