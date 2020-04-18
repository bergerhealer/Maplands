package com.bergerkiller.bukkit.maplands;

import com.bergerkiller.bukkit.common.permissions.PermissionEnum;
import org.bukkit.permissions.PermissionDefault;

public class Permission extends PermissionEnum {
    public static final Permission COMMAND_MAP = new Permission("maplands.command.map", PermissionDefault.OP, "Gives the player the maplands map item");
    public static final Permission COMMAND_GIVE = new Permission("maplands.command.give", PermissionDefault.OP, "Gives the maplands map item to another player");

    private Permission(final String node, final PermissionDefault permdefault, final String desc) {
        super(node, permdefault, desc);
    }
}
