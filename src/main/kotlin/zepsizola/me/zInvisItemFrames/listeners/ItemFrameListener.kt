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
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
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

    // Returns true if the Material is an item frame (ITEM_FRAME or GLOW_ITEM_FRAME).
    fun Material.isItemFrame(): Boolean {
        return (this == Material.ITEM_FRAME || this == Material.GLOW_ITEM_FRAME)
    }

    // Returns true if the Material is a GLOW_ITEM_FRAME.
    fun Material.isGlowItemFrame(): Boolean {
        return (this == Material.GLOW_ITEM_FRAME)
    }

    // Returns tre if the entity is a GLOW_ITEM_FRAME.
    fun Entity.isGlowItemFrame(): Boolean {
        return this.type == EntityType.GLOW_ITEM_FRAME
    }

    // Returns true if the entity is an invisible item frame. Should be called on ItemFrame or Entity.
    fun PersistentDataHolder.hasInvisKey(): Boolean {
        try {
            return (this.persistentDataContainer.has(plugin.invisItemFrameKey, PersistentDataType.BYTE)) ?: false
        } catch (e: NullPointerException) {
            return false
        }
    }

    // Sets the ItemFrame to be an invisible item frame.
    fun PersistentDataHolder.setInvisKey() {
        this.persistentDataContainer.set(plugin.invisItemFrameKey, PersistentDataType.BYTE, 1)
    }

    // Returns the item frame if it is an invisible item frame, else returns null.
    fun Entity.getInvisItemFrame(): ItemFrame? {
        return if (this.hasInvisKey()) (this as? ItemFrame) else null ?: return null
    }

    // Returns true if the ItemStack represents an invisible item frame.
    fun createInvisItemFrameItem(isGlowItemFrame: Boolean = false): ItemStack {
        val material = if (isGlowItemFrame) Material.GLOW_ITEM_FRAME else Material.ITEM_FRAME
        val nameKey = if (isGlowItemFrame) "invisible_glow_item_frame" else "invisible_item_frame"
        val item = ItemStack(material, 1)
        val meta = item.itemMeta
        meta.displayName(plugin.messageUtil.formatName(nameKey))
        meta.setInvisKey()
        item.itemMeta = meta
        return item
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        if (!plugin.checkPermCraft) return
        val result = event.inventory.result ?: return
        if (!result.itemMeta.hasInvisKey()) return
        val player = event.view.player as? Player ?: return
        val permission = if (result.type == Material.GLOW_ITEM_FRAME) "zinvisitemframes.craft.glow_item_frame" else "zinvisitemframes.craft.item_frame"
        if (!player.hasPermission(permission)) {
            event.inventory.result = null
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInvisFramePlace(event: HangingPlaceEvent) {
        val player = event.player ?: return
        val itemFrame = (event.entity as ItemFrame)
        // Checks if...
        // - The item in the main hand is an invisible item frame.
        // - The item in the off hand is an invisible item frame and the main hand is an item frame.
        val mainHandInvis = player.inventory.itemInMainHand.itemMeta?.hasInvisKey() ?: false
        val offHandInvis = player.inventory.itemInOffHand.itemMeta?.hasInvisKey() ?: false
        if (!mainHandInvis || (offHandInvis && player.inventory.itemInMainHand.type.isItemFrame())) {
            return
        }
        val permission = if (itemFrame.isGlowItemFrame()) "zinvisitemframes.place.glow_item_frame" else "zinvisitemframes.place.item_frame"
        val nameKey = if (itemFrame.isGlowItemFrame()) "invisible_glow_item_frame" else "invisible_item_frame"
        if (!player.hasPermission(permission) && plugin.checkPermPlace) {
            event.isCancelled = true
            plugin.messageUtil.sendMessage(player, "error.no-place", "item_frame_type" to (plugin.config.getString("name.$nameKey") ?: nameKey))
            return
        }
        itemFrame.setInvisKey() // Sets metadata for the item frame to indicate that it is an invisible item frame.
        // Item frame is visible/glowing if empty-frame.glow and empty-frame.visible in config.yml are true
        itemFrame.isVisible = plugin.visibleEmpty
        itemFrame.isGlowing = plugin.glowEmpty
    }

    // This function is called when an item frame is broken.
    // It checks if the item frame is invisible and if so...
    // - cancels the default drop of the item frame.
    // - drops a custom invisible item frame instead of a regular one.
    // - also plays a sound effect for breaking the item frame.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onInvisFrameBreak(event: HangingBreakEvent) {
        val itemFrame = event.entity.getInvisItemFrame() ?: return
        val player = (event as? HangingBreakByEntityEvent)?.remover as? Player
        event.isCancelled = true
        itemFrame.scheduler.run(plugin, Consumer { _: ScheduledTask ->
            itemFrame.world.playSound(itemFrame.location, "entity.item_frame.break", 1.0f, 1.0f)
            itemFrame.remove()
            if (player?.gameMode == GameMode.CREATIVE) return@Consumer
            val drop = createInvisItemFrameItem(itemFrame.isGlowItemFrame())
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
        if (!itemFrame.hasInvisKey()) return
        itemFrame.scheduler.run(plugin, Consumer { _: ScheduledTask ->
            val isEmpty = event.getAction() == PlayerItemFrameChangeEvent.ItemFrameChangeAction.REMOVE
            itemFrame.isGlowing = isEmpty && plugin.glowEmpty
            itemFrame.isVisible = isEmpty && plugin.visibleEmpty
        }, null)
    }
}
