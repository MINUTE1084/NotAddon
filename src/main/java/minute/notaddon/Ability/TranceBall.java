package minute.notaddon.Ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.util.Vector;

import java.util.Random;

@AbilityManifest(
        name = "트랜스볼", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.OTHERS,
        explain = {
                "§7철괴 F §8- §a좌표 저장§f 현재 자신이 서있는 좌표를 저장합니다.",
                " 저장된 좌표는 이펙트로 나타나며, 모두가 확인 할 수 있습니다.",
                "§7철괴 타격 §8- §a강제 이동§f 상대방 타격 시 상대방을 저장된 좌표로 이동시킵니다.",
                "§7철괴 우 클릭 §8- §a자가 이동§f 우 클릭 시 자신을 저장된 좌표로 이동시킵니다.",
                "§f강제 이동, 자가 이동은 쿨타임을 공유합니다. ( $[COOLDOWN] )"
        },
        summarize = {
                "§7철괴를 왼손에 들 때 자신이 서있는 좌표를 저장합니다.",
                "§7철괴로 상대방 타격 시 상대방을 저장된 좌표로 이동시킵니다.",
                "§7철괴를 들고 우 클릭 시 자신을 저장된 좌표로 이동시킵니다.",
        })

public class TranceBall extends AbilityBase implements ActiveHandler {
    public TranceBall(AbstractGame.Participant participant) {
        super(participant);
    }

    public static final AbilitySettings.SettingObject<Integer> COOLDOWN =
            abilitySettings.new SettingObject<Integer>(TranceBall.class, "cooldown", 25, "# 쿨타임", "# 단위. 초") {
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

    private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), "이동", CooldownDecrease._25);
    private final Vector addVector = new Vector(0, 1, 0);
    private final Particle.DustOptions limeDust = new Particle.DustOptions(Color.LIME, 1);
    private final AbilityTimer passive = new AbilityTimer() {
        @Override
        public void run(int count) {
            if (savedLoc != null) {
                String str = "§a월드 §6: §b" + worldInfo(savedLoc.getWorld().getEnvironment()) + "§a, X §6: §b" + Math.round(savedLoc.getX()) + "§a, Y §6: §b" + Math.round(savedLoc.getY()) + "§a, Z §6: §b" + Math.round(savedLoc.getZ());
                savedLoc.getWorld().spawnParticle(Particle.REDSTONE, savedLoc.clone().add(addVector), 300, 0.2, 0.2, 0.2, 0.05, limeDust);
                savedLoc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, savedLoc.clone().add(addVector), 150, 0.2, 0.2, 0.2, 0.05);
                cc.update(str);
            }
        }
    }.setPeriod(TimeUnit.TICKS, 1).register();

    private Location savedLoc;

    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            passive.start();
        }
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && !cool.isCooldown()) {
            if (clickType == ClickType.RIGHT_CLICK) {
                if (savedLoc == null) {
                    getPlayer().sendMessage("§f[§c!§f] §c좌표가 저장되어 있지 않습니다.");
                    return false;
                }
                getPlayer().getWorld().spawnParticle(Particle.PORTAL, getPlayer().getLocation().add(addVector), 1000, 0.1, 0.1, 0.1);
                getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.5f, 1);

                getPlayer().getWorld().spawnParticle(Particle.REVERSE_PORTAL, savedLoc.clone().add(addVector), 1000, 0.1, 0.1, 0.1);
                getPlayer().getWorld().playSound(savedLoc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.5f, 1);

                getPlayer().setFallDistance(0);
                getPlayer().teleport(savedLoc);
                return cool.start();
            }
        }
        return false;
    }

    @SubscribeEvent
    private void onDamaged(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player damager && e.getEntity() instanceof Player damagee && damager.getName().equals(getPlayer().getName())) {
            if (damager.getInventory().getItemInMainHand().getType().equals(Material.IRON_INGOT) && !cool.isCooldown()) {
                if (savedLoc == null) {
                    getPlayer().sendMessage("§f[§c!§f] §c좌표가 저장되어 있지 않습니다.");
                    return;
                }
                damagee.getWorld().spawnParticle(Particle.PORTAL, damagee.getLocation().add(addVector), 1000, 0.1, 0.1, 0.1);
                damagee.getWorld().playSound(damagee.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.5f, 1);

                damagee.getWorld().spawnParticle(Particle.REVERSE_PORTAL, savedLoc.clone().add(addVector), 1000, 0.1, 0.1, 0.1);
                damagee.getWorld().playSound(savedLoc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.5f, 1);

                damagee.setFallDistance(0);
                damagee.teleport(savedLoc);
                cool.start();
            }
        }
    }

    @SubscribeEvent
    private void onSwap(PlayerSwapHandItemsEvent e) {
        if (e.getOffHandItem() != null) {
            if (e.getOffHandItem().getType().equals(Material.IRON_INGOT)) {
                savedLoc = e.getPlayer().getLocation();
                getPlayer().sendMessage("§f[§a!§f] §a좌표를 저장했습니다.");
                e.getPlayer().spawnParticle(Particle.SMOKE_NORMAL, e.getPlayer().getLocation(), 150, 0.5, 1, 0.5, 0.1);
                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1);
            }
        }
    }

    private String worldInfo(World.Environment info) {
        if (info == World.Environment.NETHER) return "네더";
        if (info == World.Environment.THE_END) return "엔드";
        if (info == World.Environment.NORMAL) return "일반";
        else return "???";
    }
}
