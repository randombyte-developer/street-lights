package de.randombyte.streetlights.commands

import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

object CommandUtils {

    /**
     * Ensures that a Player executed the command. Sends a message to the source otherwise.
     * @return If the CommandSource is a Player
     */
    fun CommandSource.ensurePlayer(): Boolean = if (this !is Player) {
        sendMessage("Command must be executed by a player!".toErrorText())
        false
    } else true

    /**
     * Wraps the String in a Text object with appropriate formattings.
     * @return Text with content of String and formatting.
     */
    fun String.toText(): Text = Text.of(this)
    fun String.toErrorText(): Text = Text.of(TextColors.RED, this)
    fun String.toNotifyText(): Text = Text.of(TextColors.YELLOW, this)
    fun String.toSuccessText(): Text = Text.of(TextColors.GREEN, this)

}