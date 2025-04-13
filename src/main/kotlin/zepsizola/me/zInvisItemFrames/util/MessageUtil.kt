package zepsizola.me.zInvisItemFrames.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import zepsizola.me.zInvisItemFrames.ZInvisItemFrames

class MessageUtil(private val plugin: ZInvisItemFrames) {
    
    private val miniMessage = MiniMessage.miniMessage()
    
    fun sendMessage(sender: CommandSender, key: String, vararg replacements: Pair<String, String>) {
        val message = getMessage(key, *replacements)
        if (message.isNotEmpty()) {
            sender.sendMessage(miniMessage.deserialize(message))
        }
    }
    
    fun getMessage(key: String, vararg replacements: Pair<String, String>): String {
        var message = plugin.config.getString("messages.$key") ?: return ""
        
        replacements.forEach { (placeholder, value) ->
            message = message.replace("%$placeholder%", value)
        }
        
        return message
    }
    
    fun formatName(key: String): Component {
        val name = plugin.config.getString("name.$key") ?: return Component.empty()
        return miniMessage.deserialize(name)
    }
}

