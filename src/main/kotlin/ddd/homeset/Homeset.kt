package ddd.homeset

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.io.File

class HomeTeleport : JavaPlugin() {

    private val homes = mutableMapOf<String, MutableMap<String, Location>>() 
    // playerUUID -> (homeName -> location)

    override fun onEnable() {
        loadHomes()
        logger.info("HomeTeleport Enabled")
    }

    override fun onDisable() {
        saveHomes()
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        if (sender !is Player) return false
        val player = sender

        when (command.name.lowercase()) {
            "homeset" -> {
                if (args.isEmpty()) {
                    player.sendMessage("§c저장할 장소이름을 정해주세요")
                    return true
                }

                val name = args[0]
                val uuid = player.uniqueId.toString()
                homes.putIfAbsent(uuid, mutableMapOf())
                homes[uuid]!![name] = player.location

                player.sendMessage("§a홈 '$name' 이(가) 저장되었습니다")
                saveHomes()
            }

            "home" -> {
                if (args.isEmpty()) {
                    player.sendMessage("§c/home <장소이름>")
                    return true
                }

                val name = args[0]
                val uuid = player.uniqueId.toString()
                val home = homes[uuid]?.get(name)

                if (home == null) {
                    player.sendMessage("§c저장된 장소가 없습니다.")
                    return true
                }

                
                if (!player.inventory.contains(Material.EMERALD, 1)) {
                    showNoEmeraldItem(player)
                    player.sendMessage("§c에메랄드가 없음")
                    return true
                }

               
                removeEmerald(player)

                
                player.teleport(home)
                player.sendMessage("§a홈 '$name' 으로 이동했습니다")
            }
        }
        return true
    }

    private fun removeEmerald(player: Player) {
        val inv = player.inventory
        for (i in 0 until inv.size) {
            val item = inv.getItem(i) ?: continue
            if (item.type == Material.EMERALD) {
                item.amount -= 1
                if (item.amount <= 0) inv.setItem(i, null)
                break
            }
        }
    }

    private fun showNoEmeraldItem(player: Player) {
        val item = ItemStack(Material.BARRIER)
        val meta: ItemMeta = item.itemMeta!!
        meta.setDisplayName("§c에메랄드가 없음")
        item.itemMeta = meta

        player.inventory.addItem(item)
    }

   

    private fun getHomesFile(): File {
        val file = File(dataFolder, "homes.yml")
        if (!file.exists()) {
            dataFolder.mkdirs()
            file.createNewFile()
        }
        return file
    }

    private fun saveHomes() {
        val config = YamlConfiguration()
        for ((uuid, map) in homes) {
            for ((name, loc) in map) {
                val path = "$uuid.$name"
                config.set("$path.world", loc.world!!.name)
                config.set("$path.x", loc.x)
                config.set("$path.y", loc.y)
                config.set("$path.z", loc.z)
                config.set("$path.yaw", loc.yaw)
                config.set("$path.pitch", loc.pitch)
            }
        }
        config.save(getHomesFile())
    }

    private fun loadHomes() {
        val file = getHomesFile()
        val config = YamlConfiguration.loadConfiguration(file)

        for (uuid in config.getKeys(false)) {
            val map = mutableMapOf<String, Location>()
            val section = config.getConfigurationSection(uuid) ?: continue

            for (name in section.getKeys(false)) {
                val w = Bukkit.getWorld(section.getString("$name.world")!!) ?: continue
                val x = section.getDouble("$name.x")
                val y = section.getDouble("$name.y")
                val z = section.getDouble("$name.z")
                val yaw = section.getDouble("$name.yaw").toFloat()
                val pitch = section.getDouble("$name.pitch").toFloat()

                map[name] = Location(w, x, y, z, yaw, pitch)
            }
            homes[uuid] = map
        }
    }
}
