package arch.zero.bestSleep

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.Locale

class I18n(private val plugin: JavaPlugin) {
    private val defaultLocale = plugin.config
        .getString("default-locale", "zh_cn")
        ?.replace('-', '_')
        ?.lowercase(Locale.ROOT)
        ?: "zh_cn"
    private val bundles: Map<String, Map<String, String>> by lazy {
        val root = plugin.config.getConfigurationSection("messages") ?: return@lazy emptyMap()
        root.getKeys(false).associateWith { locale ->
            flatten(root.getConfigurationSection(locale))
        }
    }

    fun text(sender: CommandSender?, key: String, color: NamedTextColor, vararg args: Any): Component {
        val locale = localeOf(sender)
        val template = bundles[locale]?.get(key)
            ?: bundles[defaultLocale]?.get(key)
            ?: key
        val rendered = template.format(*args)
        return Component.text(rendered, color).decoration(TextDecoration.ITALIC, false)
    }

    fun raw(sender: CommandSender?, key: String, vararg args: Any): String {
        val locale = localeOf(sender)
        val template = bundles[locale]?.get(key)
            ?: bundles[defaultLocale]?.get(key)
            ?: key
        return template.format(*args)
    }

    private fun localeOf(sender: CommandSender?): String {
        if (sender is Player) {
            return sender.locale().toLanguageTag().replace('-', '_').lowercase(Locale.ROOT)
        }
        return defaultLocale
    }

    private fun flatten(section: ConfigurationSection?, prefix: String = ""): Map<String, String> {
        if (section == null) return emptyMap()
        val values = mutableMapOf<String, String>()
        for (key in section.getKeys(false)) {
            val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
            val value = section.get(key)
            when (value) {
                is ConfigurationSection -> values.putAll(flatten(value, fullKey))
                is String -> values[fullKey] = value
            }
        }
        return values
    }
}
