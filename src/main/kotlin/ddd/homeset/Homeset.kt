package ddd.homeset

import org.bukkit.*
import org.bukkit.command.*
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class HomeTeleport : JavaPlugin() {
    private val homes = mutableMapOf<String, MutableMap<String, Location>>()
    private val file by lazy { File(dataFolder, "homes.yml") }

    override fun onEnable() {
        loadHomes()
        logger.info("HomeTeleport Enabled")
    }

    override fun onDisable() = saveHomes()

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return false
        val uuid = sender.uniqueId.toString()

        when (cmd.name.lowercase()) {
            "homeset" -> {
                val name = args.getOrNull(0) ?: return message(sender, "§c장소 이름을 입력하세요.")
                homes.getOrPut(uuid) { mutableMapOf() }[name] = sender.location
                saveHomes()
                message(sender, "§a홈 '$name' 저장 완료!")
            }
            "home" -> {
                val name = args.getOrNull(0) ?: return message(sender, "§c/home <이름>")
                val loc = homes[uuid]?[name] ?: return message(sender, "§c저장된 장소가 없습니다.")

                if (!sender.inventory.contains(Material.EMERALD)) {
                    giveBarrier(sender)
                    return message(sender, "§c에메랄드가 필요합니다.")
                }

                sender.inventory.removeItem(ItemStack(Material.EMERALD, 1))
                sender.teleport(loc)
                message(sender, "§a'$name'(으)로 이동했습니다.")
            }
        }
        return true
    }

    private fun message(p: Player, msg: String): Boolean {
        p.sendMessage(msg)
        return true
    }

    private fun giveBarrier(p: Player) {
        val item = ItemStack(Material.BARRIER).apply {
            itemMeta = itemMeta?.apply { setDisplayName("§c에메랄드 없음") }
        }
        p.inventory.addItem(item)
    }

    private fun saveHomes() {
        val config = YamlConfiguration()
        homes.forEach { (uuid, map) -> map.forEach { (name, loc) -> config.set("$uuid.$name", loc) } }
        config.save(file)
    }

    private fun loadHomes() {
        if (!file.exists()) return
        val config = YamlConfiguration.loadConfiguration(file)
        config.getKeys(false).forEach { uuid ->
            val section = config.getConfigurationSection(uuid) ?: return@forEach
            val map = mutableMapOf<String, Location>()
            section.getKeys(false).forEach { name ->
                (section.get(name) as? Location)?.let { map[name] = it }
            }
            homes[uuid] = map
        }
    }
}
