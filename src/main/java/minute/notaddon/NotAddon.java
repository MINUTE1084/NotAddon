package minute.notaddon;

import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.addon.Addon;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.manager.AbilityList;
import minute.notaddon.Ability.*;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class NotAddon extends Addon implements Listener {

    @Override
    public void onEnable() {
        AbilityFactory.registerAbility(Blackjack.class);
        AbilityList.registerAbility(Blackjack.class);

        AbilityFactory.registerAbility(Guppy.class);
        AbilityList.registerAbility(Guppy.class);

        AbilityFactory.registerAbility(OutsideWorld.class);
        AbilityList.registerAbility(OutsideWorld.class);

        AbilityFactory.registerAbility(TranceBall.class);
        AbilityList.registerAbility(TranceBall.class);

        AbilityFactory.registerAbility(StarForce.class);
        AbilityList.registerAbility(StarForce.class);

        Bukkit.getPluginManager().registerEvents(this, getPlugin());

        Bukkit.broadcastMessage("§aNotAddon (애드온 아님) 적용 완료!");
        Bukkit.broadcastMessage("§aNotAddon (애드온 아님) 개발자 디스코드 §6: §bMINUTE#4438");
    }

    @EventHandler()
    public void onGameCredit(GameCreditEvent e) {
        e.addCredit("§aNotAddon (애드온 아님) 적용 완료!");
        e.addCredit("§aNotAddon (애드온 아님) 개발자 디스코드 : MINUTE#4438");
    }
}
