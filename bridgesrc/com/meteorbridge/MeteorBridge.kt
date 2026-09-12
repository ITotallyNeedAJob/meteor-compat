package com.meteorbridge

import meteordevelopment.meteorclient.settings.BoolSetting
import meteordevelopment.meteorclient.settings.DoubleSetting
import meteordevelopment.meteorclient.settings.EnumSetting
import meteordevelopment.meteorclient.settings.IntSetting
import meteordevelopment.meteorclient.settings.Setting
import meteordevelopment.meteorclient.settings.StringSetting
import meteordevelopment.meteorclient.systems.modules.Module
import meteordevelopment.meteorclient.systems.modules.Modules
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.fabricmc.loader.api.FabricLoader
import java.util.function.Supplier

/**
 * Mirrors every meteor-compat module into LiquidBounce's ClickGUI (Misc tab).
 *
 * NOTE: the bridge used to be its own mod (meteor-bridge) compiled against a
 * meteor-compat.jar copy; it is now part of this mod, so it compiles against
 * these sources directly and no jar refresh is ever needed.
 *
 * MeteorBridgeModule is a tiny status/loader module: once per second it checks
 * whether the `meteor-client` mod (our meteor-compat host) is present, and wraps
 * each of its modules in a [MeteorProxyModule] registered with LB's ModuleManager.
 * Late-loading Meteor addons are picked up automatically on later ticks.
 *
 * Toggle + supported settings (bool/int/double/string/enum) sync both ways:
 * LB -> Meteor on user edit, Meteor -> LB on tick (so `.toggle` in chat stays consistent).
 */
object MeteorBridgeModule : ClientModule(
    name = "MeteorBridge",
    category = ModuleCategories.MISC
) {
    private var linked = false
    private var tickCount = 0
    private val proxies = mutableMapOf<Module, MeteorProxyModule>()

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        tickCount++
        if (tickCount % 20 != 0) return@handler
        try {
            if (!FabricLoader.getInstance().isModLoaded("meteor-client")) return@handler
            val all = try {
                Modules.get().getAll()
            } catch (t: Throwable) {
                return@handler
            } ?: return@handler

            for (m in all) {
                if (m == null || proxies.containsKey(m)) continue
                val rawName = try {
                    m.name
                } catch (t: Throwable) {
                    null
                }
                val base = uniqueModuleName(rawName)
                try {
                    proxies[m] = addProxy(m, base)
                } catch (t: Throwable) {
                    println("[MeteorBridge] Could not mirror '${base}' (${m.javaClass.name}): $t")
                }
            }

            if (!linked && proxies.isNotEmpty()) {
                linked = true
                // NOTE: no ClientModule.message() here on purpose: its signature
                // touches net.minecraft classes, which this merged (yarn) module
                // cannot resolve from the official-named LB jar. Console is enough.
                println("[MeteorBridge] Linked ${proxies.size} Meteor modules into ClickGUI (Misc).")
            }

            // Central pull-sync: proxy tick handlers only run while the proxy is
            // enabled, so converge Meteor -> LB here (this module stays enabled).
            for ((_, proxy) in proxies) {
                runCatching { proxy.pullFromMeteor() }
            }
        } catch (t: Throwable) {
            println("[MeteorBridge] tick failed: $t")
        }
    }
}

class MeteorProxyModule(val mmod: Module, lbName: String = mmod.name) : ClientModule(
    name = lbName,
    category = ModuleCategories.MISC
) {
    private var syncing = false
    private var ticks = 0
    private val mirrors = mutableListOf<() -> Unit>()
    private var skipped = 0

    init {
        val seenSettings = mutableSetOf<String>()
        for (group in mmod.settings) {
            for (s in group) {
                try {
                    val key = s.name
                    if (key.isBlank()) {
                        skipped++
                        continue
                    }
                    if (!seenSettings.add(key.lowercase())) {
                        skipped++
                        println("[MeteorBridge] Skipping duplicate setting '${mmod.name}.${key}'.")
                        continue
                    }
                    if (!mirrorSetting(s)) skipped++
                } catch (t: Throwable) {
                    skipped++
                    println("[MeteorBridge] Skipping setting '${mmod.name}.${try { s.name } catch (_: Throwable) { "?" }}': $t")
                }
            }
        }
        if (skipped > 0) {
            println("[MeteorBridge] '${mmod.name}': mirrored ${mirrors.size} settings, skipped $skipped (unsupported types).")
        }
    }

    override fun onEnabled() {
        if (!syncing && !mmod.isActive()) {
            runCatching { mmod.toggle() }
        }
    }

    override fun onDisabled() {
        if (!syncing && mmod.isActive()) {
            runCatching { mmod.toggle() }
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (++ticks % 10 != 0) return@handler
        runCatching { pullFromMeteor() }
    }

    /** Copies Meteor state into the LB proxy (toggle + settings). LB -> Meteor push happens via onEnabled/onDisabled/onChanged. */
    fun pullFromMeteor() {
        try {
            syncing = true
            if (enabled != mmod.isActive()) {
                enabled = mmod.isActive()
            }
            for (pull in mirrors) {
                runCatching { pull() }
            }
        } catch (_: Throwable) {
        } finally {
            syncing = false
        }
    }

    /**
     * Creates an LB value for a Meteor setting. Returns false when the type
     * has no ClickGUI equivalent yet (color/keybind/lists/...).
     */
    private fun mirrorSetting(s: Setting<*>): Boolean {
        when (s) {
            is BoolSetting -> {
                val v = boolean(s.name, s.get())
                v.onChanged { nv -> if (!syncing) runCatching { s.set(nv) } }
                mirrors.add {
                    val cur = s.get()
                    if (v.get() != cur) v.set(cur)
                }
            }

            is IntSetting -> {
                val def = s.get().coerceIn(s.min, s.max)
                val v = int(s.name, def, s.min..s.max)
                v.onChanged { nv -> if (!syncing) runCatching { s.set(nv) } }
                mirrors.add {
                    val cur = s.get().coerceIn(s.min, s.max)
                    if (v.get() != cur) v.set(cur)
                }
            }

            is DoubleSetting -> {
                val lo = if (s.min.isFinite()) s.min.toFloat() else -Float.MAX_VALUE
                val hi = if (s.max.isFinite()) s.max.toFloat() else Float.MAX_VALUE
                val def = s.get().toFloat().coerceIn(lo, hi)
                val v = float(s.name, def, lo..hi)
                v.onChanged { nv -> if (!syncing) runCatching { s.set(nv.toDouble()) } }
                mirrors.add {
                    val cur = s.get().toFloat().coerceIn(lo, hi)
                    if (kotlin.math.abs(v.get() - cur) > 1e-6f) v.set(cur)
                }
            }

            is StringSetting -> {
                val v = text(s.name, s.get())
                v.onChanged { nv -> if (!syncing) runCatching { s.set(nv) } }
                mirrors.add {
                    val cur = s.get()
                    if (v.get() != cur) v.set(cur)
                }
            }

            is EnumSetting<*> -> {
                val cur = s.get() ?: return false
                val cls = cur.javaClass
                val names = cls.enumConstants.map { (it as Enum<*>).name }
                val v = text(s.name, cur.name)
                v.description = Supplier { "Options: ${names.joinToString(", ")}" }
                v.onChanged { nv ->
                    if (syncing) return@onChanged
                    val found = cls.enumConstants.firstOrNull { (it as Enum<*>).name == nv }
                    if (found != null) {
                        runCatching { setRaw(s, found) }
                    } else {
                        // Invalid option typed in: revert to the live value.
                        runCatching {
                            syncing = true
                            try {
                                v.set((s.get() as Enum<*>).name)
                            } finally {
                                syncing = false
                            }
                        }
                    }
                }
                mirrors.add {
                    val live = (s.get() as Enum<*>).name
                    if (v.get() != live) v.set(live)
                }
            }

            else -> return false
        }
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun setRaw(s: Setting<*>, value: Any) {
        (s as Setting<Any>).set(value)
    }
}

private fun addProxy(m: Module, lbName: String): MeteorProxyModule {
    val proxy = MeteorProxyModule(m, lbName)
    ModuleManager.addModule(proxy)
    return proxy
}

/**
 * Hostile-name hardening for obfuscated addons: blank names get a fallback,
 * and every name is deduplicated case-insensitively (LB rejects clashes with
 * its builtins, e.g. Meteor "zoom" vs LB "Zoom") plus against already-linked
 * proxies. Displayed names stay as close to the addon original as possible.
 */
private val usedLbNames = mutableSetOf<String>()
private var lbNamesSeeded = false

private fun uniqueModuleName(raw: String?): String {
    // Seed once with LB's own modules so builtin clashes (e.g. Meteor "zoom"
    // vs LB "Zoom", matched case-insensitively) are renamed, not rejected.
    if (!lbNamesSeeded) {
        lbNamesSeeded = true
        try {
            for (mod in ModuleManager.getModules()) {
                usedLbNames.add(mod.name.lowercase())
            }
        } catch (_: Throwable) {
        }
    }
    var base = raw?.trim().orEmpty()
    if (base.isEmpty()) {
        base = "AddonModule"
    }
    var candidate = base
    var n = 2
    while (!usedLbNames.add(candidate.lowercase())) {
        candidate = "$base $n"
        n++
    }
    if (candidate != base) {
        println("[MeteorBridge] Renamed '${raw}' -> '${candidate}' (name clash).")
    }
    return candidate
}
