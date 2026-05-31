package arch.zero.bestSleep

import org.bukkit.entity.Player
class FloodgateFormBridge(private val plugin: BestSleep) {
    private val floodgateApiClass = runCatching { Class.forName("org.geysermc.floodgate.api.FloodgateApi") }.getOrNull()
    private val modalFormClass = runCatching { Class.forName("org.geysermc.cumulus.form.ModalForm") }.getOrNull()

    val available: Boolean = floodgateApiClass != null && modalFormClass != null

    fun isBedrockPlayer(player: Player): Boolean {
        if (!available) return false
        return runCatching {
            val instance = floodgateApiClass!!.getMethod("getInstance").invoke(null)
            floodgateApiClass.getMethod("isFloodgatePlayer", java.util.UUID::class.java).invoke(instance, player.uniqueId) as Boolean
        }.getOrDefault(false)
    }

    fun openConfirmation(
        player: Player,
        title: String,
        content: String,
        onAccept: () -> Unit,
        onDeny: () -> Unit
    ): Boolean {
        if (!available) return false

        return runCatching {
            val api = floodgateApiClass!!.getMethod("getInstance").invoke(null)
            val builder = modalFormClass!!.getMethod("builder").invoke(null)
            builder.javaClass.getMethod("title", String::class.java).invoke(builder, title)
            builder.javaClass.getMethod("content", String::class.java).invoke(builder, content)
            builder.javaClass.getMethod("button1", String::class.java).invoke(builder, plugin.i18n.raw(player, "ui.accept"))
            builder.javaClass.getMethod("button2", String::class.java).invoke(builder, plugin.i18n.raw(player, "ui.deny"))

            val consumerClass = Class.forName("java.util.function.Consumer")
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                plugin.javaClass.classLoader,
                arrayOf(consumerClass)
            ) { _, method, args ->
                if (method.name == "accept") {
                    val response = args?.firstOrNull()
                    val clickedFirst = response?.javaClass?.methods?.firstOrNull { it.name == "clickedFirst" }?.invoke(response) as? Boolean
                    if (clickedFirst == true) onAccept() else onDeny()
                }
                null
            }

            val handlerMethod = builder.javaClass.methods.firstOrNull {
                it.name == "validResultHandler" && it.parameterTypes.size == 1
            } ?: return@runCatching false
            handlerMethod.invoke(builder, proxy)

            val form = builder.javaClass.getMethod("build").invoke(builder)
            val sendForm = floodgateApiClass.getMethod("sendForm", java.util.UUID::class.java, Class.forName("org.geysermc.cumulus.form.Form"))
            sendForm.invoke(api, player.uniqueId, form)
            true
        }.getOrElse {
            plugin.logger.warning("Failed to open Floodgate form: ${it.message}")
            false
        }
    }
}
