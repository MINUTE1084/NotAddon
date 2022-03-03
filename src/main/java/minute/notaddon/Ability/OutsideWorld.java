package minute.notaddon.Ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

@AbilityManifest(
        name = "세상 밖", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.OTHERS,
        explain = {
                "§7철괴 타격 §8- §a세상 밖 구경§f 상대방 타격 시 상대방에게 세상 밖을 구경시켜 줍니다. $[COOLDOWN]",
                " 세상 밖은 Y -256입니다.",
                " 1초 후 원래 있던 자리로 복귀시킵니다."
        },
        summarize = {
                "§7철괴로 상대방 타격 시 상대방에게 세상 밖을 구경시켜 줍니다. $[COOLDOWN]",
                " 세상 밖은 Y -256입니다.",
                " 1초 후 원래 있던 자리로 복귀시킵니다."
        })

public class OutsideWorld extends AbilityBase {
    public OutsideWorld(AbstractGame.Participant participant) { super(participant); }

    public static final AbilitySettings.SettingObject<Integer> COOLDOWN =
            abilitySettings.new SettingObject<Integer>(OutsideWorld.class, "cooldown", 40,"# 쿨타임", "# 단위. 초") {
                @Override
                public boolean condition(Integer value) {
                    return value >= 0;
                }
                @Override
                public String toString() {
                    return Formatter.formatCooldown(getValue());
                }
            };

    private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), "세상 밖", CooldownDecrease._25);
    private final Vector addVector = new Vector(0, 1, 0);
    private final AbilityTimer move = new AbilityTimer(18) {
        @Override
        public void onEnd() {
            onSilentEnd();
        }

        @Override
        public void onSilentEnd() {
            targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.5f, 1.2f);
            targetPlayer.getWorld().playSound(beforeLoc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.5f, 1.2f);
            targetPlayer.getWorld().spawnParticle(Particle.SMOKE_NORMAL, beforeLoc.add(addVector), 200, 0.5, 1, 0.5, 0.1);
            targetPlayer.setFallDistance(0);
            targetPlayer.teleport(beforeLoc);
            targetPlayer = null;
            beforeLoc = null;
        }
    }.setPeriod(TimeUnit.TICKS, 1).register();

    private Player targetPlayer;
    private Location beforeLoc;

    @SubscribeEvent
    private void onDamaged(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player damager && e.getEntity() instanceof Player damagee && damager.getName().equals(getPlayer().getName())) {
            if (damager.getInventory().getItemInMainHand().getType().equals(Material.IRON_INGOT) && !cool.isCooldown()) {
                if (!move.isRunning()) {
                    targetPlayer = damagee;
                    beforeLoc = damagee.getLocation().clone();
                    Location newLoc = damagee.getLocation();
                    newLoc.setY(-256);
                    targetPlayer.teleport(newLoc);

                    damagee.getWorld().spawnParticle(Particle.SMOKE_NORMAL, beforeLoc.add(addVector), 200, 0.5, 1, 0.5, 0.1);
                    damagee.getWorld().playSound(beforeLoc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.5f, 1.2f);
                    damagee.getWorld().playSound(newLoc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.5f, 1.2f);

                    cool.start();
                    move.start();
                }
            }
        }
    }
}
