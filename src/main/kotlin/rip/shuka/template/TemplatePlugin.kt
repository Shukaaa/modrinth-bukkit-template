package rip.shuka.template

import org.bukkit.plugin.java.JavaPlugin

class TemplatePlugin : JavaPlugin() {
    override fun onEnable() {
        logger.info("${description.name} ${description.version} enabled")
        logger.info("Minecraft: ${server.version.substringAfter("MC: ").trimEnd(')')}")
        logger.info("Server: ${server.name}")
        logger.info("Runtime Java: ${Runtime.version().feature()}")
    }
}
