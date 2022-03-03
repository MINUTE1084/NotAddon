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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.Random;

@AbilityManifest(
        name = "블랙잭", rank = AbilityManifest.Rank.B, species = AbilityManifest.Species.HUMAN,
        explain = {
                "§7철괴 클릭 §8- §a카드 뽑기§f: 우 클릭 시 1~10 카드 중 한 장을 뽑습니다. $[COOLDOWN]",
                " 뽑은 카드의 숫자는 누적되고, 현재 누적된 숫자에 따라 효과를 받습니다.",
                " 누적 숫자가 21이 되면, 30초 후 누적 숫자가 초기화 됩니다.",
                "§6[§e누적 숫자 효과§6]",
                "§b홀수 : 타격 데미지 1.25배 §8/ §a짝수 : 피격 데미지 0.75배",
                "§621 : 피격 데미지 0.5배 + 타격 데미지 1.5배 §8/ §c21 초과 : 타격 데미지 0배"
        },
        summarize = {
                "§7철괴 우 클릭 §f시 1~10 카드 중 한 장을 뽑습니다. $[COOLDOWN]",
                "뽑은 카드의 숫자는 누적되고, 현재 누적된 숫자에 따라 효과를 받습니다.",
                "누적 숫자가 21이 되면, 30초 후 누적 숫자가 초기화 됩니다."
        })

public class Blackjack extends AbilityBase implements ActiveHandler {

    public Blackjack(AbstractGame.Participant participant) { super(participant); }

    public static final AbilitySettings.SettingObject<Integer> COOLDOWN =
            abilitySettings.new SettingObject<Integer>(Blackjack.class, "cooldown", 3,"# 쿨타임", "# 단위: 초") {
                @Override
                public boolean condition(Integer value) {
                    return value >= 0;
                }
                @Override
                public String toString() {
                    return Formatter.formatCooldown(getValue());
                }
            };

    private int currentNumber = 0;
    private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), "블랙잭", CooldownDecrease._25);
    private final Particle.DustOptions blackjackDust = new Particle.DustOptions(Color.ORANGE, 1);
    private final Particle.DustOptions oddDust = new Particle.DustOptions(Color.AQUA, 1);
    private final Particle.DustOptions evenDust = new Particle.DustOptions(Color.LIME, 1);
    private final Vector addVector = new Vector(0, 1, 0);

    AbstractGame.Participant.ActionbarNotification.ActionbarChannel ac = newActionbarChannel();
    AbstractGame.Participant.ActionbarNotification.ActionbarChannel cc = newActionbarChannel();

    private final AbilityTimer passive = new AbilityTimer() {
        @Override
        public void run(int count) {
            if (currentNumber > 21) {
                ac.update("§c누적 숫자 : " + currentNumber + " / 타격 데미지 0배");
                getPlayer().spawnParticle(Particle.SMOKE_NORMAL, getPlayer().getLocation().add(addVector), 6, 0.25, 0.5, 0.25, 0.05);
            }
            else if (currentNumber == 21) {
                ac.update("§6누적 숫자 : 21 / 피격 데미지 0.5배 + 타격 데미지 1.5배");
                getPlayer().spawnParticle(Particle.REDSTONE, getPlayer().getLocation().add(addVector), 3, 0.25, 0.5, 0.25, blackjackDust);
            }
            else if (currentNumber % 2 == 1) {
                ac.update("§b누적 숫자 : " + currentNumber + " / 타격 데미지 1.25배");
                getPlayer().spawnParticle(Particle.REDSTONE, getPlayer().getLocation().add(addVector), 3, 0.25, 0.5, 0.25,  oddDust);
            }
            else if (currentNumber > 0) {
                ac.update("§a누적 숫자 : " + currentNumber + " / 피격 데미지 0.75배");
                getPlayer().spawnParticle(Particle.REDSTONE, getPlayer().getLocation().add(addVector), 3, 0.25, 0.5, 0.25, evenDust);
            }
            else ac.update("§f누적 숫자 : " + currentNumber + " / 효과 없음");
        }
    }.setPeriod(TimeUnit.TICKS, 1).register();

    private final AbilityTimer reset = new AbilityTimer(SimpleTimer.TaskType.REVERSE, 30) {
        @Override
        public void run(int count) {
            cc.update("§a숫자 초기화까지 " + count + "초" );
        }

        @Override
        public void onEnd() {
            onSilentEnd();
        }

        @Override
        public void onSilentEnd() {
            getPlayer().sendMessage("§2[§a블랙잭§2] §a누적 숫자가 초기화 되었습니다.");
            getPlayer().playSound(getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1);
            currentNumber = 0;
            cc.unregister();
            cc = newActionbarChannel();
        }
    }.setPeriod(TimeUnit.SECONDS, 1).register();

    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            passive.start();
        }
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && !cool.isCooldown() && currentNumber < 21) {
            if (clickType == ClickType.RIGHT_CLICK) {
                Random random = new Random();
                int randomNumber = random.nextInt(9) + 1;
                currentNumber += randomNumber;
                getPlayer().sendMessage("§a뽑은 숫자 : " + randomNumber);
                CheckInt();
                if (currentNumber >= 21) reset.start();
                return cool.start();
            }
        }
        return false;
    }

    private void CheckInt(){
        if (currentNumber > 21) {
            getPlayer().sendMessage("§f[§c!§f] §c누적 숫자가 21을 초과했습니다.");
            getPlayer().sendMessage("§f[§c!§f] §c30초 후 누적 숫자가 초기화 됩니다.");
            getPlayer().playSound(getPlayer().getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5F, 1);
        }
        else if (currentNumber == 21) {
            getPlayer().sendMessage("§f[§6!§f] §6누적 숫자가 21입니다.");
            getPlayer().sendMessage("§f[§6!§f] §630초 후 누적 숫자가 초기화 됩니다.");
            getPlayer().playSound(getPlayer().getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5F, 1);
        }
        else if (currentNumber % 2 == 1) {
            getPlayer().sendMessage("§f[§b!§f] §b누적 숫자가 홀수입니다.");
            getPlayer().playSound(getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1);
        }
        else if (currentNumber > 0) {
            getPlayer().sendMessage("§f[§a!§f] §a누적 숫자가 짝수입니다.");
            getPlayer().playSound(getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1);
        }
    }

    @SubscribeEvent
    private void onAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager().getType().equals(EntityType.PLAYER) && e.getDamager().getName().equals(getPlayer().getName())) {
            double before = e.getDamage();
            if (currentNumber == 21) e.setDamage(before * 1.5);
            else if (currentNumber > 21) e.setDamage(0);
            else if (currentNumber % 2 == 1) e.setDamage(before * 1.25);
        } else if (e.getEntity().getType().equals(EntityType.PLAYER) && e.getEntity().getName().equals(getPlayer().getName())) {
            double before = e.getDamage();
            if (currentNumber == 21) e.setDamage(before * 0.5);
            else if (currentNumber % 2 == 0 && currentNumber > 0) e.setDamage(before * 0.75);
        }
    }
}
