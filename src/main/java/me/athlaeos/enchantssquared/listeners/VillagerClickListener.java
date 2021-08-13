package me.athlaeos.enchantssquared.listeners;

import me.athlaeos.enchantssquared.configs.ConfigManager;
import me.athlaeos.enchantssquared.dom.CustomEnchant;
import me.athlaeos.enchantssquared.dom.SingleEnchant;
import me.athlaeos.enchantssquared.enchantments.StandardGlintEnchantment;
import me.athlaeos.enchantssquared.main.EnchantsSquared;
import me.athlaeos.enchantssquared.managers.CustomEnchantManager;
import me.athlaeos.enchantssquared.managers.RandomNumberGenerator;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class VillagerClickListener implements Listener {
    private final double bookCustomEnchantChance;
    private final double otherCustomEnchantChance;
    private final int otherCustomEnchantRolls;
    private final NamespacedKey key = new NamespacedKey(EnchantsSquared.getPlugin(), "es_tradeitem_custom");
    public VillagerClickListener(){
        bookCustomEnchantChance = ConfigManager.getInstance().getConfig("config.yml").get().getDouble("custom_enchant_trade_rate_book");
        otherCustomEnchantChance = ConfigManager.getInstance().getConfig("config.yml").get().getDouble("custom_enchant_trade_rate_other");
        otherCustomEnchantRolls = Math.max(1, ConfigManager.getInstance().getConfig("config.yml").get().getInt("custom_enchant_trade_rolls_other"));
    }

    @EventHandler
    public void onVillagerClick(PlayerInteractAtEntityEvent e){
        Entity entity = e.getRightClicked();
        CustomEnchantManager manager = CustomEnchantManager.getInstance();
        if (entity instanceof Villager){
            Villager villager = (Villager) entity;
            List<MerchantRecipe> villagerTrades = new ArrayList<>();
            for (MerchantRecipe trade : villager.getRecipes()){
                ItemStack tradeItem = trade.getResult();
                if (tradeItem.getItemMeta() == null) {
                    villagerTrades.add(trade);
                    continue;
                }
                if (tradeItem.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.SHORT)){
                    villagerTrades.add(trade);
                    continue;
                }

                ItemStack newResult = tradeItem.clone();
                Map<CustomEnchant, Integer> newCustomEnchantMap = new HashMap<>();

                List<ItemStack> newIngredients = trade.getIngredients();
                if (tradeItem.getType() == Material.ENCHANTED_BOOK){
                    ItemMeta newResultMeta = newResult.getItemMeta();
                    assert newResultMeta != null;
                    newResultMeta.getPersistentDataContainer().set(key, PersistentDataType.SHORT, (short) 1);
                    newResult.setItemMeta(newResultMeta);
                    if (RandomNumberGenerator.getRandom().nextDouble() * 100 <= bookCustomEnchantChance){
                        ItemMeta resultMeta = newResult.getItemMeta();
                        newIngredients = new ArrayList<>();
                        if (resultMeta instanceof EnchantmentStorageMeta){
                            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) resultMeta;
                            for (Enchantment en : meta.getStoredEnchants().keySet()){
                                meta.removeStoredEnchant(en);
                            }
                            meta.addStoredEnchant(StandardGlintEnchantment.getEnsquaredGlint(), 1, true);
                            newResult.setItemMeta(meta);

                            SingleEnchant chosenEnchant = manager.pickRandomEnchant(manager.getTradableEnchants());
                            newCustomEnchantMap.put(chosenEnchant.getEnchantment(), chosenEnchant.getLevel());
                            manager.setItemEnchants(newResult, newCustomEnchantMap);
                            ItemStack book = null;
                            boolean hasEmeralds = false;
                            for (ItemStack i : trade.getIngredients()){
                                if (i.getType() == Material.BOOK) book = i;
                                if (i.getType() == Material.EMERALD) {
                                    hasEmeralds = true;
                                }
                            }
                            if (book == null && !hasEmeralds){
                                newIngredients = trade.getIngredients();
                            }
                            if (hasEmeralds){
                                int minAmount = (chosenEnchant.getLevel() <= 1) ? chosenEnchant.getEnchantment().getTradeMinCostBase() : chosenEnchant.getEnchantment().getTradeMinCostBase() + (chosenEnchant.getEnchantment().getTradeMinCostLv() * (chosenEnchant.getLevel() - 1));
                                int maxAmount = (chosenEnchant.getLevel() <= 1) ? chosenEnchant.getEnchantment().getTradeMaxCostBase() : chosenEnchant.getEnchantment().getTradeMaxCostBase() + (chosenEnchant.getEnchantment().getTradeMaxCostLv() * (chosenEnchant.getLevel() - 1));
                                int emeraldAmount = Math.max(1, RandomNumberGenerator.getRandom().nextInt((maxAmount - minAmount) + 1) + minAmount);
                                newIngredients.add(new ItemStack(Material.EMERALD, emeraldAmount));
                            }
                            if (book != null) {
                                newIngredients.add(book);
                            }
                        } else {
                            villagerTrades.add(trade);
                        }
                    }
                    MerchantRecipe newTrade = new MerchantRecipe(newResult, trade.getUses(), trade.getMaxUses(), trade.hasExperienceReward(), trade.getVillagerExperience(), trade.getPriceMultiplier());
                    newTrade.setIngredients(newIngredients);
                    villagerTrades.add(newTrade);
                } else {
                    if (newResult.getEnchantments().size() > 0){
                        ItemMeta newResultMeta = newResult.getItemMeta();
                        assert newResultMeta != null;
                        newResultMeta.getPersistentDataContainer().set(key, PersistentDataType.SHORT, (short) 1);
                        newResult.setItemMeta(newResultMeta);

                        if (RandomNumberGenerator.getRandom().nextDouble() * 100 <= otherCustomEnchantChance){
                            for (int i = 0; i < RandomNumberGenerator.getRandom().nextInt(otherCustomEnchantRolls) + 1; i++){
                                SingleEnchant chosenEnchant = manager.pickRandomEnchant(manager.getCompatibleEnchants(tradeItem));
                                if (chosenEnchant != null){
                                    newCustomEnchantMap.put(chosenEnchant.getEnchantment(), chosenEnchant.getLevel());
                                }
                            }
                            manager.setItemEnchants(newResult, newCustomEnchantMap);
                        }
                        MerchantRecipe newTrade = new MerchantRecipe(newResult, trade.getUses(), trade.getMaxUses(), trade.hasExperienceReward(), trade.getVillagerExperience(), trade.getPriceMultiplier());
                        newTrade.setIngredients(trade.getIngredients());
                        villagerTrades.add(newTrade);
                    } else {
                        villagerTrades.add(trade);
                    }
                }
            }
            villager.setRecipes(villagerTrades);
        }
    }
}
