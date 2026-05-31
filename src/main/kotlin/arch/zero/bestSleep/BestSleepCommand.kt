package arch.zero.bestSleep

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class BestSleepCommand(private val plugin: BestSleep) : CommandExecutor, TabCompleter {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage(plugin.i18n.raw(sender, "command.players_only"))
            return true
        }

        if (args.isEmpty()) {
            plugin.sleepManager.startRequest(sender)
            return true
        }

        when (args[0].lowercase()) {
            "request", "start" -> plugin.sleepManager.startRequest(sender)
            "accept", "yes" -> plugin.sleepManager.respond(sender, true)
            "deny", "no", "reject" -> plugin.sleepManager.respond(sender, false)
            "cancel" -> plugin.sleepManager.cancelBy(sender)
            else -> sender.sendMessage(plugin.i18n.raw(sender, "command.usage", label))
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size != 1) return emptyList()
        val options = listOf("request", "accept", "deny", "cancel")
        return options.filter { it.startsWith(args[0].lowercase()) }
    }
}
