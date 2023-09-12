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

import com.djrapitops.plan.gathering.cache.JoinAddressCache;
import com.djrapitops.plan.gathering.domain.event.PlayerJoin;
import com.djrapitops.plan.gathering.domain.event.PlayerLeave;
import com.djrapitops.plan.gathering.events.PlayerJoinEventConsumer;
import com.djrapitops.plan.gathering.events.PlayerLeaveEventConsumer;
import com.djrapitops.plan.identification.ServerInfo;
import com.djrapitops.plan.identification.ServerUUID;
import com.djrapitops.plan.storage.database.DBSystem;
import com.djrapitops.plan.storage.database.transactions.events.BanStatusTransaction;
import com.djrapitops.plan.storage.database.transactions.events.KickStoreTransaction;
import com.djrapitops.plan.utilities.logging.ErrorContext;
import com.djrapitops.plan.utilities.logging.ErrorLogger;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.playeranalytics.plan.gathering.ForgePlayerPositionTracker;
import net.playeranalytics.plan.gathering.domain.ForgePlayerData;
import net.playeranalytics.plan.gathering.listeners.ForgeListener;
import net.playeranalytics.plan.gathering.listeners.events.PlanForgeEvents;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class PlayerOnlineListener implements ForgeListener {

    private final PlayerJoinEventConsumer joinEventConsumer;
    private final PlayerLeaveEventConsumer leaveEventConsumer;
    private final JoinAddressCache joinAddressCache;

    private final ServerInfo serverInfo;
    private final DBSystem dbSystem;
    private final ErrorLogger errorLogger;
    private final MinecraftServer server;

    private final AtomicReference<String> joinAddress = new AtomicReference<>();

    private boolean isEnabled = false;
    private boolean wasRegistered = false;

    @Inject
    public PlayerOnlineListener(
            PlayerJoinEventConsumer joinEventConsumer,
            PlayerLeaveEventConsumer leaveEventConsumer,
            JoinAddressCache joinAddressCache, ServerInfo serverInfo,
            DBSystem dbSystem,
            ErrorLogger errorLogger,
            MinecraftServer server
    ) {
        this.joinEventConsumer = joinEventConsumer;
        this.leaveEventConsumer = leaveEventConsumer;
        this.joinAddressCache = joinAddressCache;
        this.serverInfo = serverInfo;
        this.dbSystem = dbSystem;
        this.errorLogger = errorLogger;
        this.server = server;
    }

    @Override
    public void register() {
        if (this.wasRegistered) {
            return;
        }

        MinecraftForge.EVENT_BUS.addListener(event -> {
            if (event instanceof PlayerEvent.PlayerLoggedInEvent playerEvent) {
                if (!isEnabled) {
                    return;
                }
                onPlayerJoin((ServerPlayer) playerEvent.getEntity());
            }
        });
        MinecraftForge.EVENT_BUS.addListener(event -> {
            if (event instanceof PlayerEvent.PlayerLoggedOutEvent playerEvent) {
                if (!isEnabled) {
                    return;
                }
                beforePlayerQuit((ServerPlayer) playerEvent.getEntity());
                onPlayerQuit((ServerPlayer) playerEvent.getEntity());
            }
        });
        MinecraftForge.EVENT_BUS.addListener(event -> {
            if (event instanceof PlanForgeEvents.PlayerKickEvent playerEvent) {
                if (!isEnabled) {
                    return;
                }
                for (ServerPlayer target : playerEvent.getTargets()) {
                    onPlayerKick(target);
                }
            }
        });
        MinecraftForge.EVENT_BUS.addListener(event -> {
            if (event instanceof PlanForgeEvents.PlayerLoginEvent playerEvent) {
                if (!isEnabled) {
                    return;
                }
                onPlayerLogin(playerEvent.getSocketAddress(), playerEvent.getGameProfile(), playerEvent.getComponent() != null);
            }
        });
        MinecraftForge.EVENT_BUS.addListener(event -> {
            if (event instanceof PlanForgeEvents.PlayerHandshakeEvent playerEvent) {
                if (!isEnabled) {
                    return;
                }
                onHandshake(playerEvent.getIntentionPacket());
            }
        });

        this.enable();
        this.wasRegistered = true;
    }

    private void onHandshake(ClientIntentionPacket packet) {
        try {
            if (packet.getIntention() == ConnectionProtocol.LOGIN) {
                String address = packet.getHostName();
                if (address != null && address.contains("\u0000")) {
                    address = address.substring(0, address.indexOf('\u0000'));
                }
                joinAddress.set(address);
            }
        } catch (Exception e) {
            errorLogger.error(e, ErrorContext.builder().related(getClass(), "onHandshake").build());
        }
    }

    public void onPlayerLogin(SocketAddress address, GameProfile profile, boolean banned) {
        try {
            UUID playerUUID = profile.getId();
            ServerUUID serverUUID = serverInfo.getServerUUID();

            joinAddressCache.put(playerUUID, joinAddress.get());

            dbSystem.getDatabase().executeTransaction(new BanStatusTransaction(playerUUID, serverUUID, banned));
        } catch (Exception e) {
            errorLogger.error(e, ErrorContext.builder().related(getClass(), address, profile, banned).build());
        }
    }

    public void onPlayerKick(ServerPlayer player) {
        try {
            UUID uuid = player.getUUID();
            if (ForgeAFKListener.afkTracker.isAfk(uuid)) {
                return;
            }

            dbSystem.getDatabase().executeTransaction(new KickStoreTransaction(uuid));
        } catch (Exception e) {
            errorLogger.error(e, ErrorContext.builder().related(getClass(), player).build());
        }
    }

    public void onPlayerJoin(ServerPlayer player) {
        try {
            actOnJoinEvent(player);
        } catch (Exception e) {
            errorLogger.error(e, ErrorContext.builder().related(getClass(), player).build());
        }
    }

    private void actOnJoinEvent(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        long time = System.currentTimeMillis();

        ForgeAFKListener.afkTracker.performedAction(playerUUID, time);

        joinEventConsumer.onJoinGameServer(PlayerJoin.builder()
                .server(serverInfo.getServer())
                .player(new ForgePlayerData(player, server, joinAddressCache.getNullableString(playerUUID)))
                .time(time)
                .build());
    }

    public void beforePlayerQuit(ServerPlayer player) {
        leaveEventConsumer.beforeLeave(PlayerLeave.builder()
                .server(serverInfo.getServer())
                .player(new ForgePlayerData(player, server, null))
                .time(System.currentTimeMillis())
                .build());
    }

    public void onPlayerQuit(ServerPlayer player) {
        try {
            actOnQuitEvent(player);
        } catch (Exception e) {
            errorLogger.error(e, ErrorContext.builder().related(getClass(), player).build());
        }
    }

    private void actOnQuitEvent(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        long time = System.currentTimeMillis();
        ForgeAFKListener.afkTracker.loggedOut(playerUUID, time);
        ForgePlayerPositionTracker.removePlayer(playerUUID);

        leaveEventConsumer.onLeaveGameServer(PlayerLeave.builder()
                .server(serverInfo.getServer())
                .player(new ForgePlayerData(player, server, null))
                .time(time)
                .build());
    }

    @Override
    public boolean isEnabled() {
        return this.isEnabled;
    }

    @Override
    public void enable() {
        this.isEnabled = true;
    }

    @Override
    public void disable() {
        this.isEnabled = false;
    }
}
