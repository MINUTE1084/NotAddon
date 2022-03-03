package minute.notaddon.Factory;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.AbilityList;
import minute.notaddon.Ability.*;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NotSynergyFactory {
    private static Map<String, Class<? extends AbilityBase>> registeredSynergy = new HashMap<>();
    static {
    }

    public static void registerSynergy(Class<? extends AbilityBase> first, Class<? extends AbilityBase> second, Class<? extends Synergy> clazz) {
        if (!registeredSynergy.containsValue(clazz)) {
            registeredSynergy.put(clazz.getAnnotation(AbilityManifest.class).name(), clazz);
            SynergyFactory.registerSynergy(first, second, clazz);
            AbilityList.registerAbility(clazz);
        } else {
            Bukkit.getConsoleSender().sendMessage("§f[§c!§f] §c시너지 " + clazz.getName() + "는 등록할 수 없습니다.");
        }
    }

    public static Set<String> load() { return registeredSynergy.keySet(); }
}
