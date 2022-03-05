package minute.notaddon.Factory;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.game.manager.AbilityList;
import minute.notaddon.Ability.*;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NotAbilityFactory {
    private static Map<String, Class<? extends AbilityBase>> registeredAbility = new HashMap<>();
    static {
        registerAbility(Blackjack.class);
        registerAbility(Guppy.class);
        registerAbility(OutsideWorld.class);
        registerAbility(TranceBall.class);
        registerAbility(StarForce.class);
        registerAbility(Multiverse.class);
        registerAbility(LastHit.class);
        registerAbility(QuestionMaster.class);
        registerAbility(Tanker.class);
        registerAbility(Warlock.class);
    }

    public static void registerAbility(Class<? extends AbilityBase> clazz) {
        if (!registeredAbility.containsValue(clazz)) {
            registeredAbility.put(clazz.getAnnotation(AbilityManifest.class).name(), clazz);
            AbilityFactory.registerAbility(clazz);
            AbilityList.registerAbility(clazz);
        } else {
            Bukkit.getConsoleSender().sendMessage("§f[§c!§f] §c능력 " + clazz.getName() + "는 등록할 수 없습니다.");
        }
    }

    public static void registerHiddenAbility(Class<? extends AbilityBase> clazz) {
        if (!registeredAbility.containsValue(clazz)) {
            registeredAbility.put(clazz.getAnnotation(AbilityManifest.class).name(), clazz);
            AbilityFactory.registerAbility(clazz);
        } else {
            Bukkit.getConsoleSender().sendMessage("§f[§c!§f] §c숨겨진 능력 " + clazz.getName() + "는 등록할 수 없습니다.");
        }
    }

    public static Set<String> load() { return registeredAbility.keySet(); }
}
