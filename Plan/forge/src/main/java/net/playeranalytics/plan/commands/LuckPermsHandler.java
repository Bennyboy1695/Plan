/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package net.playeranalytics.plan.commands;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.fml.ModList;

public enum LuckPermsHandler {
    INSTANCE;

    public LuckPerms getLuckPerms() {
        return ModList.get().isLoaded("luckperms") ? LuckPermsProvider.get() : null;
    }

    public boolean hasPermission(CommandSourceStack player, String permission) {
        User user = getLuckPerms().getPlayerAdapter(CommandSourceStack.class).getUser(player);
        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }
}
