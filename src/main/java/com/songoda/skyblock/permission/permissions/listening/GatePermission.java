package com.songoda.skyblock.permission.permissions.listening;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.message.MessageManager;
import com.songoda.skyblock.permission.ListeningPermission;
import com.songoda.skyblock.permission.PermissionHandler;
import com.songoda.skyblock.permission.PermissionType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class GatePermission extends ListeningPermission {

    private final SkyBlock plugin;
    private final MessageManager messageManager;

    public GatePermission(SkyBlock plugin) {
        super("Gate", CompatibleMaterial.OAK_FENCE_GATE, PermissionType.GENERIC);
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @PermissionHandler
    public void onInteract(PlayerInteractEvent event) {


        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();
        CompatibleMaterial material = CompatibleMaterial.getMaterial(event.getClickedBlock());

        if (material == CompatibleMaterial.OAK_FENCE_GATE || material == CompatibleMaterial.ACACIA_FENCE_GATE
                || material == CompatibleMaterial.BIRCH_FENCE_GATE || material == CompatibleMaterial.DARK_OAK_FENCE_GATE
                || material == CompatibleMaterial.JUNGLE_FENCE_GATE || material == CompatibleMaterial.SPRUCE_FENCE_GATE)
            cancelAndMessage(event, player, plugin, messageManager);
    }
}
