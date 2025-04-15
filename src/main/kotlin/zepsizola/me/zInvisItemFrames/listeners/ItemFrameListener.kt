package zepsizola.me.zInvisItemFrames.listeners

import java.util.function.Consumer
import org.bukkit.Location
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.util.Vector
import org.bukkit.block.BlockFace
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent.ItemFrameChangeAction
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
        if (entity.type != EntityType.ITEM_FRAME && entity.type != EntityType.GLOW_ITEM_FRAME) return null
        if (!(entity as ItemFrame).persistentDataContainer.has(plugin.invisItemFrameKey, PersistentDataType.BYTE)) return null
        return entity
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        if (!plugin.checkPermCraft) return
        val result = event.inventory.result ?: return
        if (!itemIsInvisFrame(result)) return
        val player = event.view.player as? Player ?: return
        val permission = if (result.type == Material.GLOW_ITEM_FRAME) "zinvisitemframes.craft.glow_item_frame" else "zinvisitemframes.craft.item_frame"
        if (!player.hasPermission(permission)) {
            event.inventory.result = null
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInvisFramePlace(event: HangingPlaceEvent) {
        val player = event.player ?: return
        val entity = event.entity
        if (entity.type != EntityType.ITEM_FRAME && entity.type != EntityType.GLOW_ITEM_FRAME) return
        // Checks if...
        // - The item in the main hand is an invisible item frame.
        // - The item in the off hand is an invisible item frame and the main hand is not an item frame.
        if (!itemIsInvisFrame(player.inventory.itemInMainHand)) {
            if (!(itemIsInvisFrame(player.inventory.itemInOffHand) && (player.inventory.itemInMainHand.type != Material.ITEM_FRAME && player.inventory.itemInMainHand.type != Material.GLOW_ITEM_FRAME))) {
                return
            }
        }
        val permission = if (entity.type == EntityType.GLOW_ITEM_FRAME) "zinvisitemframes.place.glow_item_frame" else "zinvisitemframes.place.item_frame"
        val nameKey = if (entity.type == EntityType.GLOW_ITEM_FRAME) "invisible_glow_item_frame" else "invisible_item_frame"
        if (!player.hasPermission(permission) && plugin.checkPermPlace) {
            event.isCancelled = true
            plugin.messageUtil.sendMessage(player, "error.no-place", "item_frame_type" to (plugin.config.getString("name.$nameKey") ?: nameKey))
            return
        }
        val itemFrame = entity as ItemFrame
        // Sets metadata for the item frame to indicate that it is an invisible item frame.
        itemFrame.persistentDataContainer.set(plugin.invisItemFrameKey, PersistentDataType.BYTE, 1)
        // Set the placed item frame to be visible.
        itemFrame.isVisible = plugin.visibleEmpty
        // Item frame only glows if empty-frame.glow in config.yml is true
        itemFrame.isGlowing = plugin.glowEmpty
    }

    // This function is called when an item frame is broken.
    // It checks if the item frame is invisible and if so...
    // - cancels the default drop of the item frame.
    // - drops a custom invisible item frame instead of a regular one.
    // - also plays a sound effect for breaking the item frame.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onInvisFrameBreak(event: HangingBreakEvent) {
        val entity = event.entity
        val itemFrame = getInvisItemFrame(entity) ?: return
        val player = (event as? HangingBreakByEntityEvent)?.remover as? Player
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
            itemFrame.remove()
            if (player?.gameMode == GameMode.CREATIVE) return@Consumer
            val vector = itemFrame.facing.direction.multiply(0.15) // Makes sure the item drops just a little bit away from the wall.
            itemFrame.world.dropItem(itemFrame.location.add(vector), drop)
        }, null)
    }

    // This function is called when a player interacts with an item frame.
    // It checks if the item frame is invisible and if so...
    // it sets the item frame to be visible and glowing if the action was REMOVE.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInvisFrameChange(event: PlayerItemFrameChangeEvent) {
        val itemFrame = event.itemFrame
        if (!itemFrame.persistentDataContainer.has(plugin.invisItemFrameKey, PersistentDataType.BYTE)) return
        itemFrame.scheduler.run(plugin, Consumer { _: ScheduledTask ->
            val isEmpty = event.getAction() == PlayerItemFrameChangeEvent.ItemFrameChangeAction.REMOVE
            itemFrame.isGlowing = isEmpty && plugin.glowEmpty
            itemFrame.isVisible = isEmpty && plugin.visibleEmpty
        }, null)
    }
}
