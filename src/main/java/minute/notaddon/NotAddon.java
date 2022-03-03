package minute.notaddon;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.addon.Addon;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.manager.AbilityList;
import minute.notaddon.Ability.*;
import minute.notaddon.Factory.NotAbilityFactory;
import minute.notaddon.Factory.NotEffectFactory;
import minute.notaddon.Factory.NotSynergyFactory;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

public final class NotAddon extends Addon implements Listener {
    @Override
    public void onEnable() {
        for (String s : NotAbilityFactory.load()) Bukkit.getConsoleSender().sendMessage("§6[§eNotAddon§6] §e능력 §6[" + s + "]§e이(가) 로드되었습니다.");
        for (String s : NotSynergyFactory.load()) Bukkit.getConsoleSender().sendMessage("§6[§eNotAddon§6] §e시너지 §6[" + s + "]§e이(가) 로드되었습니다.");
        for (String s : NotEffectFactory.load()) Bukkit.getConsoleSender().sendMessage("§6[§eNotAddon§6] §e효과 §6[" + s + "]§e이(가) 로드되었습니다.");

        Bukkit.getPluginManager().registerEvents(this, getPlugin());

        Bukkit.getConsoleSender().sendMessage("§aNotAddon (애드온 아님) 적용 완료!");
        Bukkit.getConsoleSender().sendMessage("§aNotAddon (애드온 아님) 개발자 디스코드 §6: §bMINUTE#4438");
    }

    @EventHandler()
    public void onGameCredit(GameCreditEvent e) {
        e.addCredit("§aNotAddon (애드온 아님)을 이용해주셔서 감사합니다!");
        e.addCredit("§aNotAddon (애드온 아님) 개발자 디스코드 : MINUTE#4438");
    }


}
