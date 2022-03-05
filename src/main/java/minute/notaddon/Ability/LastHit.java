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
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import minute.notaddon.Effect.Unstable;
import minute.notaddon.NotUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;

@AbilityManifest(
        name = "막타의 달인", rank = AbilityManifest.Rank.B, species = AbilityManifest.Species.OTHERS,
        explain = {
                "§7패시브 §8- §a막타충 §f피가 2칸 이하인 플레이어 머리 위에 §c표식§f이 생깁니다.",
                " §c표식§f이 없는 적을 공격 시 데미지가 30% 감소하며, 플레이어를 죽일 때 마다 체력을 2칸 회복합니다.",
                "§7철괴 타격 §8- §a컷!§f 타격한 대상을 높게 띄웁니다.",
                "타격한 대상에게 §c표식§f이 없는 경우, 자신을 높게 띄웁니다. $[COOLDOWN]"
        },
        summarize = {
                "§7피가 2칸 이하인 플레이어에게 표식이 생기고, 표식이 없는 적을 공격 시 데미지가 30% 감소합니다.",
                "§7철괴로 타격 시 표식이 있는 경우 상대를 높게 띄우고, 표식이 없는 경우 자신을 높게 띄웁니다.",
        })

public class LastHit extends AbilityBase {
    public LastHit(AbstractGame.Participant participant) {
        super(participant);
    }

    public static final AbilitySettings.SettingObject<Integer> COOLDOWN =
            abilitySettings.new SettingObject<Integer>(LastHit.class, "cooldown", 20, "# 쿨타임", "# 단위. 초") {
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

    private final Collection<? extends AbstractGame.Participant> players = getGame().getParticipants();
    private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), "막타의 달인", CooldownDecrease._25);
    private final Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 1);
    private final AbilityTimer passive = new AbilityTimer() {
        @Override
        public void run(int count) {
            for (AbstractGame.Participant p : players){
                if (!p.equals(getParticipant()) && p.getPlayer().getHealth() <= 4) {
                    getPlayer().spawnParticle(Particle.REDSTONE, p.getPlayer().getLocation().add(0, 3, 0), 10, 0.1, 0.3, 0.1, 0.05, dust);
                }
            }
        }
    }.setPeriod(TimeUnit.TICKS, 1).register();

    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            passive.start();
        }
    }

    @SubscribeEvent
    public void onHit(EntityDamageByEntityEvent e){
        if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player damagee){
            if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.IRON_INGOT) && !cool.isRunning()) {
                cool.start();
                if (damagee.getHealth() <= 4) damagee.damage(e.getDamage(), e.getDamager());

                e.setCancelled(true);
                Player target = damagee.getHealth() <= 4 ? damagee : getPlayer();

                Vector vec = getPlayer().getEyeLocation().getDirection();
                vec.setX(vec.getX() / 4f);
                vec.setY(1.8);
                vec.setZ(vec.getZ() / 4f);
                target.setVelocity(vec);
                target.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, target.getLocation(), 1, 0, 0, 0, 0.1);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1);


            }

            e.setDamage(e.getDamage() * (damagee.getHealth() <= 4 ? 1.0f : 0.7f));
        }
    }


    @SubscribeEvent(priority = SubscribeEvent.Priority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent e){
        if (!e.getEntity().equals(getPlayer())) {
            String damageCause = e.getDeathMessage();
            if (damageCause == null) return;
            if (damageCause.contains(getPlayer().getName()) || damageCause.contains(getPlayer().getDisplayName())) {
                NotUtil.regenHealth(getPlayer(), 4);
                getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.ENTITY_WITCH_DRINK, 0.5f, 1.2f);
                getPlayer().getWorld().spawnParticle(Particle.HEART, getPlayer().getLocation().add(0, 1, 0), 10, 0.3, 0.7, 0.3, 0.1);
            }
        }
    }
}
