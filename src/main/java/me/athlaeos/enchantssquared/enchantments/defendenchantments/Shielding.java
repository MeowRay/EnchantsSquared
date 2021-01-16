package me.athlaeos.enchantssquared.enchantments.defendenchantments;

import me.athlaeos.enchantssquared.configs.ConfigManager;
import me.athlaeos.enchantssquared.dom.CustomEnchantClassification;
import me.athlaeos.enchantssquared.dom.CustomEnchantEnum;
import me.athlaeos.enchantssquared.dom.MaterialClassType;
import me.athlaeos.enchantssquared.hooks.WorldguardHook;
import me.athlaeos.enchantssquared.managers.CustomEnchantManager;
import me.athlaeos.enchantssquared.managers.ItemMaterialManager;
import me.athlaeos.enchantssquared.managers.RandomNumberGenerator;
import me.athlaeos.enchantssquared.utils.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class Shielding extends DefendEnchantment {
    private double deflect_chance_lv;
    private String message;
    private CustomEnchantManager manager;

    public Shielding(){
        this.enchantType = CustomEnchantEnum.SHIELDING;
        this.config = ConfigManager.getInstance().getConfig("config.yml").get();
        this.requiredPermission = "es.enchant.shielding";
        loadConfig();
    }

    @Override
    public void execute(EntityDamageByEntityEvent e, ItemStack i, int level, LivingEntity damager, LivingEntity victim) {
        if (manager == null) manager = CustomEnchantManager.getInstance();
        if (!victim.hasPermission("es.noregionrestrictions")){
            if (WorldguardHook.getWorldguardHook().isLocationInRegionWithFlag(e.getEntity().getLocation(), "es-deny-deflect-projectiles")){
                return;
            }
        }
        if (!(e.getDamager() instanceof Projectile)){
            return;
        }

        int collectiveLevel = 0;
        for (ItemStack item : Utils.getEntityEquipment(victim, true)){
            if (this.compatibleItems.contains(item.getType())){
                collectiveLevel += manager.getEnchantStrength(item, CustomEnchantEnum.SHIELDING, CustomEnchantClassification.ON_DAMAGED);
            }
        }

        double final_deflect_chance = collectiveLevel * deflect_chance_lv;
        if (RandomNumberGenerator.getRandom().nextDouble() <= final_deflect_chance){
            e.setCancelled(true);
            if (e.getDamager() instanceof SmallFireball){
                victim.setFireTicks(0);
            }
            if (victim instanceof Player){
                if (!message.equals("")){
                    ((Player) victim).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Utils.chat(message)));
                }
            }
        }
    }

    @Override
    public void loadConfig() {
        this.enchantLore = config.getString("enchantment_configuration.shielding.enchant_name");
        this.deflect_chance_lv = config.getDouble("enchantment_configuration.shielding.deflect_chance_lv");
        this.enabled = config.getBoolean("enchantment_configuration.shielding.enabled");
        this.weight = config.getInt("enchantment_configuration.shielding.weight");
        this.book_only = config.getBoolean("enchantment_configuration.shielding.book_only");
        this.max_level_table = config.getInt("enchantment_configuration.shielding.max_level_table");
        this.max_level = config.getInt("enchantment_configuration.shielding.max_level");
        this.enchantDescription = config.getString("enchantment_configuration.shielding.description");

        message = ConfigManager.getInstance().getConfig("translations.yml").get().getString("enchant_notifications.activation_deflect_projectile");

        for (String s : config.getStringList("enchantment_configuration.shielding.compatible_with")){
            try {
                MaterialClassType type = MaterialClassType.valueOf(s);
                this.compatibleItems.addAll(ItemMaterialManager.getInstance().getMaterialsFromType(type));
            } catch (IllegalArgumentException e){
                System.out.println("Material category " + s + " in the config:shielding is not valid, please correct it");
            }
        }
    }
}
