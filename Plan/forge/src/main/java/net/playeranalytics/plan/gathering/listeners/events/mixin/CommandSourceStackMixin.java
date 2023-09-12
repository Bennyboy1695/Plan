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
package net.playeranalytics.plan.gathering.listeners.events.mixin;

import com.djrapitops.plan.commands.use.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.playeranalytics.plan.commands.CommandManager;
import net.playeranalytics.plan.commands.use.ForgeMessageBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Mixin(value = CommandSourceStack.class)
public abstract class CommandSourceStackMixin implements CMDSender {

    @Override
    public boolean isPlayer() {
        return getPlayer().isPresent();
    }

    @Override
    public boolean supportsChatEvents() {
        return isPlayer();
    }

    @Shadow(aliases = {"m_288197_"})
    public abstract void sendSuccess(Supplier<Component> par1, boolean par2);

    @Shadow(aliases = {"m_81373_"})
    @Nullable
    public abstract Entity getEntity();

    @Override
    public MessageBuilder buildMessage() {
        return new ForgeMessageBuilder((CommandSourceStack) (Object) this);
    }

    @Override
    public Optional<String> getPlayerName() {
        return getPlayer().map(player -> player.getName().getString());
    }

    @Override
    public boolean hasPermission(String permission) {
        return CommandManager.checkPermission((CommandSourceStack) (Object) this, permission);
    }

    @Override
    public Optional<UUID> getUUID() {
        return getPlayer().map(Entity::getUUID);
    }

    @Override
    public void send(String message) {
        sendSuccess(() -> Component.literal(message), false);
    }

    @Override
    public ChatFormatter getFormatter() {
        return isConsole() ? new ConsoleChatFormatter() : new PlayerChatFormatter();
    }

    private boolean isConsole() {
        return getEntity() == null;
    }

    private Optional<ServerPlayer> getPlayer() {
        if (getEntity() instanceof ServerPlayer player) {
            return Optional.of(player);
        }
        return Optional.empty();
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(isConsole()) + getUUID().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CommandSourceStackMixin other)) return false;

        return isConsole() == other.isConsole()
                && getUUID().equals(other.getUUID());
    }
}
