package minute.notaddon.Ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.bukkit.Material;
import org.bukkit.util.Vector;

@AbilityManifest(
        name = "템플릿", rank = AbilityManifest.Rank.SPECIAL, species = AbilityManifest.Species.OTHERS,
        explain = {
                "§7템플릿 §8- §a템플릿 §f템플릿 $[COOLDOWN]"
        },
        summarize = {
                "§7템플릿 §8- §a템플릿 §f템플릿 $[COOLDOWN]"
        })

public class _Template extends AbilityBase implements ActiveHandler {
    public _Template(AbstractGame.Participant participant) {
        super(participant);
    }

    public static final AbilitySettings.SettingObject<Integer> COOLDOWN =
            abilitySettings.new SettingObject<Integer>(_Template.class, "cooldown", 25, "# 쿨타임", "# 단위. 초") {
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

    private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), "템플릿", CooldownDecrease._25);
    private final Vector addVector = new Vector(0, 1, 0);
    private final AbilityTimer passive = new AbilityTimer() {
        @Override
        public void run(int count) {
        }
    }.setPeriod(TimeUnit.TICKS, 1).register();

    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            passive.start();
        }
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && !cool.isCooldown()) {
            if (clickType == ClickType.RIGHT_CLICK) {
                return cool.start();
            }
        }
        return false;
    }
}
