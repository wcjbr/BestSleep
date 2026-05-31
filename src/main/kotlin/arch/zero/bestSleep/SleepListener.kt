package arch.zero.bestSleep

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBedLeaveEvent
import org.bukkit.event.player.PlayerQuitEvent

class SleepListener(private val plugin: BestSleep) : Listener {

    @EventHandler
    fun onBedEnter(event: PlayerBedEnterEvent) {
        plugin.sleepManager.handleBedEnter(event)
    }

    @EventHandler
    fun onBedLeave(event: PlayerBedLeaveEvent) {
        plugin.sleepManager.handleBedLeave(event)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        plugin.sleepManager.handleQuit(event.player)
    }
}
