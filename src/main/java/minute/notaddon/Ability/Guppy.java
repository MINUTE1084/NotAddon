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
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.Random;

@AbilityManifest(
        name = "구피", rank = AbilityManifest.Rank.B, species = AbilityManifest.Species.ANIMAL,
        explain = {
                "§c체력이 1칸이 됩니다.",
                "§f대신, 목숨이 9개가 되며, §c치명적인 데미지§f를 입을 때마다 1개 씩 소모합니다.",
                "§f목숨을 소모할 떄 마다 1초 간 모든 데미지를 무시합니다."
        },
        summarize = {
                "§c체력이 1칸이 됩니다.",
                "§f대신, 목숨이 9개가 되며, §c치명적인 데미지§f를 입을 때마다 1개 씩 소모합니다.",
                "§f목숨을 소모할 떄 마다 1초 간 모든 데미지를 무시합니다."
        })

public class Guppy extends AbilityBase {
    public Guppy(AbstractGame.Participant participant) { super(participant); }

    private int lives = 9;
    private final Vector addVector = new Vector(0, 1, 0);

    AbstractGame.Participant.ActionbarNotification.ActionbarChannel ac = newActionbarChannel();
    AbstractGame.Participant.ActionbarNotification.ActionbarChannel cc = newActionbarChannel();

    private final AbilityTimer passive = new AbilityTimer() {
        @Override
        public void run(int count) {
            if (lives > 1) ac.update("§a남은 목숨 : " + lives + "개");
            else ac.update("§c남은 목숨 : " + lives + "개");
        }
    }.setPeriod(TimeUnit.TICKS, 1).register();

    private final AbilityTimer cancelDamage = new AbilityTimer(20) {
        @Override
        public void run(int count) {
            cc.update("§6데미지 무시");
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

    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            passive.start();
            getPlayer().setHealth(2);
            getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(2);
        }
        if (update == Update.ABILITY_DESTROY) {
            getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
        }
    }

    @SubscribeEvent
    private void onDamaged(EntityDamageEvent e) {
        if (e.getEntity().getType().equals(EntityType.PLAYER) && e.getEntity().getName().equals(getPlayer().getName())) {
            if (cancelDamage.isRunning()) e.setDamage(0);
            else if (e.getDamage() >= getPlayer().getHealth()) {
                if (lives > 1) {
                    e.setDamage(0);
                    lives--;
                    getPlayer().getWorld().spawnParticle(Particle.TOTEM, getPlayer().getLocation().add(addVector), 200, 0.5, 1, 0.5, 0.75);
                    getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.ITEM_TOTEM_USE, 0.5F, 1);
                    getPlayer().setHealth(2.0);
                    cancelDamage.start();
                }
                else {
                    lives = 9;
                }
            }
        }
    }

    @SubscribeEvent
    private void onDamagedByEntity(EntityDamageByEntityEvent e) {
        if (e.getEntity().getType().equals(EntityType.PLAYER) && e.getEntity().getName().equals(getPlayer().getName())) {
            if (cancelDamage.isRunning()) e.setDamage(0);
            else if (e.getDamage() >= getPlayer().getHealth()) {
                if (lives > 1) {
                    e.setDamage(0);
                    lives--;
                    getPlayer().getWorld().spawnParticle(Particle.TOTEM, getPlayer().getLocation().add(addVector), 200, 0.5, 1, 0.5, 0.75);
                    getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.ITEM_TOTEM_USE, 0.2F, 1);
                    getPlayer().setHealth(2.0);
                    cancelDamage.start();
                }
                else {
                    lives = 9;
                }
            }
        }
    }

    @SubscribeEvent
    private void onDamagedByBlock(EntityDamageByBlockEvent e) {
        if (e.getEntity().getType().equals(EntityType.PLAYER) && e.getEntity().getName().equals(getPlayer().getName())) {
            if (cancelDamage.isRunning()) e.setCancelled(true);
            else if (e.getDamage() >= getPlayer().getHealth()) {
                if (lives > 1) {
                    e.setCancelled(true);
                    lives--;
                    getPlayer().getWorld().spawnParticle(Particle.TOTEM, getPlayer().getLocation().add(addVector), 200, 0.5, 1, 0.5, 0.75);
                    getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.ITEM_TOTEM_USE, 0.2F, 1);
                    getPlayer().setHealth(2.0);
                    cancelDamage.start();
                }
                else {
                    lives = 9;
                }
            }
        }
    }
}
