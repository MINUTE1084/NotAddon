package minute.notaddon.Ability;

import daybreak.abilitywar.AbilityWar;
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
import minute.notaddon.NotUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Random;

@AbilityManifest(
        name = "흑마법사", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN,
        explain = {
                "§7패시브 §8- §a계약 §f75% 확률로 이 능력을 제외한 체력 회복 효과가 작동하지 않습니다.",
                "§7철괴 좌 클릭 §8- §a소환 §f체력 4칸을 소모하여 강력한 몬스터를 소환합니다.",
                " 해당 몬스터는 소환 후 30초 뒤 사라지고, 자신을 공격하지 않으며 죽지 않습니다. $[LEFT_COOLDOWN]",
                "§7철괴 우 클릭 §8- §a파괴의 비  §a§f 우 클릭을 누르는 동안 차징을 하며, 차징 중에는 0.25초마다 1의 체력이 소모됩니다.",
                " 차징 종료 시 소모한 체력에 비례하여 주변에 번개를 떨어뜨립니다. $[RIGHT_COOLDOWN]",
                " 자신은 해당 번개에 데미지를 입지 않으며, 생명체가 번개에 데미지를 입을 시 1의 체력을 회복합니다.",
        },
        summarize = {
                "§7철괴 좌 클릭 시 체력을 소모하여 강력한 몬스터를 소환합니다.",
                "§7철괴를 우 클릭 하는 동안 자신의 체력을 소모하고, 종료하면 소모한 체력에 비례하여 번개를 떨어뜨립니다."
        })

public class Warlock extends AbilityBase implements ActiveHandler {
    public Warlock(AbstractGame.Participant participant) {
        super(participant);
    }

    public static final AbilitySettings.SettingObject<Integer> LEFT_COOLDOWN =
            abilitySettings.new SettingObject<Integer>(Warlock.class, "left_cooldown", 120, "# 소환 쿨타임", "# 단위. 초") {
                @Override
                public boolean condition(Integer value) {
                    return value >= 0;
                }

                @Override
                public String toString() {
                    return Formatter.formatCooldown(getValue());
                }
            };
    public static final AbilitySettings.SettingObject<Integer> RIGHT_COOLDOWN =
            abilitySettings.new SettingObject<Integer>(Warlock.class, "right_cooldown", 60, "# 파괴의 비 쿨타임", "# 단위. 초") {
                @Override
                public boolean condition(Integer value) {
                    return value >= 0;
                }

                @Override
                public String toString() {
                    return Formatter.formatCooldown(getValue());
                }
            };

    AbstractGame.Participant.ActionbarNotification.ActionbarChannel cc = newActionbarChannel();
    AbstractGame.Participant.ActionbarNotification.ActionbarChannel ac = newActionbarChannel();

    private final Cooldown leftCool = new Cooldown(LEFT_COOLDOWN.getValue(), "소환", CooldownDecrease._50);
    private final Cooldown rightCool = new Cooldown(RIGHT_COOLDOWN.getValue(), "파괴의 비", CooldownDecrease._25);
    private final Vector addVector = new Vector(0, 1, 0);
    private final AbilityTimer charge = new AbilityTimer() {
        boolean prevCharge = false;
        int times = 0;
        @Override
        public void run(int count) {
            times = count;
            ac.update("§6위력 §f: " + times);

            if (prevCharge && !isCharging) charge.stop(false);
            if (getPlayer().getHealth() <= 1) charge.stop(false);

            getPlayer().setHealth(getPlayer().getHealth() - 1);
            getPlayer().getWorld().spawnParticle(Particle.SMOKE_NORMAL, getPlayer().getLocation(), 50, 0.2, 0.5, 0.5, 0.05);
            getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 0.2f, 1.5f);

            prevCharge = isCharging;
            isCharging = false;
        }

        @Override
        protected void onEnd() { onSilentEnd(); }

        @Override
        protected void onSilentEnd() {
            ac.update("");
            ac.unregister();
            ac = newActionbarChannel();

            rainism(times);
        }
    }.setPeriod(TimeUnit.TICKS, 10).register();

    private final AbilityTimer killEntity = new AbilityTimer(SimpleTimer.TaskType.REVERSE, 15) {
        @Override
        public void run(int count) {
            cc.update("§7몬스터 §f: " + count + "초");
        }

        @Override
        public void onSilentEnd() {
            spawnedEntity.getWorld().spawnParticle(Particle.SMOKE_NORMAL, spawnedEntity.getEyeLocation(), 200, 0.3, 0.7, 0.3, 0.2);
            spawnedEntity.getWorld().playSound(spawnedEntity.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.5f, 0.8f);

            spawnedEntity.remove();
            spawnedEntity = null;

            cc.unregister();
            cc = newActionbarChannel();
        }

        @Override
        public void onEnd() { onSilentEnd(); }
    }.setPeriod(TimeUnit.SECONDS, 1).register();

    private boolean isCharging = false;
    private WitherSkeleton spawnedEntity;
    private final ItemStack[] equipments = new ItemStack[] {
            new ItemStack(Material.IRON_BOOTS),
            new ItemStack(Material.IRON_LEGGINGS),
            new ItemStack(Material.IRON_CHESTPLATE),
            new ItemStack(Material.IRON_HELMET)
    };
    private final ItemStack weapon = new ItemStack(Material.STONE_SWORD);

    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            ItemMeta meta = weapon.getItemMeta();
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            meta.setUnbreakable(true);

            weapon.setItemMeta(meta);
        }
    }

    @SubscribeEvent
    public void onHealthRegain(EntityRegainHealthEvent e) {
        if (e.getEntity().equals(getPlayer()) && (charge.isRunning() || new Random().nextDouble() <= 0.75)){
            e.setCancelled(true);
        }
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT) {
            if (clickType == ClickType.RIGHT_CLICK) {
                if (getPlayer().getHealth() > 1) {
                    isCharging = true;
                    if (!rightCool.isRunning() && !charge.isRunning()) charge.start();
                }
            } else {
                if (!leftCool.isCooldown() && getPlayer().getHealth() > 8) {
                    spawnedEntity = (WitherSkeleton) getPlayer().getWorld().spawnEntity(getPlayer().getLocation().add(addVector.multiply(3)), EntityType.WITHER_SKELETON);
                    spawnedEntity.getEquipment().setArmorContents(equipments);
                    spawnedEntity.getEquipment().setItemInMainHand(weapon);
                    spawnedEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.35);

                    getPlayer().setHealth(getPlayer().getHealth() - 8);

                    getPlayer().getWorld().spawnParticle(Particle.SMOKE_NORMAL, getPlayer().getEyeLocation(), 200, 0.3, 0.7, 0.3, 0.2);
                    getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f, 0.8f);

                    killEntity.start();
                    return leftCool.start();
                }
            }
        }
        return false;
    }

    @SubscribeEvent(priority = SubscribeEvent.Priority.HIGHEST)
    public void onEntityTargeted(EntityTargetLivingEntityEvent e){
        if (e.getTarget() != null && e.getEntity().equals(spawnedEntity) && e.getTarget().equals(getPlayer())){
            e.setCancelled(true);
        }
    }

    private final ItemStack heal = new ItemStack(Material.PURPLE_TERRACOTTA);

    @SubscribeEvent
    public void onLightningDamaged(EntityDamageByEntityEvent e){
        if (e.getDamager() instanceof LightningStrike ls && lightningStrikes.contains(ls)) {
            if (e.getEntity().equals(getPlayer())) e.setCancelled(true);
            else {
                NotUtil.regenHealth(getPlayer(), 1);

                getPlayer().getWorld().spawnParticle(Particle.ITEM_CRACK, getPlayer().getEyeLocation(), 30, 0.2, 0.5, 0.2, 0.05, heal);
                getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.ENTITY_RABBIT_ATTACK, 0.05f, 0.6f);
            }
        }

        if (e.getEntity().equals(spawnedEntity)) e.setDamage(0);
    }

    @SubscribeEvent
    public void onDamageCancel(EntityDamageEvent e){
        if (e.getEntity().equals(spawnedEntity)) e.setDamage(0);
    }

    @SubscribeEvent
    public void onDamageCancel2(EntityDamageByBlockEvent e){
        if (e.getEntity().equals(spawnedEntity)) e.setDamage(0);
    }


    private ArrayList<LightningStrike> lightningStrikes;
    private void rainism(int count){
        lightningStrikes = new ArrayList<>();
        Random random = new Random();
        int range = count * 2;
        float lsCount = count / 2f;
        if (lsCount < 1) lsCount = 1;

        for (int i = 0; i < lsCount; i++) {
            float finalLSCount = Math.max(lsCount, 5);
            int finalRange = Math.max(range, 10);
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int j = 0; j < finalLSCount; j++) {
                        Location loc = getPlayer().getLocation().add((random.nextInt(finalRange) - (finalRange / 2f)), 0, (random.nextInt(finalRange) - (finalRange / 2f)));
                        LightningStrike ls = (LightningStrike) getPlayer().getWorld().spawnEntity(loc, EntityType.LIGHTNING);
                        lightningStrikes.add(ls);
                    }
                }
            }.runTaskLater(AbilityWar.getPlugin(), i * 10L);
        }

        getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, (int)(lsCount + 1) * 10, 0));
        rightCool.start();
    }
}
