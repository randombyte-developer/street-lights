package de.randombyte.streetlights

import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

object TextUtils {
    /**
     * Wraps the String in a Text object with appropriate formattings.
     * @return Text with content of String and formatting.
     */
    fun String.toErrorText(): Text = Text.of(TextColors.RED, this)
    fun String.toNotifyText(): Text = Text.of(TextColors.YELLOW, this)
    fun String.toSuccessText(): Text = Text.of(TextColors.GREEN, this)
}