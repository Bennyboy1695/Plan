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
package net.playeranalytics.plan.gathering.listeners.forge;

import com.djrapitops.plan.gathering.afk.AFKTracker;
import com.djrapitops.plan.settings.config.PlanConfig;
import com.djrapitops.plan.utilities.logging.ErrorContext;
import com.djrapitops.plan.utilities.logging.ErrorLogger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.playeranalytics.plan.commands.CommandManager;
import net.playeranalytics.plan.commands.LuckPermsHandler;
import net.playeranalytics.plan.gathering.ForgePlayerPositionTracker;
import net.playeranalytics.plan.gathering.listeners.ForgeListener;
import net.playeranalytics.plan.gathering.listeners.events.PlanForgeEvents;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ForgeAFKListener implements ForgeListener {

    // Static so that /reload does not cause afk tracking to fail.
    static AFKTracker afkTracker;
    private final Map<UUID, Boolean> ignorePermissionInfo;
    private final ErrorLogger errorLogger;
    private boolean isEnabled = false;
    private boolean wasRegistered = false;

    @Inject
    public ForgeAFKListener(PlanConfig config, ErrorLogger errorLogger) {
        this.errorLogger = errorLogger;
        ignorePermissionInfo = new ConcurrentHashMap<>();

        ForgeAFKListener.assignAFKTracker(config);
    }

    private static void assignAFKTracker(PlanConfig config) {
        if (afkTracker == null) {
            afkTracker = new AFKTracker(config);
        }
    }

    public static AFKTracker getAfkTracker() {
        return afkTracker;
    }

    private void event(ServerPlayer player) {
        try {
            UUID uuid = player.getUUID();
            long time = System.currentTimeMillis();

            boolean ignored = ignorePermissionInfo.computeIfAbsent(uuid, keyUUID -> checkPermission(player, com.djrapitops.plan.settings.Permissions.IGNORE_AFK.getPermission()));
            if (ignored) {
                afkTracker.hasIgnorePermission(uuid);
                ignorePermissionInfo.put(uuid, true);
                return;
            } else {
                ignorePermissionInfo.put(uuid, false);
            }

            afkTracker.performedAction(uuid, time);
        } catch (Exception e) {
            errorLogger.error(e, ErrorContext.builder().related(ForgeAFKListener.class, player).build());
        }
    }

    private static boolean checkPermission(ServerPlayer player, String permission) {
        if (CommandManager.isPermissionsApiAvailable()) {
            return LuckPermsHandler.INSTANCE.hasPermission(player, permission);
        } else {
            return false;
        }
    }

    @Override
    public void register() {
        if (this.wasRegistered) {
            return;
        }

        MinecraftForge.EVENT_BUS.addListener(event -> {
            if (event instanceof ServerChatEvent chatEvent) {
                if (!isEnabled) {
                    return;
                }
                event(chatEvent.getPlayer());
            }
        });
        MinecraftForge.EVENT_BUS.addListener(event -> {
            if (event instanceof CommandEvent commandEvent) {
                if (!isEnabled) {
                    return;
                }
                    if (commandEvent.getParseResults().getContext().getSource().isPlayer()) {
                        ServerPlayer player = commandEvent.getParseResults().getContext().getSource().getPlayer();
                        event(player);
                        boolean isAfkCommand = commandEvent.getParseResults().getReader().getString().toLowerCase().startsWith("afk");
                        if (isAfkCommand) {
                            UUID uuid = player.getUUID();
                            afkTracker.usedAfkCommand(uuid, System.currentTimeMillis());
                        }
                    }
            }
        });

        MinecraftForge.EVENT_BUS.addListener(event -> {
            if (event instanceof PlayerEvent.PlayerLoggedOutEvent loggedOutEvent) {
                if (!isEnabled) {
                    return;
                }
                ignorePermissionInfo.remove(loggedOutEvent.getEntity().getUUID());
            }
        });

        MinecraftForge.EVENT_BUS.addListener(event -> {
            if (event instanceof PlanForgeEvents.PlayerMoveEvent moveEvent) {
                if (!isEnabled) {
                    return;
                }

                UUID playerUUID = moveEvent.getServerGamePacketListener().player.getUUID();
                double[] position = ForgePlayerPositionTracker.getPosition(playerUUID);
                double x = position[0];
                double y = position[1];
                double z = position[2];
                float yaw = (float) position[3];
                float pitch = (float) position[4];
                if (ForgePlayerPositionTracker.moved(playerUUID, moveEvent.getMovePlayerPacket().getX(x), moveEvent.getMovePlayerPacket().getY(y), moveEvent.getMovePlayerPacket().getZ(z), moveEvent.getMovePlayerPacket().getXRot(yaw), moveEvent.getMovePlayerPacket().getYRot(pitch))) {
                    event(moveEvent.getServerGamePacketListener().getPlayer());
                }
            }
        });

        this.enable();
        this.wasRegistered = true;
    }


    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void enable() {
        isEnabled = true;
    }

    @Override
    public void disable() {
        isEnabled = false;
    }
}
