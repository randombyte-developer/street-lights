package de.randombyte.streetlights

import com.google.inject.Inject
import org.slf4j.Logger
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.plugin.Plugin

@Plugin(id = PluginInfo.ID, name = PluginInfo.NAME, version = PluginInfo.VERSION, authors = arrayOf(PluginInfo.AUTHOR))
class StreetLights {

    @Inject
    lateinit var logger: Logger

    @Listener
    fun onInit(event: GameInitializationEvent) {
        logger.info("${PluginInfo.NAME} loaded: ${PluginInfo.VERSION}")
    }
}