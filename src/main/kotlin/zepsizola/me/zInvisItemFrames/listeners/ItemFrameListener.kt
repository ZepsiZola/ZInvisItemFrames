package zepsizola.me.zInvisItemFrames.listeners

import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.function.Consumer
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import zepsizola.me.zInvisItemFrames.ZInvisItemFrames

class ItemFrameListener(private val plugin: ZInvisItemFrames) : Listener {
    
    // Returns true if the item is an invisible item frame.
    fun itemIsInvisFrame(item: ItemStack): Boolean {
        return (item.hasItemMeta()
            && item.itemMeta.persistentDataContainer.has(plugin.invisItemFrameKey, PersistentDataType.BYTE))
    }

    // Retrns the item frame if it is an invisible item frame, else returns null.
    fun getInvisItemFrame(entity: Entity): ItemFrame? {
        if (entity.type != EntityType.ITEM_FRAME && entity.type != EntityType.GLOW_ITEM_FRAME) {
            return null
        }
        val itemFrame = entity as ItemFrame
        if (!itemFrame.persistentDataContainer.has(plugin.invisItemFrameKey, PersistentDataType.BYTE)) {
            return null
        }
        return itemFrame
    }

    fun handleInvisFrameItemChange(entity: Entity) {
        val itemFrame = getInvisItemFrame(entity) ?: return
        itemFrame.scheduler.run(plugin, Consumer { _: ScheduledTask ->
            val isEmpty = itemFrame.item.type == Material.AIR
            itemFrame.isGlowing = plugin.glowEnabled && isEmpty
            itemFrame.isVisible = isEmpty
        }, null)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        if (!plugin.checkPermissions) {
            return
        }
        val result = event.inventory.result ?: return
        if (!itemIsInvisFrame(result)) {
            return
        }
        val player = event.view.player as? Player ?: return
        val permission = if (result.type == Material.GLOW_ITEM_FRAME) {
            "zinvisitemframes.craft.glow_item_frame"
        } else {
            "zinvisitemframes.craft.item_frame"
        }
        if (!player.hasPermission(permission)) {
            event.inventory.result = null
            // plugin.messageUtil.sendMessage(player, "error.no-access")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInvisFramePlace(event: HangingPlaceEvent) {
        val player = event.player ?: return
        val entity = event.entity
        if (entity.type != EntityType.ITEM_FRAME && entity.type != EntityType.GLOW_ITEM_FRAME) {
            return
        }
        // Checks if...
        // - The item in the main hand is an invisible item frame.
        // - The item in the off hand is an invisible item frame and the main hand is not an item frame.
        if (!itemIsInvisFrame(player.inventory.itemInMainHand)) {
            if (!(itemIsInvisFrame(player.inventory.itemInOffHand) && (player.inventory.itemInMainHand.type != Material.ITEM_FRAME && player.inventory.itemInMainHand.type != Material.GLOW_ITEM_FRAME))) {
                return
            }
        }
        val itemFrame = entity as ItemFrame
        // Set the placed item frame to be visible.
        itemFrame.setVisible(true)
        // Sets metadata for the item frame to show that it is an invisible item frame.
        itemFrame.persistentDataContainer.set(plugin.invisItemFrameKey, PersistentDataType.BYTE, 1)
        // Item frame only glows if empty-frame.glow in config.yml is true
        itemFrame.isGlowing = plugin.glowEnabled
    }

    // This function is called when an item frame is broken.
    // It checks if the item frame is invisible and if so...
    // - cancels the default drop of the item frame.
    // - drops a custom invisible item frame instead of a regular one.
    // - also plays a sound effect for breaking the item frame.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInvisFrameBreak(event: HangingBreakEvent) {
        val entity = event.entity
        val itemFrame = getInvisItemFrame(entity) ?: return
        if (!event.isCancelled) {
            event.isCancelled = true
            itemFrame.scheduler.run(plugin, Consumer { _: ScheduledTask ->
                val isGlowItemFrame = entity.type == EntityType.GLOW_ITEM_FRAME
                val material = if (isGlowItemFrame) Material.GLOW_ITEM_FRAME else Material.ITEM_FRAME
                val nameKey = if (isGlowItemFrame) "invisible_glow_item_frame" else "invisible_item_frame"
                val drop = ItemStack(material, 1)
                val meta = drop.itemMeta
                meta.displayName(plugin.messageUtil.formatName(nameKey))
                meta.persistentDataContainer.set(plugin.invisItemFrameKey, PersistentDataType.BYTE, 1)
                drop.itemMeta = meta
                itemFrame.world.playSound(itemFrame.location, "entity.item_frame.break", 1.0f, 1.0f)
                itemFrame.world.dropItemNaturally(itemFrame.location, drop)
                itemFrame.remove()
            }, null)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInvisFrameAddItem(event: PlayerInteractEntityEvent) {
        handleInvisFrameItemChange(event.rightClicked)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInvisFrameRemoveItem(event: EntityDamageByEntityEvent) {
        handleInvisFrameItemChange(event.entity)
    }
}
