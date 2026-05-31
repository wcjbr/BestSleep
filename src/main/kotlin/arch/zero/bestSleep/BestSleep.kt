package arch.zero.bestSleep

import org.bukkit.plugin.java.JavaPlugin

class BestSleep : JavaPlugin() {

    lateinit var sleepManager: SleepRequestManager
        private set
    lateinit var i18n: I18n
        private set
    private lateinit var commandHandler: BestSleepCommand

    override fun onEnable() {
        saveDefaultConfig()
        i18n = I18n(this)
        sleepManager = SleepRequestManager(this)
        commandHandler = BestSleepCommand(this)
        getCommand("bestsleep")?.setExecutor(commandHandler)
        getCommand("bestsleep")?.tabCompleter = commandHandler
        server.pluginManager.registerEvents(SleepListener(this), this)
        logger.info("BestSleep enabled")
    }

    override fun onDisable() {
        sleepManager.shutdown()
        logger.info("BestSleep disabled")
    }
}
