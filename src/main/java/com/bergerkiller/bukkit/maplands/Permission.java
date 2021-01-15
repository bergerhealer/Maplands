package com.bergerkiller.bukkit.maplands;

import com.bergerkiller.bukkit.common.permissions.PermissionEnum;
import org.bukkit.permissions.PermissionDefault;

public class Permission extends PermissionEnum {
    public static final Permission COMMAND_MAP = new Permission("maplands.command.map", PermissionDefault.OP, "Gives the player the maplands map item");
    public static final Permission COMMAND_GIVE = new Permission("maplands.command.give", PermissionDefault.OP, "Gives the maplands map item to another player");
    public static final Permission COMMAND_RENDER = new Permission("maplands.command.render", PermissionDefault.OP, "Allows a player to re-render a maplands map remotely");
    public static final Permission CHANGE_MAP = new Permission("maplands.changemap", PermissionDefault.OP, "Allows the player to make changes to the map in the interactive menu");

    private Permission(final String node, final PermissionDefault permdefault, final String desc) {
        super(node, permdefault, desc);
    }
}
