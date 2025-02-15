package de.jutechs.terminator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener, CommandExecutor {
    private static final Component BOW_NAME = Component.text("Terminator", NamedTextColor.RED, TextDecoration.BOLD);
    private static final NamespacedKey TERMINATOR_KEY = new NamespacedKey("terminator", "bow");
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 250; // 0.25 seconds in milliseconds

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("awardterminator").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("awardterminator")) {
            if (sender instanceof Player player) {
                if (player.hasPermission("terminator.award")) {
                    player.getInventory().addItem(createTerminatorBow());
                    player.sendMessage(Component.text("You have been given the Terminator bow!", NamedTextColor.GREEN));
                }
            }
            return true;
        }
        return false;
    }

    private ItemStack createTerminatorBow() {
        ItemStack terminatorBow = new ItemStack(Material.BOW);
        ItemMeta meta = terminatorBow.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Terminator", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("Fires three arrows instantly!", NamedTextColor.GRAY),
                    Component.text("Only damages non-player entities.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Shortbow: Instantly shoots!", NamedTextColor.GOLD),
                    Component.empty(),
                    Component.text("LEGENDARY BOW", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false)
            ));
            meta.getPersistentDataContainer().set(TERMINATOR_KEY, PersistentDataType.BYTE, (byte) 1);
            terminatorBow.setItemMeta(meta);
        }
        return terminatorBow;
    }

    private boolean isTerminator(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(TERMINATOR_KEY, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (isTerminator(item)) {
            event.setCancelled(true);
            if (isOnCooldown(player)) {
                //player.sendMessage(Component.text("You must wait before shooting again!", NamedTextColor.RED));
                return;
            }
            shootTripleArrows(player);
            setCooldown(player);
            player.setNoDamageTicks(0);
        }
    }

    private void shootTripleArrows(Player player) {
        Location loc = player.getEyeLocation();
        Vector direction = loc.getDirection().normalize();

        for (int i = -1; i <= 1; i++) {
            Arrow arrow = player.getWorld().spawn(loc, Arrow.class);
            Vector spread = direction.clone();
            if (i == -1) {
                spread.rotateAroundY(Math.toRadians(-5)); // Left arrow
            } else if (i == 1) {
                spread.rotateAroundY(Math.toRadians(5)); // Right arrow
            }
            arrow.setVelocity(spread.multiply(2));
            arrow.setShooter(player);
            arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        }
    }

    private boolean isOnCooldown(Player player) {
        return cooldowns.containsKey(player.getUniqueId()) && cooldowns.get(player.getUniqueId()) > System.currentTimeMillis();
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + COOLDOWN_TIME);
        new BukkitRunnable() {
            @Override
            public void run() {
                cooldowns.remove(player.getUniqueId());
            }
        }.runTaskLater(this, COOLDOWN_TIME / 50); // Convert ms to ticks
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Arrow arrow && arrow.getShooter() instanceof Player) {
            if (event.getEntity().getType() == EntityType.PLAYER) {
                event.setCancelled(true);
            }
        }
    }
}
