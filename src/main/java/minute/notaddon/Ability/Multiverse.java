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
import minute.notaddon.NotUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

@AbilityManifest(
        name = "차원 이동자", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN,
        explain = {
                "§7철괴 우 클릭 §8- §a차원 이동§f : 7초 간 다른 차원으로 이동하며, 다른 차원에 있는 동안 자신에게 §d안정 §f효과를 부여합니다. $[COOLDOWN]",
                " 차원 이동 시 다른 플레이어를 볼 수 없으며, 다른 플레이어들은 당신을 볼 수 없습니다. 대신, 다른 플레이어들의 위치가 파티클로 표시됩니다.",
                " 한번 더 우 클릭 해서 능력을 즉시 종료 시킬 수 있습니다.",
                "§7능력 종료 §8- §a복귀§f : 원래 차원으로 돌아옵니다.",
                " 복귀 시 자신을 포함한 7칸 내 주변 플레이어들에게 §c불안정 §f효과를 부여합니다.",
                " §c불안정 §f효과는 자신과 거리가 멀수록 길게 부여됩니다."
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
            abilitySettings.new SettingObject<Integer>(Multiverse.class, "cooldown", 49, "# 쿨타임", "# 단위. 초") {
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
            for (AbstractGame.Participant p : players) {
                if (!p.equals(getParticipant())) getPlayer().spawnParticle(Particle.SMOKE_NORMAL, p.getPlayer().getLocation().add(addVector), 500, 0.3, 0.8, 0.3, 0.05);
            }
            Stable.apply(getParticipant(), TimeUnit.TICKS, 70);
        }

        @Override
        public void run(int count) {
            cc.update("§6차원 이동 §f: " + (count / 20) + "초");
            enterDimension();
        }

        @Override
        public void onEnd() {
            onSilentEnd();
        }

        @Override
        public void onSilentEnd() {
            exitEffect.start();
        }
    }.setPeriod(TimeUnit.TICKS, 1).register();

    private final AbilityTimer enterEffect = new AbilityTimer(SimpleTimer.TaskType.NORMAL, 7) {
        @Override
        public void run(int count) {
            cc.update("§6차원 이동 중...");
            portalEffect();
        }

        @Override
        public void onEnd() {
            onSilentEnd();
        }

        @Override
        public void onSilentEnd() {
            anotherDimension.start();
            getPlayer().spawnParticle(Particle.SMOKE_NORMAL, getPlayer().getLocation().add(addVector), 300, 0.3, 0.8, 0.3, 0.05);
            getPlayer().playSound(getPlayer().getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.1f, 2f);
        }
    }.setPeriod(TimeUnit.TICKS, 3).register();

    private final AbilityTimer exitEffect = new AbilityTimer(SimpleTimer.TaskType.NORMAL, 7) {
        @Override
        public void run(int count) {
            cc.update("§6돌아가는 중...");
            portalEffect();
            enterDimension();
        }

        @Override
        public void onEnd() {
            onSilentEnd();
        }

        @Override
        public void onSilentEnd() {
            getPlayer().playSound(getPlayer().getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.1f, 2f);
            getPlayer().spawnParticle(Particle.SMOKE_NORMAL, getPlayer().getLocation().add(addVector), 300, 0.3, 0.8, 0.3, 0.05);
            exitDimension();
            cc.unregister();
            cc = newActionbarChannel();
        }
    }.setPeriod(TimeUnit.TICKS, 3).register();

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
            if (!(enterEffect.isRunning() || exitEffect.isRunning())) {
                if (anotherDimension.isRunning()) {
                    anotherDimension.stop(false);
                } else if (!cool.isCooldown()) {
                    enterEffect.start();
                    return cool.start();
                }
            }
        }
        return false;
    }

    private void enterDimension() {
        List<Player> nearbyPlayers = LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), 7, null);

        for (AbstractGame.Participant p : players){
            if (!p.equals(getParticipant())) {
                getPlayer().hidePlayer(AbilityWar.getPlugin(), p.getPlayer());
                p.getPlayer().hidePlayer(AbilityWar.getPlugin(), getPlayer());

                getPlayer().spawnParticle(Particle.SMOKE_NORMAL, p.getPlayer().getLocation().add(addVector), 10, 0.2, 0.35, 0.2, 0.01);
                if (nearbyPlayers.contains(p.getPlayer())) {
                    float length = (float) p.getPlayer().getLocation().distance(getPlayer().getLocation());
                    int newGB = Math.round(255 * (1f - (length / 8f)));
                    if (newGB > 255) newGB = 255;
                    if (newGB < 0) newGB = 0;
                    getPlayer().spawnParticle(Particle.REDSTONE, p.getPlayer().getLocation().add(addVector), 10, 0.2, 0.35, 0.2, 0.05, new Particle.DustOptions(Color.fromRGB(204, newGB, newGB), 0.5f));
                }
                else getPlayer().spawnParticle(Particle.REDSTONE, p.getPlayer().getLocation().add(addVector), 10, 0.2, 0.35, 0.2, 0.05, dust);
            }
        }
    }

    private void exitDimension() {
        List<Player> nearbyPlayers = LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), 7, null);
        for (AbstractGame.Participant p : players){
            if (!p.equals(getParticipant())) {
                getPlayer().showPlayer(AbilityWar.getPlugin(), p.getPlayer());
                p.getPlayer().showPlayer(AbilityWar.getPlugin(), getPlayer());
                getPlayer().spawnParticle(Particle.SMOKE_NORMAL, p.getPlayer().getLocation().add(addVector), 500, 0.3, 0.8, 0.3, 0.05);
            }

            if (nearbyPlayers.contains(p.getPlayer())){
                float length = (float) p.getPlayer().getLocation().distance(getPlayer().getLocation());
                Unstable.apply(p, TimeUnit.TICKS, Math.round(length * 7) + 30);
            }
        }
    }
    private final ItemStack glass = new ItemStack(Material.BLACK_SHULKER_BOX);
    private void portalEffect() {
        ArrayList<Location> effectList = new ArrayList<>();

        for (int i = 0; i < 5; i++){
            Random random = new Random();
            Vector dir = new Vector(random.nextInt(15) - 7.5f, random.nextInt(6) - 1, random.nextInt(15) - 7.5f);
            effectList.add(getPlayer().getLocation().add(dir));
            getPlayer().spawnParticle(Particle.ITEM_CRACK, getPlayer().getLocation().add(dir), 50, 0.5, 0.5, 0.5, 0.3, glass);
            getPlayer().playSound(getPlayer().getLocation().add(dir), Sound.BLOCK_GLASS_BREAK, 0.1f, 0.7f);
        }

        for (int i = 0; i < effectList.size(); i++){
            NotUtil.drawLine(effectList.get(i),
                    effectList.get(i + 1 >= effectList.size() ? 0 : i + 1),
                    new Particle[] {
                            Particle.REDSTONE,
                            Particle.REDSTONE
                    },
                    2, 0.1, 0.05,
                    new Object[] {
                            new Particle.DustOptions(Color.WHITE, 1),
                            new Particle.DustOptions(Color.fromRGB(222, 255, 255), 1)
            });
        }
    }
}
