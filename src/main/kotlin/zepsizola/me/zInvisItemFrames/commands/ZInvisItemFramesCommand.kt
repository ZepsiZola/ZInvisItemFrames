package zepsizola.me.zInvisItemFrames.commands

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import zepsizola.me.zInvisItemFrames.ZInvisItemFrames

class ZInvisItemFramesCommand(private val plugin: ZInvisItemFrames) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("zinvisitemframes.admin")) {
            plugin.messageUtil.sendMessage(sender, "no-access")
            return true
        }
        
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "help" -> sendHelp(sender)
            "reload" -> reloadCommand(sender)
            "item" -> itemCommand(sender)
            "give" -> giveCommand(sender, args)
            else -> sendHelp(sender)
        }
        
        return true
    }
    
    private fun sendHelp(sender: CommandSender) {
        plugin.messageUtil.sendMessage(sender, "help")
    }
    
    private fun reloadCommand(sender: CommandSender) {
        plugin.reloadPlugin()
        plugin.messageUtil.sendMessage(sender, "success.reload")
    }
    
    private fun itemCommand(sender: CommandSender) {
        if (sender !is Player) {
            plugin.messageUtil.sendMessage(sender, "error.not-player")
            return
        }
        
        val item = sender.inventory.itemInMainHand
        if (item.type == Material.AIR) {
            plugin.messageUtil.sendMessage(sender, "error.mainhand-empty")
            return
        }
        
        // Save the item to config
        plugin.config.set("recipe.item", item)
        plugin.saveConfig()
        
        // Re-register recipes
        plugin.registerRecipes()
        
        plugin.messageUtil.sendMessage(sender, "success.recipe-update")
    }
    
    private fun giveCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            plugin.messageUtil.sendMessage(sender, "error.usage-give")
            return
        }
        
        val targetName = args[1]
        val target = Bukkit.getPlayer(targetName)
        
        if (target == null) {
            plugin.messageUtil.sendMessage(sender, "error.player-not-found")
            return
        }
        
        val amount = try {
            args[2].toInt()
        } catch (e: NumberFormatException) {
            plugin.messageUtil.sendMessage(sender, "error.must-be-number")
            return
        }
        
        val isGlowing = args.size > 3 && args[3] == "--glow"
        val material = if (isGlowing) Material.GLOW_ITEM_FRAME else Material.ITEM_FRAME
        val persistentKey = plugin.invisItemFrameKey
        val nameKey = if (isGlowing) "invisible_glow_item_frame" else "invisible_item_frame"
        
        // Create the invisible item frame
        val itemFrame = ItemStack(material, amount)
        val meta = itemFrame.itemMeta
        
        // Set custom name
        meta.displayName(plugin.messageUtil.formatName(nameKey))
        
        // Mark as invisible item frame
        meta.persistentDataContainer.set(persistentKey, PersistentDataType.BYTE, 1)
        itemFrame.itemMeta = meta
        
        // Give to player
        target.inventory.addItem(itemFrame)
        
        // Send success message
        plugin.messageUtil.sendMessage(
            sender, 
            "success.give", 
            "player" to target.name,
            "amount" to amount.toString(),
            "item_frame_type" to (plugin.config.getString("name.$nameKey") ?: nameKey)
        )
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.hasPermission("zinvisitemframes.admin")) {
            return emptyList()
        }
        
        return when (args.size) {
            1 -> listOf("help", "reload", "item", "give").filter { it.startsWith(args[0].lowercase()) }
            2 -> {
                if (args[0].equals("give", ignoreCase = true)) {
                    return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
                }
                return emptyList()
            }
            3 -> {
                if (args[0].equals("give", ignoreCase = true)) {
                    return listOf("1", "5", "10", "64").filter { it.startsWith(args[2]) }
                }
                return emptyList()
            }
            4 -> {
                if (args[0].equals("give", ignoreCase = true)) {
                    return listOf("--glow").filter { it.startsWith(args[3].lowercase()) }
                }
                return emptyList()
            }
            else -> emptyList()
        }
    }
}

