package zepsizola.me.zInvisItemFrames

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import zepsizola.me.zInvisItemFrames.commands.ZInvisItemFramesCommand
import zepsizola.me.zInvisItemFrames.listeners.ItemFrameListener
import zepsizola.me.zInvisItemFrames.util.MessageUtil

class ZInvisItemFrames : JavaPlugin() {
    
    lateinit var invisItemFrameKey: NamespacedKey
    lateinit var messageUtil: MessageUtil
    var glowEnabled: Boolean = true
    var checkPermissions: Boolean = false
    
    override fun onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig()
        
        // Initialize keys
        invisItemFrameKey = NamespacedKey(this, "invisible-item-frame")
        
        // Initialize message utility
        messageUtil = MessageUtil(this)
        
        // Register command
        getCommand("zinvisitemframes")?.setExecutor(ZInvisItemFramesCommand(this))
        
        // Register listeners
        server.pluginManager.registerEvents(ItemFrameListener(this), this)
        

        reloadPlugin()
        
        logger.info("ZInvisItemFrames has been enabled!")
    }
    
    override fun onDisable() {
        // Clear existing recipes
        server.removeRecipe(NamespacedKey(this, "invisible_item_frame"))
        server.removeRecipe(NamespacedKey(this, "invisible_glow_item_frame"))
        logger.info("ZInvisItemFrames has been disabled!")
    }
    
    fun registerRecipes() {
        // Clear existing recipes
        server.removeRecipe(NamespacedKey(this, "invisible_item_frame"))
        server.removeRecipe(NamespacedKey(this, "invisible_glow_item_frame"))
        
        // Get recipe item from config
        val recipeItem = config.getItemStack("recipe.item")
        
        // Only register recipes if a recipe item is set
        if (recipeItem != null) {
            // Register invisible item frame recipe
            registerInvisItemFrameRecipe(recipeItem, false)
            
            // Register invisible glow item frame recipe
            registerInvisItemFrameRecipe(recipeItem, true)
        }
    }
    
    private fun registerInvisItemFrameRecipe(centerItem: ItemStack, isGlowing: Boolean) {
        val material = if (isGlowing) Material.GLOW_ITEM_FRAME else Material.ITEM_FRAME
        val key = if (isGlowing) "invisible_glow_item_frame" else "invisible_item_frame"
        val persistentKey = invisItemFrameKey
        
        // Create result item
        val recipeResult = ItemStack(material, config.getInt("recipe.quantity", 1))
        val meta = recipeResult.itemMeta
        
        // Set custom name from config
        val name = config.getString("name.$key")
        if (name != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name))
        }
        
        // Mark as invisible item frame
        meta.persistentDataContainer.set(persistentKey, PersistentDataType.BYTE, 1)
        recipeResult.itemMeta = meta
        
        // Create recipe
        val recipe = ShapedRecipe(NamespacedKey(this, key), recipeResult)
        recipe.shape("III", "ICI", "III")
        recipe.setIngredient('I', RecipeChoice.MaterialChoice(material))
        recipe.setIngredient('C', RecipeChoice.ExactChoice(centerItem))
        
        // Register recipe
        server.addRecipe(recipe)
    }
    
    fun reloadPlugin() {
        reloadConfig()
        registerRecipes()
        glowEnabled = this.config.getBoolean("empty-frame.glow", false)
        checkPermissions = this.config.getBoolean("recipe.check-permission", false)
    }
}

