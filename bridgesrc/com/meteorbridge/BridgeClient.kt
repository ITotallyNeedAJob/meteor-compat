package com.meteorbridge

import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.fabricmc.api.ClientModInitializer
import java.util.concurrent.atomic.AtomicBoolean

class BridgeClient : ClientModInitializer {

    private val started = AtomicBoolean(false)

    override fun onInitializeClient() {
        if (!started.compareAndSet(false, true)) return

        if (tryRegister()) {
            return
        }

        val thread = Thread {
            var attempts = 0

            while (attempts < 60) {
                attempts++

                try {
                    Thread.sleep(1000L)

                    if (tryRegister()) {
                        return@Thread
                    }
                } catch (e: InterruptedException) {
                    return@Thread
                } catch (t: Throwable) {
                    // Wait and retry.
                }
            }

            println("[MeteorBridge] Giving up module registration after $attempts attempts. Is LiquidBounce installed?")
        }

        thread.isDaemon = true
        thread.name = "MeteorBridge-Init"
        thread.start()
    }

    private fun tryRegister(): Boolean {
        return try {
            ModuleManager.addModule(MeteorBridgeModule)
            println("[MeteorBridge] Bridge module registered (waiting for meteor-client).")
            true
        } catch (t: Throwable) {
            false
        }
    }
}
