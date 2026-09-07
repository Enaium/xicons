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

package cn.enaium.xicons.imgui.sample

import cn.enaium.imgui.ImGui
import cn.enaium.imgui.ImGuiBackendFlags
import cn.enaium.imgui.ImGuiChildFlags
import cn.enaium.imgui.ImGuiCol
import cn.enaium.imgui.ImGuiCond
import cn.enaium.imgui.ImGuiStyleVar
import cn.enaium.imgui.ImGuiWindowFlags
import cn.enaium.imgui.ImVec2
import cn.enaium.imgui.ImVec4
import cn.enaium.imgui.backends.sdl.ImGuiSdlBackend
import cn.enaium.imgui.backends.sdl.ImGuiSdlRendererBackend
import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLColor
import cn.enaium.sdl.SDLInitFlags
import cn.enaium.sdl.SDLWindowFlags
import cn.enaium.xicons.imgui.Icon

/**
 * Renders the generated icon sets into an ImGui window: each row draws one
 * icon with its name; hovered icons are highlighted.
 *
 * @author Enaium
 */
internal fun runSample(frames: Int, iconSets: List<Pair<String, List<Pair<String, Icon>>>>) {
    SDL.setMainReady()
    if (!SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
        SDL.setHint("SDL_VIDEO_DRIVER", "dummy")
        check(SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) { "SDL_Init failed: ${SDL.error()}" }
        println("video init fell back to the dummy driver - running headless")
    }

    SDL.createWindow(
        title = "xicons imgui sample",
        width = 1280,
        height = 800,
        flags = SDLWindowFlags.RESIZABLE or SDLWindowFlags.HIGH_PIXEL_DENSITY,
    ).use { window ->
        SDL.createRenderer(window).use { renderer ->
            val context = ImGui.createContext()
            try {
                val imgui = ImGuiSdlBackend(window)
                val backend = ImGuiSdlRendererBackend(renderer)
                imgui.init()

                val fonts = ImGui.getIO().fonts
                // imgui-kmp's SDL renderer backend renders ImDrawCmd.VtxOffset,
                // but never advertises the capability: without the flag Dear
                // ImGui falls back to 16-bit indices and wraps once a draw list
                // exceeds 65535 vertices, corrupting everything drawn after.
                ImGui.getIO().backendFlags =
                    ImGui.getIO().backendFlags or ImGuiBackendFlags.RENDERER_HAS_VTX_OFFSET
                val density = maxOf(imgui.framebufferScale.x, imgui.framebufferScale.y, 1f)
                fonts.addFontDefault(cn.enaium.imgui.ImFontConfig(sizePixels = 13f * density, rasterizerDensity = density))
                check(fonts.build()) { "font atlas build failed" }
                val texData = fonts.getTexDataAsRGBA32()
                val fontTextureId = backend.uploadFontTexture(texData.pixels, texData.width, texData.height)
                fonts.setTexID(fontTextureId)

                var frameCount = 0
                var running = true
                var selected = 2 // Antd Twotone: multi-tone icons with tint layers
                var query = ""
                while (running && frameCount < frames) {
                    while (true) {
                        val event = SDL.pollEvent() ?: break
                        when (event) {
                            is cn.enaium.sdl.SDLEvent.Quit -> running = false
                            is cn.enaium.sdl.SDLEvent.Window ->
                                if (event.type == cn.enaium.sdl.SDLWindowEventType.CLOSE_REQUESTED) running = false
                            else -> imgui.processEvent(event)
                        }
                    }

                    imgui.newFrame()

                    val io = ImGui.getIO()
                    ImGui.setNextWindowPos(ImVec2(0f, 0f), ImGuiCond.ALWAYS)
                    ImGui.setNextWindowSize(io.displaySize, ImGuiCond.ALWAYS)
                    ImGui.begin(
                        "XIcons",
                        null,
                        ImGuiWindowFlags.NO_TITLE_BAR or ImGuiWindowFlags.NO_RESIZE or ImGuiWindowFlags.NO_MOVE,
                    )

                    ImGui.pushStyleVarVec2(ImGuiStyleVar.FRAME_PADDING, ImVec2(6f, 6f))
                    // Combo width fits the longest set title instead of a
                    // fixed size, and the search box gets the remaining line
                    // width, so nothing overflows the window.
                    val availW = ImGui.getContentRegionAvail().x
                    val maxTitle = iconSets.maxOf { it.first }
                    val comboW = ImGui.calcTextSize(maxTitle).x + ImGui.getFrameHeight() + 20f
                    ImGui.setNextItemWidth(comboW)
                    if (ImGui.beginCombo("Set", iconSets[selected].first)) {
                        iconSets.forEachIndexed { index, (title, _) ->
                            if (ImGui.selectable(title, selected == index)) selected = index
                        }
                        ImGui.endCombo()
                    }
                    ImGui.popStyleVar()
                    ImGui.sameLine()
                    val searchW = (availW - comboW - 8f - ImGui.calcTextSize("Search").x).coerceAtLeast(80f)
                    ImGui.setNextItemWidth(searchW)
                    query = ImGui.inputText("Search", query) ?: query

                    ImGui.separator()

                    val (title, icons) = iconSets[selected]
                    val filtered = icons.filter { (name, _) -> query.isBlank() || name.contains(query, ignoreCase = true) }
                    val childW = ImGui.getContentRegionAvail().x
                    if (ImGui.beginChild("icons", ImVec2(childW, 0f), ImGuiChildFlags.BORDERS)) {
                        ImGui.textUnformatted("$title - ${filtered.size} icons")
                        ImGui.separator()

                        val iconSize = Icon.DEFAULT_SIZE
                        val spacing = 8f
                        val dl = ImGui.getWindowDrawList()
                        val startPos = ImGui.getCursorScreenPos()
                        val avail = ImGui.getContentRegionAvail()
                        val columns = maxOf(1, (avail.x / (iconSize + spacing)).toInt())

                        // Virtualized drawing: only icons inside the visible
                        // viewport emit geometry. imgui-kmp's SDL renderer
                        // backend rebuilds every vertex from each cmd's
                        // VtxOffset to the end of the draw-list buffer, so a
                        // frame that draws all 1700+ icons at once becomes
                        // O(n^2) vertex copies and stalls the app.
                        val scrollY = ImGui.getScrollY()
                        val rows = (filtered.size + columns - 1) / columns
                        val firstVisibleRow = (scrollY / (iconSize + spacing)).toInt() - 2
                        val lastVisibleRow = ((scrollY + avail.y) / (iconSize + spacing)).toInt() + 2
                        val firstVisible = maxOf(0, firstVisibleRow * columns)
                        val lastVisible = minOf(filtered.size, (lastVisibleRow + 1) * columns)

                        for (index in firstVisible until lastVisible) {
                            val (name, icon) = filtered[index]
                            val col = index % columns
                            val row = index / columns
                            val x = startPos.x + col * (iconSize + spacing)
                            val y = startPos.y + row * (iconSize + spacing)
                            val mouse = ImGui.getMousePos()
                            val hovered = mouse.x in x..(x + iconSize) && mouse.y in y..(y + iconSize)

                            if (hovered) {
                                dl.DrawRectFilled(ImVec2(x, y), ImVec2(x + iconSize, y + iconSize), 0xFF3A3A44.toInt(), 4f)
                            }

                            // Icons are drawn via the current window draw
                            // list at the cursor position (like ImGui.text).
                            ImGui.setCursorScreenPos(ImVec2(x, y))
                            icon.draw(color = 0xFFCCCCCC.toInt())

                            if (hovered) {
                                ImGui.pushStyleColor(ImGuiCol.TEXT, ImVec4(1f, 0.85f, 0.3f, 1f))
                                ImGui.setTooltip(name)
                                ImGui.popStyleColor()
                            }
                        }

                        ImGui.dummy(ImVec2(avail.x, (filtered.size + columns - 1) / columns * (iconSize + spacing)))
                    }
                    ImGui.endChild()

                    ImGui.end()

                    ImGui.render()
                    renderer.drawColor = SDLColor(18, 18, 24, 255)
                    renderer.clear()
                    backend.renderDrawData(ImGui.getDrawData())
                    renderer.present()
                    frameCount++
                }

                backend.close()
            } finally {
                ImGui.destroyContext(context)
            }
        }
    }
    SDL.quit()
}

