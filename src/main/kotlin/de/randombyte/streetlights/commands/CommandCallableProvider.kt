package de.randombyte.streetlights.commands

import org.spongepowered.api.command.CommandCallable

interface CommandCallableProvider {
    /**
     * Provides the CommandCallable.
     * @return CommandCallable for this command
     */
    fun getCallable(): CommandCallable

    /**
     * Provides a list of aliases.
     * @return List of aliases for this command
     */
    fun getAliases(): List<String>
}