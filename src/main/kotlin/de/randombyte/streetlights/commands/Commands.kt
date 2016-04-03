package de.randombyte.streetlights.commands

import org.spongepowered.api.Sponge

object Commands {
    val commands = arrayOf<CommandCallableProvider>(ToggleEditorMode)

    fun register(plugin: Any) {
        commands.forEach { command ->
            Sponge.getCommandManager().register(plugin, command.getCallable(), command.getAliases())
        }
    }
}