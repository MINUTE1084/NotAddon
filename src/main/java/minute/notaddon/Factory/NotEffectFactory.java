package minute.notaddon.Factory;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import minute.notaddon.Effect.*;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NotEffectFactory {
    private static Map<String, Class<? extends AbstractGame.Effect>> registeredEffect = new HashMap<>();
    static {
        registerEffect(Stable.class);
        registerEffect(Unstable.class);
    }

    public static void registerEffect(Class<? extends AbstractGame.Effect> clazz) {
        if (!registeredEffect.containsValue(clazz)) {
            registeredEffect.put(clazz.getAnnotation(EffectManifest.class).name(), clazz);
            EffectRegistry.registerEffect(clazz);
        } else {
            Bukkit.getConsoleSender().sendMessage("§f[§c!§f] §c능력 " + clazz.getName() + "는 등록할 수 없습니다.");
        }
    }

    public static Set<String> load() { return registeredEffect.keySet(); }
}
