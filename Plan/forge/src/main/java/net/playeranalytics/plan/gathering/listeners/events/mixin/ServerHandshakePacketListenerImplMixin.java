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

import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import net.playeranalytics.plan.gathering.listeners.events.PlanForgeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerHandshakePacketListenerImpl.class)
public class ServerHandshakePacketListenerImplMixin {

    @Inject(method = "handleIntention", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setListener(Lnet/minecraft/network/PacketListener;)V", ordinal = 0))
    public void onClientHandshakeFromNetwork(ClientIntentionPacket packet, CallbackInfo ci) {
        PlanForgeEvents.postHandshakeEvent(packet);
    }

}
