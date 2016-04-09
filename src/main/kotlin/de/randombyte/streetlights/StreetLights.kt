package de.randombyte.streetlights

import com.google.inject.Inject
import de.randombyte.streetlights.commands.CommandUtils.toErrorText
import de.randombyte.streetlights.commands.CommandUtils.toNotifyText
import de.randombyte.streetlights.commands.CommandUtils.toSuccessText
import de.randombyte.streetlights.database.DbManager
import de.randombyte.streetlights.database.Lights
import org.h2.tools.Server
import org.slf4j.Logger
import org.spongepowered.api.block.BlockTypes
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.block.ChangeBlockEvent
import org.spongepowered.api.event.filter.cause.First
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.event.game.state.GamePostInitializationEvent
import org.spongepowered.api.event.world.ChangeWorldWeatherEvent
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.action.TextActions
import org.spongepowered.api.text.format.TextColors
import org.spongepowered.api.world.weather.Weathers
import java.nio.file.Path
import java.util.*

@Plugin(id = PluginInfo.ID, name = PluginInfo.NAME, version = PluginInfo.VERSION, authors = arrayOf(PluginInfo.AUTHOR))
class StreetLights @Inject constructor (val logger: Logger, @ConfigDir(sharedRoot = false) val configDir: Path) {

    companion object {
        //default state when using not mapped player UUID should be false
        val playerEditMode = mutableMapOf<UUID, Boolean>()
    }

    @Listener
    fun onInit(event: GameInitializationEvent) {
        DbManager.path = configDir.resolve("database").toAbsolutePath().toString()
        logger.info("${PluginInfo.NAME} loaded: ${PluginInfo.VERSION}")
    }

    @Listener
    fun onPostInit(event: GamePostInitializationEvent) {
        //DEBUGGUNG
        Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start()
    }

    /**
     * Adds a placed REDSTONE_LAMP to the system.
     */
    @Listener
    fun onPlaceBlock(event: ChangeBlockEvent.Place, @First player: Player) {
        event.transactions.filter { it.final.state.type.equals(BlockTypes.REDSTONE_LAMP) }.forEach { transaction ->
            val location = transaction.final.location.get()
            val light = DbManager.getLight(location)
            if (light == null) {
                player.sendMessage(Text.builder()
                    .append(Text.of(TextColors.YELLOW, "[CLICK] to add as Light"))
                    .onClick(TextActions.executeCallback {
                        if (!location.block.type.equals(BlockTypes.REDSTONE_LAMP))
                            player.sendMessage("Redstone lamp isn't there anymore!".toErrorText())
                        else if (DbManager.getLight(location) == null) {
                            DbManager.addLight(location)
                            player.sendMessage("Added Light!".toSuccessText())
                        } else player.sendMessage("Already added!".toNotifyText())
                    })
                    .onHover(TextActions.showText(location.toString().toNotifyText())).build())
            } else {
                player.sendMessage("Already added!".toNotifyText())
            }
        }
    }

    /**
     * Removes a removed REDSTONE_LAMP from the system.
     */
    @Listener
    fun onBreakBlock(event: ChangeBlockEvent.Break) {
        event.transactions.filter { it.original.state.type.equals(BlockTypes.REDSTONE_LAMP) }.forEach { transaction ->
            val location = transaction.original.location.get()
            val light = DbManager.getLight(location)
            if (light != null) {
                DbManager.removeLight(light.id)
                event.cause.first(Player::class.java).ifPresent {
                    it.sendMessage("Removed Light!".toNotifyText())
                }
            }
        }
    }

    @Listener
    fun onChangeWeather(event: ChangeWorldWeatherEvent) {
        val lights = DbManager.getAllLights(event.targetWorld.uniqueId.toString())
        Lights.setLightsState(lights, !event.weather.equals(Weathers.CLEAR)) //on when not clear weather
    }
}