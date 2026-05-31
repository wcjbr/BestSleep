package arch.zero.bestSleep

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.title.Title
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Bed
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBedLeaveEvent
import org.bukkit.scheduler.BukkitTask
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

class SleepRequestManager(private val plugin: BestSleep) {
    private val floodgateBridge = FloodgateFormBridge(plugin)
    private val i18n = plugin.i18n
    private var activeRequest: SleepRequest? = null
    private val occupiedBeds = ConcurrentHashMap.newKeySet<String>()
    private val requestCooldowns = ConcurrentHashMap<UUID, Long>()

    fun shutdown() {
        activeRequest?.cancelTasks()
        activeRequest = null
        occupiedBeds.clear()
    }

    fun startRequest(requester: Player) {
        if (activeRequest != null) {
            requester.sendMessage(i18n.text(requester, "sleep.request_active", NamedTextColor.RED))
            return
        }

        val cooldownSeconds = plugin.config.getLong("request-cooldown-seconds", 60L).coerceAtLeast(0L)
        val now = System.currentTimeMillis()
        val nextAllowed = requestCooldowns[requester.uniqueId] ?: 0L
        if (cooldownSeconds > 0 && now < nextAllowed) {
            val remaining = ((nextAllowed - now) / 1000L).coerceAtLeast(1L)
            requester.sendMessage(i18n.text(requester, "sleep.cooldown", NamedTextColor.RED, remaining))
            return
        }

        if (requester.world.isDayTime) {
            requester.sendMessage(i18n.text(requester, "sleep.night_only", NamedTextColor.RED))
            return
        }

        val online = Bukkit.getOnlinePlayers()
            .filter { it.isOnline && !it.isDead }
            .filter { it.world == requester.world }
            .sortedBy { it.name.lowercase() }

        if (online.size < 2) {
            requester.sendMessage(i18n.text(requester, "sleep.need_two_players", NamedTextColor.RED))
            return
        }

        val noBedPlayers = online.filter { !hasUsableRespawn(it) }
        if (noBedPlayers.isNotEmpty()) {
            requester.sendMessage(
                i18n.text(
                    requester,
                    "sleep.missing_beds",
                    NamedTextColor.RED,
                    noBedPlayers.joinToString(", ") { it.name }
                )
            )
            return
        }

        val participants = online.map { it.uniqueId }.toSet()
        val approvals = mutableSetOf(requester.uniqueId)
        val timeoutTask = plugin.server.scheduler.runTaskLater(plugin, Runnable {
            val current = activeRequest ?: return@Runnable
            cancelInternal(current, "sleep.request_timeout")
        }, 20L * 60L)

        val request = SleepRequest(
            requester = requester.uniqueId,
            world = requester.world,
            participants = participants,
            approvals = approvals,
            timeoutTask = timeoutTask,
            titleTask = null,
            advanceTask = null,
            timeAdvancePerTick = 1L,
            sleepingPlayers = mutableSetOf()
        )
        activeRequest = request
        if (cooldownSeconds > 0) {
            requestCooldowns[requester.uniqueId] = now + cooldownSeconds * 1000L
        }
        request.titleTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val current = activeRequest ?: return@Runnable
            if (current !== request) return@Runnable
            showWorldTimeTitle(current)
        }, 0L, 20L)

        val displayNames = online.joinToString(", ") { it.name }
        Bukkit.getOnlinePlayers().forEach { player ->
            player.sendMessage(i18n.text(player, "sleep.request_started_full", NamedTextColor.GOLD, requester.name))
            player.sendMessage(i18n.text(player, "sleep.participants", NamedTextColor.GRAY, displayNames))
            player.sendMessage(progressLine(request, player))
        }
        plugin.server.consoleSender.sendMessage(i18n.raw(null, "sleep.request_started_full", requester.name))
        plugin.server.consoleSender.sendMessage(i18n.raw(null, "sleep.participants", displayNames))
        plugin.server.consoleSender.sendMessage(
            i18n.raw(null, "sleep.progress", request.approvals.size, request.participants.size, requiredApprovals(request))
        )
        showWorldTimeTitle(request)

        online.forEach { player ->
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.2f)
            if (player.uniqueId == requester.uniqueId) {
                player.sendMessage(progressLine(request, player))
            } else {
                openConfirmationUi(request, player, requester)
            }
        }

        checkCompletion()
    }

    fun respond(player: Player, accepted: Boolean) {
        val request = activeRequest ?: run {
            player.sendMessage(i18n.text(player, "sleep.no_request", NamedTextColor.RED))
            return
        }
        if (player.uniqueId !in request.participants) {
            player.sendMessage(i18n.text(player, "sleep.not_participant", NamedTextColor.RED))
            return
        }
        if (accepted) {
            if (!request.approvals.add(player.uniqueId)) {
                player.sendMessage(i18n.text(player, "sleep.already_accepted", NamedTextColor.YELLOW))
                return
            }
            broadcastLocalized("sleep.player_accepted", NamedTextColor.GREEN, player.name)
            broadcastProgress(request)
            showWorldTimeTitle(request)
            if (request.phase == SleepPhase.COLLECTING) {
                checkCompletion()
            } else {
                updateAdvanceSpeed(request)
                trySleepPlayer(request, player)
            }
            return
        }

        if (request.phase == SleepPhase.ADVANCING) {
            player.sendMessage(i18n.text(player, "sleep.already_advancing", NamedTextColor.RED))
            return
        }

        if (request.approvals.remove(player.uniqueId)) {
            broadcastLocalized("sleep.player_unaccepted", NamedTextColor.YELLOW, player.name)
            broadcastProgress(request)
            showWorldTimeTitle(request)
        } else {
            player.sendMessage(i18n.text(player, "sleep.still_not_accepted", NamedTextColor.YELLOW))
        }
    }

    fun cancelBy(player: Player) {
        val request = activeRequest ?: run {
            player.sendMessage(i18n.text(player, "sleep.no_request", NamedTextColor.RED))
            return
        }
        if (request.requester != player.uniqueId) {
            player.sendMessage(i18n.text(player, "sleep.only_requester_cancel", NamedTextColor.RED))
            return
        }
        cancelInternal(request, "sleep.request_cancelled", player.name)
    }

    fun handleQuit(player: Player) {
        val request = activeRequest ?: return
        if (player.uniqueId !in request.participants) return
        cancelInternal(request, "sleep.request_quit_cancelled", player.name)
    }

    fun handleBedEnter(event: PlayerBedEnterEvent) {
        if (event.useBed() == Event.Result.DENY || event.isCancelled) {
            return
        }
        occupiedBeds.add(bedKey(event.bed))
    }

    fun handleBedLeave(event: PlayerBedLeaveEvent) {
        occupiedBeds.remove(bedKey(event.bed))
    }

    private fun openConfirmationUi(request: SleepRequest, target: Player, requester: Player) {
        val title = i18n.raw(target, "sleep.dialog_title")
        val content = i18n.raw(target, "sleep.dialog_content", requester.name)

        if (floodgateBridge.isBedrockPlayer(target)) {
            val opened = floodgateBridge.openConfirmation(
                target,
                title,
                content,
                onAccept = { respond(target, true) },
                onDeny = { respond(target, false) }
            )
            if (opened) {
                target.sendMessage(progressLine(request, target))
                return
            }
        }

        target.showDialog(buildJavaDialog(target, requester))
        target.sendMessage(progressLine(request, target))
        target.sendMessage(i18n.text(target, "sleep.dialog_fallback", NamedTextColor.GRAY))
    }

    private fun buildJavaDialog(viewer: Player, requester: Player): Dialog {
        val yesButton = ActionButton.builder(i18n.text(viewer, "ui.accept", NamedTextColor.GREEN))
            .tooltip(i18n.text(viewer, "sleep.ui_accept_tooltip", NamedTextColor.GRAY))
            .width(150)
            .action(
                DialogAction.customClick(
                    { _, audience -> if (audience is Player) respond(audience, true) },
                    ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(2)).build()
                )
            )
            .build()

        val noButton = ActionButton.builder(i18n.text(viewer, "ui.deny", NamedTextColor.RED))
            .tooltip(i18n.text(viewer, "sleep.ui_deny_tooltip", NamedTextColor.GRAY))
            .width(150)
            .action(
                DialogAction.customClick(
                    { _, audience -> if (audience is Player) respond(audience, false) },
                    ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(2)).build()
                )
            )
            .build()

        return Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(i18n.text(viewer, "sleep.dialog_title", NamedTextColor.GOLD))
                        .externalTitle(text("BestSleep", NamedTextColor.YELLOW))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(
                            listOf(
                                DialogBody.plainMessage(
                                    i18n.text(viewer, "sleep.dialog_body_line1", NamedTextColor.WHITE, requester.name)
                                        .appendNewline()
                                        .append(i18n.text(viewer, "sleep.dialog_body_line2", NamedTextColor.GRAY)),
                                    260
                                )
                            )
                        )
                        .inputs(emptyList())
                        .build()
                )
                .type(DialogType.confirmation(yesButton, noButton))
        }
    }

    private fun checkCompletion() {
        val request = activeRequest ?: return
        val required = requiredApprovals(request)
        if (request.approvals.size < required) return

        startAdvancingToDay(request)
    }

    private fun startAdvancingToDay(request: SleepRequest) {
        if (request.phase == SleepPhase.ADVANCING) return
        request.phase = SleepPhase.ADVANCING
        request.timeoutTask.cancel()
        broadcastLocalized("sleep.threshold_reached", NamedTextColor.GREEN)
        updateAdvanceSpeed(request)
        showWorldTimeTitle(request)
        request.approvals.mapNotNull(Bukkit::getPlayer).forEach { player -> trySleepPlayer(request, player) }
        request.advanceTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val current = activeRequest ?: return@Runnable
            if (current !== request) return@Runnable
            if (request.world.isDayTime) {
                finishRequest(request, "sleep.finished_day", NamedTextColor.GREEN)
                return@Runnable
            }
            request.world.time = (request.world.time + request.timeAdvancePerTick) % 24000
        }, 1L, 1L)
    }

    private fun trySleepPlayer(request: SleepRequest, player: Player) {
        if (player.uniqueId in request.sleepingPlayers) return
        val bedBlock = resolveBedBlock(player)
        if (bedBlock == null) {
            player.sendMessage(i18n.text(player, "sleep.bed_not_found", NamedTextColor.RED))
            return
        }
        if (isBedOccupied(bedBlock)) {
            player.sendMessage(i18n.text(player, "sleep.bed_occupied", NamedTextColor.RED))
            return
        }

        val destination = sleepEntryLocation(bedBlock)
        val teleported = player.teleport(destination)
        if (!teleported) {
            player.sendMessage(i18n.text(player, "sleep.teleport_failed", NamedTextColor.RED))
            return
        }

        plugin.server.scheduler.runTask(plugin, Runnable {
            val current = activeRequest ?: return@Runnable
            if (current !== request) return@Runnable
            if (isBedOccupied(bedBlock)) {
                player.sendMessage(i18n.text(player, "sleep.bed_occupied_before_sleep", NamedTextColor.RED))
                return@Runnable
            }
            val slept = player.sleep(bedBlock.location, true)
            if (!slept) {
                player.sendMessage(i18n.text(player, "sleep.sleep_failed", NamedTextColor.YELLOW))
            } else {
                occupiedBeds.add(bedKey(bedBlock))
                request.sleepingPlayers.add(player.uniqueId)
                updateAdvanceSpeed(request)
                player.sendMessage(i18n.text(player, "sleep.sleep_success", NamedTextColor.GREEN))
            }
        })
    }

    private fun updateAdvanceSpeed(request: SleepRequest) {
        val required = requiredApprovals(request)
        val activeSleepers = request.approvals.size.coerceAtLeast(1)
        val durationTicks = (400.0 * required / activeSleepers).coerceAtLeast(1.0)
        val remainingTicks = ticksUntilDay(request.world.time).coerceAtLeast(1L)
        request.timeAdvancePerTick = kotlin.math.ceil(remainingTicks / durationTicks).toLong().coerceAtLeast(1L)
    }

    private fun ticksUntilDay(worldTime: Long): Long {
        val normalized = ((worldTime % 24000) + 24000) % 24000
        return if (normalized < 12300) 0L else 24000L - normalized
    }

    private fun finishRequest(request: SleepRequest, reasonKey: String, color: NamedTextColor, vararg args: Any) {
        if (activeRequest === request) {
            activeRequest = null
        }
        request.cancelTasks()
        broadcastLocalized(reasonKey, color, *args)
        request.participants.mapNotNull(Bukkit::getPlayer).forEach { it.resetTitle() }
    }

    private fun cancelInternal(request: SleepRequest, reasonKey: String, vararg args: Any) {
        finishRequest(request, reasonKey, NamedTextColor.GREEN, *args)
    }

    private fun hasUsableRespawn(player: Player): Boolean = resolveBedBlock(player) != null

    private fun isBedOccupied(bedBlock: Block): Boolean {
        val bedData = bedBlock.blockData as? Bed ?: return false
        return bedData.isOccupied || occupiedBeds.contains(bedKey(bedBlock))
    }

    private fun bedKey(bedBlock: Block): String {
        val loc = bedBlock.location
        val worldName = loc.world?.name ?: "unknown"
        return "$worldName:${loc.blockX}:${loc.blockY}:${loc.blockZ}"
    }

    private fun resolveBedBlock(player: Player): Block? {
        val respawn = player.respawnLocation ?: return null
        val world = respawn.world ?: return null
        var fallback: Block? = null

        for (y in -1..1) {
            for (x in -1..1) {
                for (z in -1..1) {
                    val block = world.getBlockAt(
                        respawn.blockX + x,
                        respawn.blockY + y,
                        respawn.blockZ + z
                    )
                    if (Tag.BEDS.isTagged(block.type)) {
                        val bed = block.blockData as? Bed ?: return block
                        if (bed.part == Bed.Part.HEAD) {
                            return block
                        }
                        if (fallback == null) {
                            fallback = block
                        }
                    }
                }
            }
        }
        return fallback
    }

    private fun sleepEntryLocation(bedBlock: Block): Location {
        val bedData = bedBlock.blockData as? Bed
        val facing = bedData?.facing ?: BlockFace.NORTH
        val target = bedBlock.location.clone().add(0.5, 0.6, 0.5)
        target.yaw = when (facing) {
            BlockFace.NORTH -> 180f
            BlockFace.SOUTH -> 0f
            BlockFace.WEST -> 90f
            BlockFace.EAST -> -90f
            else -> 0f
        }
        return target
    }

    private fun requiredApprovals(request: SleepRequest): Int {
        val playerCount = request.participants.size
        val percentage = request.world.getGameRuleValue(GameRule.PLAYERS_SLEEPING_PERCENTAGE) ?: 100
        if (percentage <= 0) return 1
        return ((playerCount * percentage) / 100.0).let { kotlin.math.ceil(it).toInt() }.coerceIn(1, playerCount)
    }

    private fun progressLine(request: SleepRequest, viewer: Player?): Component {
        val required = requiredApprovals(request)
        return i18n.text(
            viewer,
            "sleep.progress",
            NamedTextColor.AQUA,
            request.approvals.size,
            request.participants.size,
            required
        )
    }

    private fun showWorldTimeTitle(request: SleepRequest) {
        val mcTime = minecraftTime(request.world.time)
        request.participants.mapNotNull(Bukkit::getPlayer).forEach { player ->
            val subtitle = when (request.phase) {
                SleepPhase.COLLECTING -> i18n.text(
                    player,
                    "sleep.title_collecting",
                    NamedTextColor.YELLOW,
                    request.approvals.size,
                    requiredApprovals(request)
                )
                SleepPhase.ADVANCING -> i18n.text(player, "sleep.title_advancing", NamedTextColor.YELLOW)
            }
            val title = Title.title(
                i18n.text(player, "sleep.title_time", NamedTextColor.GOLD, mcTime),
                subtitle,
                Title.Times.times(
                    Duration.ofMillis(200),
                    Duration.of(2, ChronoUnit.SECONDS),
                    Duration.ofMillis(400)
                )
            )
            player.showTitle(title)
        }
    }

    private fun minecraftTime(worldTime: Long): String {
        val ticks = ((worldTime + 6000) % 24000).toInt()
        val hours = ticks / 1000
        val minutes = ((ticks % 1000) * 60) / 1000
        return "%02d:%02d".format(hours, minutes)
    }

    private fun text(content: String, color: NamedTextColor): Component {
        return Component.text(content, color).decoration(TextDecoration.ITALIC, false)
    }

    private fun broadcastLocalized(key: String, color: NamedTextColor, vararg args: Any) {
        Bukkit.getOnlinePlayers().forEach { player ->
            player.sendMessage(i18n.text(player, key, color, *args))
        }
        plugin.server.consoleSender.sendMessage(i18n.raw(null, key, *args))
    }

    private fun broadcastProgress(request: SleepRequest) {
        Bukkit.getOnlinePlayers().forEach { player ->
            player.sendMessage(progressLine(request, player))
        }
        plugin.server.consoleSender.sendMessage(
            i18n.raw(null, "sleep.progress", request.approvals.size, request.participants.size, requiredApprovals(request))
        )
    }

    private data class SleepRequest(
        val requester: UUID,
        val world: org.bukkit.World,
        val participants: Set<UUID>,
        val approvals: MutableSet<UUID>,
        val timeoutTask: BukkitTask,
        var titleTask: BukkitTask?,
        var advanceTask: BukkitTask?,
        var timeAdvancePerTick: Long,
        val sleepingPlayers: MutableSet<UUID>,
        var phase: SleepPhase = SleepPhase.COLLECTING
    ) {
        fun cancelTasks() {
            timeoutTask.cancel()
            titleTask?.cancel()
            advanceTask?.cancel()
        }
    }

    private enum class SleepPhase {
        COLLECTING,
        ADVANCING
    }
}
