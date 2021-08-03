package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.useentity.WrappedPacketInUseEntity;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PacketPlayerAttack extends PacketListenerAbstract {

    public PacketPlayerAttack() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        if (event.getPacketId() == PacketType.Play.Client.USE_ENTITY) {
            WrappedPacketInUseEntity action = new WrappedPacketInUseEntity(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            if (player == null) return;

            if (action.getAction() == WrappedPacketInUseEntity.EntityUseAction.ATTACK) {
                Entity attackedEntity = action.getEntity();
                player.reach.checkReach(action.getEntityId());
                if (attackedEntity != null && (!(attackedEntity instanceof LivingEntity) || attackedEntity instanceof Player)) {
                    ItemStack heldItem = player.bukkitPlayer.getInventory().getItem(player.packetStateData.lastSlotSelected);
                    boolean hasKnockbackSword = heldItem != null && heldItem.getEnchantmentLevel(Enchantment.KNOCKBACK) > 0;
                    boolean isLegacyPlayer = player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_8);

                    // 1.8 players who are packet sprinting WILL get slowed
                    // 1.9+ players who are packet sprinting might not, based on attack cooldown
                    // Players with knockback enchantments always get slowed
                    if ((player.packetStateData.isPacketSprinting && isLegacyPlayer) || hasKnockbackSword) {
                        player.packetStateData.minPlayerAttackSlow += 1;
                        player.packetStateData.maxPlayerAttackSlow += 1;

                        // Players cannot slow themselves twice in one tick without a knockback sword
                        if (!hasKnockbackSword) {
                            player.packetStateData.minPlayerAttackSlow = 0;
                            player.packetStateData.maxPlayerAttackSlow = 1;
                        }
                    } else if (!isLegacyPlayer && player.packetStateData.isPacketSprinting) {
                        // 1.9+ player who might have been slowed, but we can't be sure
                        player.packetStateData.maxPlayerAttackSlow += 1;
                    }
                }
            }
        }
    }
}
