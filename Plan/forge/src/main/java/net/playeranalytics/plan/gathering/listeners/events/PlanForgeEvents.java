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
package net.playeranalytics.plan.gathering.listeners.events;

import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.playeranalytics.plan.PlanForge;

import java.net.SocketAddress;
import java.util.Collection;

public class PlanForgeEvents {

    public static void postPlanEnableEvent(PlanForge planForge) {
        PlanEnableEvent event = new PlanEnableEvent(planForge);
        MinecraftForge.EVENT_BUS.post(event);
    }

    public static void postMoveEvent(ServerGamePacketListenerImpl packetListener, ServerboundMovePlayerPacket movePlayerPacket) {
        PlayerMoveEvent event = new PlayerMoveEvent(packetListener, movePlayerPacket);
        MinecraftForge.EVENT_BUS.post(event);
    }

    public static void postLoginEvent(SocketAddress socketAddress, GameProfile gameProfile, Component component) {
        PlayerLoginEvent event = new PlayerLoginEvent(socketAddress, gameProfile, component);
        MinecraftForge.EVENT_BUS.post(event);
    }

    public static void postKickEvent(CommandSourceStack source, Collection<ServerPlayer> targets, Component reason) {
        PlayerKickEvent event = new PlayerKickEvent(source, targets, reason);
        MinecraftForge.EVENT_BUS.post(event);
    }

    public static void postHandshakeEvent(ClientIntentionPacket packet) {
        PlayerHandshakeEvent event = new PlayerHandshakeEvent(packet);
        MinecraftForge.EVENT_BUS.post(event);
    }

    public static class PlanEnableEvent extends Event {

        private final PlanForge plan;

        public PlanEnableEvent(PlanForge plan) {
            this.plan = plan;
        }

        public PlanForge getPlan() {
            return plan;
        }
    }


    public static class PlayerHandshakeEvent extends Event {

        private final ClientIntentionPacket intentionPacket;

        public PlayerHandshakeEvent(ClientIntentionPacket intentionPacket) {
            this.intentionPacket = intentionPacket;
        }

        public ClientIntentionPacket getIntentionPacket() {
            return intentionPacket;
        }
    }

    public static class PlayerKickEvent extends Event {

        private final CommandSourceStack source;
        private final Collection<ServerPlayer> targets;
        private final Component reason;

        public PlayerKickEvent(CommandSourceStack source, Collection<ServerPlayer> targets, Component reason) {
            this.source = source;
            this.targets = targets;
            this.reason = reason;
        }

        public CommandSourceStack getSource() {
            return source;
        }

        public Collection<ServerPlayer> getTargets() {
            return targets;
        }

        public Component getReason() {
            return reason;
        }
    }

    public static class PlayerLoginEvent extends Event {

        private final SocketAddress socketAddress;
        private final GameProfile gameProfile;
        private final Component component;

        public PlayerLoginEvent(SocketAddress socketAddress, GameProfile gameProfile, Component component) {
            this.socketAddress = socketAddress;
            this.gameProfile = gameProfile;
            this.component = component;
        }

        public SocketAddress getSocketAddress() {
            return socketAddress;
        }

        public GameProfile getGameProfile() {
            return gameProfile;
        }

        public Component getComponent() {
            return component;
        }
    }

    public static class PlayerMoveEvent extends Event {

        private final ServerGamePacketListenerImpl serverGamePacketListener;
        private final ServerboundMovePlayerPacket movePlayerPacket;

        public PlayerMoveEvent(ServerGamePacketListenerImpl serverGamePacketListener, ServerboundMovePlayerPacket movePlayerPacket) {
            this.serverGamePacketListener = serverGamePacketListener;
            this.movePlayerPacket = movePlayerPacket;
        }

        public ServerGamePacketListenerImpl getServerGamePacketListener() {
            return serverGamePacketListener;
        }

        public ServerboundMovePlayerPacket getMovePlayerPacket() {
            return movePlayerPacket;
        }
    }
}
