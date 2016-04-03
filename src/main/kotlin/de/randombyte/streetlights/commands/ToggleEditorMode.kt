package de.randombyte.streetlights.commands

import de.randombyte.streetlights.StreetLights
import de.randombyte.streetlights.commands.CommandUtils.ensurePlayer
import de.randombyte.streetlights.commands.CommandUtils.toNotifyText
import de.randombyte.streetlights.commands.CommandUtils.toText
import org.spongepowered.api.command.CommandCallable
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.entity.living.player.Player

/**
 * Toggles the state of EditMode(saved in StreetLights.playerEditMode).
 */
object ToggleEditorMode : CommandCallableProvider {
    override fun getAliases() = listOf("toggleEditMode")
    override fun getCallable(): CommandCallable = CommandSpec.builder()
            .executor { src, ctx ->
                if (!src.ensurePlayer()) return@executor CommandResult.empty()
                val player = src as Player
                val newEditModeState = StreetLights.playerEditMode[player.uniqueId]?.not() ?: true //Defaults to true(being in EditMode)
                StreetLights.playerEditMode[player.uniqueId] = newEditModeState
                player.sendMessage(("EditMode: " + if (newEditModeState) "ON" else "OFF").toNotifyText())
                return@executor CommandResult.success()
            }
            .description("Toggles the EditMode".toText())
            .build()
}
