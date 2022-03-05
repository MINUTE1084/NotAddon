package minute.notaddon.Effect;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.function.Predicate;

@EffectManifest(name = "안정", displayName = "§d안정", method = ApplicationMethod.UNIQUE_LONGEST, type = {
}, description = {
        "회복 효율이 100% 증가합니다.",
        "불안정, 신속을 제외한 모든 효과를 제거합니다.",
        "신속 1을 부여받습니다."
})
public class Stable extends AbstractGame.Effect implements Listener {
    public static final EffectRegistry.EffectRegistration<Stable> registration = (EffectRegistry.EffectRegistration<Stable>) EffectRegistry.getRegistration(Stable.class);

    public static void apply(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        registration.apply(participant, timeUnit, duration);

    }

    private static final Predicate<AbstractGame.Effect> awEffectPredicate = effect -> {
        if (effect.getRegistration().getEffectClass().equals(Stable.class)) return false;
        if (effect.getRegistration().getEffectClass().equals(Unstable.class)) return false;
        else return true;
    };

    private static final Predicate<PotionEffect> potionEffectPredicate = effect -> {
        if (effect.getType().equals(PotionEffectType.SPEED)) return false;
        else return true;
    };

    private final AbstractGame.Participant participant;

    public Stable(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
        this.participant = participant;
        setPeriod(TimeUnit.TICKS, 2);
    }

    @Override
    protected void onStart() {
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        super.onStart();
    }

    @EventHandler
    public void onRegainHealth(EntityRegainHealthEvent e) {
        if (participant.getPlayer().equals(e.getEntity())) {
            e.setAmount(e.getAmount() * 2);
        }
    }

    @Override
    protected void run(int count) {
        participant.removeEffects(awEffectPredicate);
        participant.getPlayer().getActivePotionEffects().removeIf(potionEffectPredicate);
        participant.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 4, 0));
        super.run(count);
    }

    @Override
    protected void onEnd() {
        HandlerList.unregisterAll(this);
        super.onEnd();
    }

    @Override
    protected void onSilentEnd() {
        HandlerList.unregisterAll(this);
        super.onSilentEnd();
    }
}

