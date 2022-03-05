package minute.notaddon.Ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import minute.notaddon.Effect.Unstable;
import minute.notaddon.NotUtil;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

@AbilityManifest(
        name = "갈고리 수집가", rank = AbilityManifest.Rank.A, species = AbilityManifest.Species.HUMAN,
        explain = {
                "§7패시브 §8- §a갈고리 수집 §f자신을 제외한 누군가가 '?'가 포함된 채팅을 쳤을 때, 갈고리를 수집합니다.",
                " §f갈고리는 최대 10개까지 수집가능합니다.",
                "§7철괴 우 클릭 §8- §a채팅 부검 §f갈고리 1개를 소모하여 주변 30칸 내 모든 플레이어를 자신의 방향으로 끌고옵니다.",
                "§7철괴 좌 클릭 §8- §a밴 §f갈고리를 모두 소모하여 주변 5칸 내 모든 플레이어에게 (소모한 갈고리 × 1.5)만큼 데미지를 줍니다."
        },
        summarize = {
                "§7자신을 제외한 누군가가 '?'를 치면, 갈고리를 1개 수집합니다.",
                "§7철괴 우 클릭 시 갈고리 1개를 소모해 주변 30칸 내 플레이어들을 끌고옵니다.",
                "§7철괴 좌 클릭 시 갈고리를 모두 소모해 주변 5칸 내 플레이어들에게 (소모한 갈고리 × 1.5)만큼 데미지를 줍니다.",
        })

public class QuestionMaster extends AbilityBase implements ActiveHandler {
    public QuestionMaster(AbstractGame.Participant participant) {
        super(participant);
    }

    AbstractGame.Participant.ActionbarNotification.ActionbarChannel cc = newActionbarChannel();

    private int questionStack = 0;
    private final Vector addVector = new Vector(0, 1, 0);
    private final AbilityTimer passive = new AbilityTimer() {
        @Override
        public void run(int count) {
            cc.update("§a수집한 갈고리 §f: " + questionStack + "개");
        }
    }.setPeriod(TimeUnit.TICKS, 1).register();

    protected void onUpdate(Update update) {
        if (update == Update.RESTRICTION_CLEAR) {
            passive.start();
        }
    }

    private final Particle[] particles = new Particle[]{ Particle.SMOKE_NORMAL };
    private final Object[] options = new Object[]{ null };
    private final ItemStack blood = new ItemStack(Material.REDSTONE_BLOCK);
    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && questionStack > 0) {
            List<Player> nearbyPlayers = LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), clickType == ClickType.RIGHT_CLICK ? 30 : 5, null);
            nearbyPlayers.remove(getPlayer());

            if (nearbyPlayers.size() > 0) {
                if (clickType == ClickType.RIGHT_CLICK) {
                    for (Player p : nearbyPlayers) {
                        Vector vec = getPlayer().getLocation().toVector().subtract(p.getLocation().toVector()).multiply(0.175);
                        vec.setY(vec.getY() + 0.6);
                        p.setVelocity(vec);

                        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.6f);

                        NotUtil.drawLine(getPlayer().getLocation().add(addVector), p.getLocation().add(addVector), particles, 10, 0.1, 0.01, options);
                    }

                    questionStack--;
                } else {
                    for (Player p : nearbyPlayers) {
                        p.damage(questionStack * 1.5f);

                        p.getWorld().spawnParticle(Particle.ITEM_CRACK, p.getEyeLocation(), 50, 0.1, 0.1, 0.1, 0.05, blood);
                        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.5f, 0.8f);

                        NotUtil.drawLine(getPlayer().getLocation().add(addVector), p.getLocation().add(addVector), particles, 10, 0.1, 0.01, options);
                    }

                    questionStack = 0;
                }
            }
        }
        return false;
    }

    @SubscribeEvent(priority = SubscribeEvent.Priority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e){
        if (!e.getPlayer().equals(getPlayer()) && e.getMessage().contains("?") && questionStack < 10) {
            questionStack++;
            getPlayer().spawnParticle(Particle.SMOKE_NORMAL, getPlayer().getLocation().add(addVector), 50, 0.5, 0.5, 0.5, 0.05);
            getPlayer().playSound(getPlayer().getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.25f, 2);
        }
    }
}
