package minute.notaddon.Effect;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;
import java.util.function.Predicate;

@EffectManifest(name = "불안정", displayName = "§c불안정", method = ApplicationMethod.UNIQUE_LONGEST, type = {
}, description = {
        "안정 효과를 제거하며, 회복 효율이 50% 감소합니다.",
        "아이템 슬롯이 0.1초마다 무작위로 바뀝니다.",
        "화면이 불안정해집니다. (멀미 1)"
})
public class Unstable extends AbstractGame.Effect implements Listener {
    public static final EffectRegistry.EffectRegistration<Unstable> registration = (EffectRegistry.EffectRegistration<Unstable>) EffectRegistry.getRegistration(Unstable.class);

    public static void apply(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
        registration.apply(participant, timeUnit, duration);
    }

    private final AbstractGame.Participant participant;

    private static final Predicate<AbstractGame.Effect> effectPredicate = effect -> {
        if (effect.getRegistration().getEffectClass().equals(Stable.class)) return true;
        else return false;
    };

    public Unstable(AbstractGame.Participant participant, TimeUnit timeUnit, int duration) {
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
            e.setAmount(e.getAmount() * 0.5f);
        }
    }

    @Override
    protected void run(int count) {
        participant.getPlayer().getInventory().setHeldItemSlot(new Random().nextInt(9));
        participant.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 80, 0));
        participant.removeEffects(effectPredicate);
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

