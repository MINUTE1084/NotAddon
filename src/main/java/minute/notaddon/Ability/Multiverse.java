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
import daybreak.abilitywar.utils.base.math.LocationUtil;
import minute.notaddon.Effect.Stable;
import minute.notaddon.Effect.Unstable;
import minute.notaddon.NotAddon;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.util.Vector;

import java.sql.Time;
import java.util.Collection;
import java.util.List;

@AbilityManifest(
        name = "차원 이동자", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN,
        explain = {
                "§7철괴 우 클릭 §8- §a차원 이동§f : 7초 간 다른 차원으로 이동하며, 다른 차원에 있는 동안 자신에게 §d안정 §f효과를 부여합니다. $[COOLDOWN]",
                " 차원 이동 시 다른 플레이어를 볼 수 없으며, 다른 플레이어들은 당신을 볼 수 없습니다. 대신, 다른 플레이어들의 위치가 파티클로 표시됩니다.",
                " 한번 더 우 클릭 해서 능력을 즉시 종료 시킬 수 있습니다.",
                "§7능력 종료 §8- §a복귀§f : 원래 차원으로 돌아옵니다.",
                " 복귀 시 자신을 포함한 7칸 내 주변 플레이어들에게 §c불안정 §f효과를 부여합니다.",
                " §c불안정 §f효과는 자신과 거리가 가까울수록 길게 부여됩니다."
        },
        summarize = {
                "§7철괴 우 클릭 시 다른 차원으로 이동하며, 자신에게 안정 효과를 부여합니다.",
                "§7차원 이동 시 다른 플레이어를 볼 수 없으며, 다른 플레이어들은 당신을 볼 수 없습니다.",
                "§7원래 차원으로 복귀 시 자신을 포함한 주변 플레이어들에게 §c불안정 §f효과를 부여합니다."
        })

public class Multiverse extends AbilityBase implements ActiveHandler {
    public Multiverse(AbstractGame.Participant participant) {
        super(participant);
    }

    public static final AbilitySettings.SettingObject<Integer> COOLDOWN =
            abilitySettings.new SettingObject<Integer>(Multiverse.class, "cooldown", 50, "# 쿨타임", "# 단위. 초") {
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
    private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), "차원 이동", CooldownDecrease._25);
    private final Vector addVector = new Vector(0, 1, 0);
    private final Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(204, 255, 255), 0.5f);
    private final AbilityTimer anotherDimension = new AbilityTimer(SimpleTimer.TaskType.REVERSE, 140) {
        @Override
        protected void onStart() {
            Stable.apply(getParticipant(), TimeUnit.TICKS, 70);
        }

        @Override
        public void run(int count) {
            cc.update("§6차원 이동 §f: " + (count / 20) + "초");
            enterDimension();
            List<Player> nearbyPlayers = LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), 7, null);
            for (AbstractGame.Participant p : players){
                if (!p.equals(getParticipant())) {
                    getPlayer().spawnParticle(Particle.SMOKE_NORMAL, p.getPlayer().getLocation().add(addVector), 10, 0.2, 0.35, 0.2, 0.01);
                    if (nearbyPlayers.contains(p.getPlayer())) {
                        float length = (float) p.getPlayer().getLocation().distance(getPlayer().getLocation());
                        int newGB = length > 0 ? Math.round(255 * (length / 14f)) : 0;
                        getPlayer().spawnParticle(Particle.REDSTONE, p.getPlayer().getLocation().add(addVector), 10, 0.2, 0.35, 0.2, 0.05, new Particle.DustOptions(Color.fromRGB(204, newGB, newGB), 0.5f));
                    }
                    else getPlayer().spawnParticle(Particle.REDSTONE, p.getPlayer().getLocation().add(addVector), 10, 0.2, 0.35, 0.2, 0.05, dust);
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
            exitDimension();
        }
    }.setPeriod(TimeUnit.TICKS, 1).register();

    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR || update == Update.ABILITY_DESTROY) {
            for (AbstractGame.Participant p : players){
                if (!p.equals(getParticipant())) {
                    getPlayer().showPlayer(AbilityWar.getPlugin(), p.getPlayer());
                    p.getPlayer().showPlayer(AbilityWar.getPlugin(), getPlayer());
                }
            }
        }
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
            if (anotherDimension.isRunning()) {
                anotherDimension.stop(false);
            } else if (!cool.isCooldown()) {
                anotherDimension.start();
                return cool.start();
            }
        }
        return false;
    }

    private void enterDimension() {
        for (AbstractGame.Participant p : players){
            if (!p.equals(getParticipant())) {
                getPlayer().hidePlayer(AbilityWar.getPlugin(), p.getPlayer());
                p.getPlayer().hidePlayer(AbilityWar.getPlugin(), getPlayer());
            }
        }
    }

    private void exitDimension() {
        List<Player> nearbyPlayers = LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), 7, null);
        for (AbstractGame.Participant p : players){
            if (!p.equals(getParticipant())) {
                getPlayer().showPlayer(AbilityWar.getPlugin(), p.getPlayer());
                p.getPlayer().showPlayer(AbilityWar.getPlugin(), getPlayer());
            }

            if (nearbyPlayers.contains(p.getPlayer())){
                float length = (float) p.getPlayer().getLocation().distance(getPlayer().getLocation());
                Unstable.apply(p, TimeUnit.TICKS, Math.round((7 - length) * 5) + 20);
                p.getPlayer().getWorld().spawnParticle(Particle.SMOKE_NORMAL, p.getPlayer().getLocation().add(addVector), 100, 0.25, 0.7, 0.25, 0.3);
            }
        }
    }
}
