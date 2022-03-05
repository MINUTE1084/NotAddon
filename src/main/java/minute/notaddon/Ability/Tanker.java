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
import daybreak.abilitywar.utils.base.math.LocationUtil;
import minute.notaddon.NotUtil;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.function.Predicate;

@AbilityManifest(
        name = "탱커", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN,
        explain = {
                "§7패시브 §8- §a탱킹 §f현재 게임에 참가한 플레이어 수 × 4(2칸) 만큼 추가 체력을 받습니다. 체력은 최대 40(20칸)까지 추가됩니다.",
                "§7패시브 §8- §a고집 §f체력 회복이 불가능합니다.",
                "§7철괴 우 클릭 §8- §a돌진 §f바라보는 방향으로 돌진합니다. 돌진 중 부딪힌 플레이어를 멀리 밀쳐내며 6의 데미지를 줍니다. $[COOLDOWN]",
                " 돌진으로 플레이어 처치 시 체력을 6(3칸) 회복하며, 플레이어와 부딪힌 경우 돌진이 종료됩니다."
        },
        summarize = {
                "§7현재 게임에 참가한 플레이어 수 × 4(2칸) 만큼 추가 체력을 받습니다. 체력 회복이 불가능합니다.",
                "§7철괴 우 클릭 시 돌진 하며, 돌진중 부딪힌 플레이어를 멀리 밀쳐내며 6의 데미지를 줍니다. $[COOLDOWN]",
                "§7돌진으로 플레이어 처치 시 체력을 6(3칸) 회복합니다.",
        })

public class Tanker extends AbilityBase implements ActiveHandler {
    public Tanker(AbstractGame.Participant participant) {
        super(participant);
    }

    public static final AbilitySettings.SettingObject<Integer> COOLDOWN =
            abilitySettings.new SettingObject<Integer>(Tanker.class, "cooldown", 35, "# 쿨타임", "# 단위. 초") {
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

    private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), "탱커", CooldownDecrease._25);
    private final Vector addVector = new Vector(0, 1, 0);
    private final AbilityTimer rush = new AbilityTimer(SimpleTimer.TaskType.NORMAL, 12) {
        @Override
        public void run(int count) {
            checkRush();
        }
    }.setPeriod(TimeUnit.TICKS, 1).register();

    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            double addHealth = getGame().getParticipants().size() * 4;
            if (addHealth > 40) addHealth = 40;

            getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth + addHealth);
            getPlayer().setHealth(maxHealth + addHealth);
        } else if (update == Update.ABILITY_DESTROY) {
            getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
            if (getPlayer().getHealth() > getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue()) getPlayer().setHealth(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
        }
    }

    @SubscribeEvent
    public void onHealthRegain(EntityRegainHealthEvent e) {
        if (e.getEntity().equals(getPlayer())){
            e.setCancelled(true);
        }
    }

    @SubscribeEvent(priority = SubscribeEvent.Priority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent e){
        if (!e.getEntity().equals(getPlayer()) && rush.isRunning()) {
            String damageCause = e.getDeathMessage();
            if (damageCause == null) return;
            if (damageCause.contains(getPlayer().getName()) || damageCause.contains(getPlayer().getDisplayName())) {
                NotUtil.regenHealth(getPlayer(), 6);
            }
        }
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && !cool.isCooldown()) {
            if (clickType == ClickType.RIGHT_CLICK) {
                Vector vec = getPlayer().getLocation().getDirection();
                vec.setX(vec.getX() * 6.0);
                vec.setY(0.15);
                vec.setZ(vec.getZ() * 6.0);

                getPlayer().setVelocity(vec);
                getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.3f, 1);

                rush.start();
                return cool.start();
            }
        }
        return false;
    }

    private final Predicate<Player> rushPredicate = player -> !player.equals(getPlayer());
    private final ItemStack blood = new ItemStack(Material.REDSTONE_BLOCK);
    private void checkRush() {
        Player nearestEntity = LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), rushPredicate);
        if (nearestEntity == null) return;
        if (!nearestEntity.getWorld().equals(getPlayer().getWorld())) return;
        if (nearestEntity.getLocation().distance(getPlayer().getLocation()) > 3.0) return;

        Vector vec = getPlayer().getLocation().getDirection();
        vec.setX(vec.getX() * 6.0);
        vec.setY(0.15);
        vec.setZ(vec.getZ() * 6.0);

        nearestEntity.setVelocity(vec);
        nearestEntity.damage(6, getPlayer());
        nearestEntity.getWorld().playSound(nearestEntity.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.7f, 1f);
        nearestEntity.getWorld().spawnParticle(Particle.ITEM_CRACK, nearestEntity.getLocation().add(addVector), 50, 0.2, 0.5, 0.2, 0.05, blood);

        vec = getPlayer().getLocation().getDirection();
        vec.setX(vec.getX() * 0.75);
        vec.setZ(vec.getZ() * 0.75);

        getPlayer().setVelocity(vec);
        rush.stop(false);
    }
}
