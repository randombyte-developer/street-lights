package de.randombyte.streetlights

import com.google.inject.Inject
import de.randombyte.streetlights.commands.CommandUtils.toErrorText
import de.randombyte.streetlights.commands.CommandUtils.toNotifyText
import de.randombyte.streetlights.commands.CommandUtils.toSuccessText
import de.randombyte.streetlights.database.DbManager
import de.randombyte.streetlights.database.Lights
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.block.BlockSnapshot
import org.spongepowered.api.block.BlockType
import org.spongepowered.api.block.BlockTypes.*
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.data.Transaction
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
import java.util.concurrent.TimeUnit

@Plugin(id = StreetLights.ID, name = StreetLights.NAME, version = StreetLights.VERSION, authors = arrayOf(StreetLights.AUTHOR))
class StreetLights @Inject constructor (val logger: Logger, @ConfigDir(sharedRoot = false) val configDir: Path) {

    companion object {
        const val NAME = "StreetLights"
        const val ID = "de.randombyte.streelights"
        const val VERSION = "v0.1.1"
        const val AUTHOR = "RandomByte"

        val TICKS_PER_DAY = 24000
        val TIME_RANGE_LAMPS_ON = 12500..23500 //in ticks

        //default state of unmapped player should be false
        val playerEditMode = mutableMapOf<UUID, Boolean>()
    }

    //per world lamps state
    val lightsOn = mutableMapOf<UUID, Boolean>()

    @Listener
    fun onInit(event: GameInitializationEvent) {
        DbManager.path = configDir.resolve("database").toAbsolutePath().toString()

        //Scheduled task to update the lights based on time; runs every 10 seconds
        Sponge.getScheduler().createTaskBuilder()
                .interval(10, TimeUnit.SECONDS)
                .execute(Runnable { checkTime() })
                .submit(this)

        logger.info("$NAME loaded: $VERSION")
    }

    @Listener
    fun onPostInit(event: GamePostInitializationEvent) {
        //DEBUGGUNG
        //Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start()
    }

    /**
     * Adds a placed REDSTONE_LAMP to the system.
     */
    @Listener
    fun onPlaceLamp(event: ChangeBlockEvent.Place, @First player: Player) {
        if (!player.hasPermission("streetlights.create")) return
        //transaction AIR -> REDSTONE_LAMP so a user place event
        event.transactions.filter { it.checkType(AIR, REDSTONE_LAMP) }.forEach { transaction ->
            val location = transaction.final.location.get()
            val light = DbManager.getLight(location)
            if (light == null) {
                player.sendMessage(Text.builder()
                    .append(Text.of(TextColors.YELLOW, "[CLICK] to add as Light"))
                    .onClick(TextActions.executeCallback {
                        //Watch out, maybe the player has removed the lamp in the time between placing it and clicking text
                        if (!(location.block.type.equals(REDSTONE_LAMP) ||
                                //Also check for LIT_REDSTONE_LAMP because the lamp may get powered after placing
                                location.block.type.equals(LIT_REDSTONE_LAMP)))
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
    fun onBreakLamp(event: ChangeBlockEvent.Break) {
        event.transactions.filter { it.original.state.type.equals(REDSTONE_LAMP) }.forEach { transaction ->
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

    /**
     * Prevents lit lamps being updated by Minecraft. Vanilla behaviour: Lit lamps without a signal would
     * correctly be updated to unlit lamps.
     */
    @Listener
    fun onUpdateLampBlock(event: ChangeBlockEvent.Place) {
        //Minecraft updates LIT_REDSTONE_LAMP to REDSTONE_LAMP; prevent those updates by cancelling this event
        event.transactions.filter { it.checkType(LIT_REDSTONE_LAMP, REDSTONE_LAMP) }.forEach { transaction ->
            //Don't cancel all events that match: It could be a redstone signal that went off which mustn't be cancelled
            //So check if this is a registered block:
            val lampRegistered = DbManager.getLight(transaction.original.location.get()) != null
            if (lampRegistered) {
                //And check if this lamp should stay on; as this event get fired when this plugin turns the light off
                //it mustn't be cancelled otherwise lamps would never be turned off
                val lampsInWorldOn = lightsOn[transaction.original.location.get().extent.uniqueId] ?: false
                if (lampsInWorldOn) event.isCancelled = true
            }
        }
    }

    @Listener
    fun onChangeWeather(event: ChangeWorldWeatherEvent) {
        val lampsOn = !event.weather.equals(Weathers.CLEAR) //lamps on when not clear weather
        lightsOn[event.targetWorld.uniqueId] = lampsOn
        Lights.setLightsState(DbManager.getAllLights(event.targetWorld.uniqueId.toString()), lampsOn)
    }

    /**
     * Updates the [lightsOn]-state for all worlds with a sky and updates lights in world if [lightsOn]-state changed.
     */
    fun checkTime() {
        Sponge.getGame().server.worlds.filter { it.dimension.hasSky() }.forEach { world -> //Check all worlds with sky
            val dayTime = world.properties.worldTime % TICKS_PER_DAY
            val oldValue = lightsOn[world.uniqueId]
            val newValue = dayTime in TIME_RANGE_LAMPS_ON
            lightsOn[world.uniqueId] = newValue
            if (oldValue != newValue) {
                //LampsOn-state changed -> update all lights in target world
                Lights.setLightsState(DbManager.getAllLights(world.uniqueId.toString()), newValue)
            }
        }
    }

    /**
     * Checks whether a transaction matches the criteria. Pass null to ignore this criteria.
     * @original The [BlockType] the original snapshot should have
     * @final The BlockType the final snapshot should have
     * @return Whether the criteria matches this transaction
     */
    fun Transaction<BlockSnapshot>.checkType(original: BlockType? = null, final: BlockType? = null) =
            (original == null || this.original.state.type.equals(original)) &&
                    (final == null || this.final.state.type.equals(final))
}