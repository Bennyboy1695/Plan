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

import com.djrapitops.plan.gathering.cache.SessionCache;
import com.djrapitops.plan.gathering.domain.ActiveSession;
import com.djrapitops.plan.identification.ServerInfo;
import com.djrapitops.plan.settings.config.WorldAliasSettings;
import com.djrapitops.plan.storage.database.DBSystem;
import com.djrapitops.plan.storage.database.transactions.events.StoreWorldNameTransaction;
import com.djrapitops.plan.utilities.logging.ErrorContext;
import com.djrapitops.plan.utilities.logging.ErrorLogger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.playeranalytics.plan.gathering.listeners.ForgeListener;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.UUID;

/**
 * Event Listener for PlayerGameModeChangeEvents.
 *
 * @author Bennyboy1695
 */
@Singleton
public class GameModeChangeListener implements ForgeListener {

    private final WorldAliasSettings worldAliasSettings;
    private final ServerInfo serverInfo;
    private final DBSystem dbSystem;
    private final ErrorLogger errorLogger;

    private boolean isEnabled = false;
    private boolean wasRegistered = false;

    @Inject
    public GameModeChangeListener(
            WorldAliasSettings worldAliasSettings,
            ServerInfo serverInfo,
            DBSystem dbSystem,
            ErrorLogger errorLogger
    ) {
        this.worldAliasSettings = worldAliasSettings;
        this.serverInfo = serverInfo;
        this.dbSystem = dbSystem;
        this.errorLogger = errorLogger;
    }

    public void onGameModeChange(ServerPlayer player, GameType newGameMode) {
        try {
            actOnEvent(player, newGameMode);
        } catch (Exception e) {
            errorLogger.error(e, ErrorContext.builder().related(getClass(), player, newGameMode).build());
        }
    }

    private void actOnEvent(ServerPlayer player, GameType newGameMode) {
        UUID uuid = player.getUUID();
        long time = System.currentTimeMillis();
        String gameMode = newGameMode.name();
        String worldName = player.serverLevel().dimension().location().toString();

        dbSystem.getDatabase().executeTransaction(new StoreWorldNameTransaction(serverInfo.getServerUUID(), worldName));
        worldAliasSettings.addWorld(worldName);

        Optional<ActiveSession> cachedSession = SessionCache.getCachedSession(uuid);
        cachedSession.ifPresent(session -> session.changeState(worldName, gameMode, time));
    }

    @Override
    public void register() {
        if (this.wasRegistered) {
            return;
        }

        MinecraftForge.EVENT_BUS.addListener(event -> {
            if (event instanceof PlayerEvent.PlayerChangeGameModeEvent changeGameModeEvent) {
                if (!isEnabled) {
                    return;
                }

                onGameModeChange((ServerPlayer) changeGameModeEvent.getEntity(), changeGameModeEvent.getNewGameMode());
            }
        });

        this.enable();
        this.wasRegistered = true;
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
