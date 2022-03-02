package minute.notaddon.Ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

@AbilityManifest(
        name = "대장장이", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.OTHERS,
        explain = {
                "§7철괴 우 클릭 §8- §a강화§f 왼손에 든 무기를 강화합니다.",
                " 강화는 도끼, 검 종류만 가능합니다.",
                " 강화에 성공할 때마다 무기 공격력이 1씩 상승합니다.",
                " 강화된 아이템에는 §c소실 저주§f 및 §6부서지지 않음§f이 붙습니다.",
                " 강화 실패 시 §c무기가 파괴§f됩니다.",
                "§7강화 확률 : (★1) 90% - (★2) 70% - (★3) 50% - (★4) 30% - (★5) 15%"
        },
        summarize = {
                "§7철괴 우 클릭 시 왼손에 든 도끼, 검을 강화합니다.",
                "강화에 성공할 때마다 무기 공격력이 1씩 상승합니다.",
                "강화 실패 시 §c무기가 파괴§f됩니다."
        })

public class StarForce extends AbilityBase implements ActiveHandler {
    public StarForce(AbstractGame.Participant participant) {
        super(participant);
    }

    AbstractGame.Participant.ActionbarNotification.ActionbarChannel cc = newActionbarChannel();

    private final Vector addVector = new Vector(0, 1, 0);
    private final AbilityTimer reinforce = new AbilityTimer(SimpleTimer.TaskType.NORMAL, 40) {
        @Override
        public void run(int count) {
            if (count < 30) {
                if (count % 10 == 1) {
                    getPlayer().getWorld().spawnParticle(Particle.LAVA, getPlayer().getLocation().add(addVector), 20, 0.5, 0.5, 0.5, 0.5);
                    getPlayer().getWorld().spawnParticle(Particle.SMOKE_NORMAL, getPlayer().getLocation().add(addVector), 50, 0.5, 0.5, 0.5, 0.05);
                    getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.6f);
                    cc.update("§a강화 중...");
                }
            } else if (count == 31){
                int result = upgrade();
                if ( result == -1 ) {
                    getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1.2f);
                    getPlayer().getWorld().spawnParticle(Particle.ITEM_CRACK, getPlayer().getLocation().add(addVector), 200, 0.1, 0.1, 0.1, 0.05, targetItem);
                    getPlayer().sendMessage("§c강화에 실패하여 아이템이 0강이 되었습니다.");
                    cc.update("§c강화 실패!");
                } else {
                    getPlayer().sendMessage("§a강화에 성공했습니다! §6(★" + (result - 1) + " -> ★" + result + ")");
                    cc.update("§6강화 성공!");
                    getPlayer().getWorld().spawnParticle(Particle.VILLAGER_HAPPY, getPlayer().getLocation().add(addVector), 100, 0.5, 1, 0.5, 0.05);
                    if ( result < 5 ) {
                        getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.ENTITY_VILLAGER_YES, 0.25f, 1);
                    } else getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.25f, 1);
                }
            }
        }

        @Override
        public void onEnd() {
            onSilentEnd();
        }

        @Override
        public void onSilentEnd() {
            cc.unregister();
            cc = newActionbarChannel();
        }
    }.setPeriod(TimeUnit.TICKS, 1).register();

    private ItemStack targetItem;

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && !reinforce.isRunning()) {
            if (clickType == ClickType.RIGHT_CLICK) {
                ItemStack targetItem = getPlayer().getInventory().getItemInOffHand();
                if (targetItem == null || targetItem.getType().equals(Material.AIR)) {
                    getPlayer().sendMessage("§f[§c!§f] §c왼손에 아이템이 없습니다.");
                    return false;
                }
                if (targetItem.getItemMeta().getDisplayName().contains("★5")) {
                    getPlayer().sendMessage("§f[§c!§f] §c이미 최대 강화 상태입니다.");
                    return false;
                }
                if (weaponDamage(targetItem.getType()) < 2) {
                    getPlayer().sendMessage("§f[§c!§f] §c강화가 불가능한 종류의 아이템입니다.");
                    return false;
                }

                this.targetItem = targetItem;
                return reinforce.start();
            }
        }
        return false;
    }

    public int upgrade() {
        ItemMeta itemMeta = targetItem.getItemMeta();

        int targetStar = 1;
        String displayName = itemMeta.getDisplayName();
        if (displayName.contains("★1")) targetStar = 2;
        else if (displayName.contains("★2")) targetStar = 3;
        else if (displayName.contains("★3")) targetStar = 4;
        else if (displayName.contains("★4")) targetStar = 5;

        double targetStarPercent = 0.9;
        if (targetStar == 2) targetStarPercent = 0.7;
        else if (targetStar == 3) targetStarPercent = 0.5;
        else if (targetStar == 4) targetStarPercent = 0.3;
        else if (targetStar == 5) targetStarPercent = 0.15;

        double randomNumber = new Random().nextDouble();
        if (randomNumber <= targetStarPercent) {
            var itemDamage = weaponDamage(targetItem.getType()) - 1;

            if (targetStar == 1) itemDamage = itemDamage + 1;
            else if (targetStar == 2) itemDamage = itemDamage + 2;
            else if (targetStar == 3) itemDamage = itemDamage + 3;
            else if (targetStar == 4) itemDamage = itemDamage + 4;
            else if (targetStar == 5) itemDamage = itemDamage + 5;

            var itemAttribute = new AttributeModifier(UUID.randomUUID(), "generic.attackDamage", itemDamage, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);

            if (!itemMeta.hasEnchant(Enchantment.VANISHING_CURSE)) itemMeta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            if (!itemMeta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES)) itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            if (itemMeta.hasAttributeModifiers()) itemMeta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
            itemMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, itemAttribute);
            itemMeta.removeAttributeModifier(EquipmentSlot.OFF_HAND);

            var lore = new ArrayList<String>();
            lore.add("§7무기 데미지 : " + (itemDamage + 1));
            itemMeta.setLore(lore);
            itemMeta.setUnbreakable(true);

            itemMeta.setDisplayName("§6[★" + targetStar + "] §a대장장이의 무기");
            targetItem.setItemMeta(itemMeta);
        }
        else {
            ItemMeta blankItem = new ItemStack(targetItem.getType()).getItemMeta();
            targetItem.setItemMeta(blankItem);
            return -1;
        }

        return targetStar;
    }

    private int weaponDamage(Material itemType) {
        if (itemType == Material.WOODEN_AXE) return 7;
        else if (itemType == Material.WOODEN_SWORD) return 4;
        else if (itemType == Material.STONE_AXE) return 9;
        else if (itemType == Material.STONE_SWORD) return 5;
        else if (itemType == Material.IRON_AXE) return 9;
        else if (itemType == Material.IRON_SWORD) return 6;
        else if (itemType == Material.GOLDEN_AXE) return 7;
        else if (itemType == Material.GOLDEN_SWORD) return 4;
        else if (itemType == Material.DIAMOND_AXE) return 9;
        else if (itemType == Material.DIAMOND_SWORD) return 7;
        else if (itemType == Material.NETHERITE_AXE) return 10;
        else if (itemType == Material.NETHERITE_SWORD) return 8;
        else return 1;
    }
}
