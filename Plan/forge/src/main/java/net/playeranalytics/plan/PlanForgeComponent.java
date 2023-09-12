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
package net.playeranalytics.plan;

import com.djrapitops.plan.PlanPlugin;
import com.djrapitops.plan.PlanSystem;
import com.djrapitops.plan.commands.PlanCommand;
import com.djrapitops.plan.gathering.ServerShutdownSave;
import com.djrapitops.plan.modules.FiltersModule;
import com.djrapitops.plan.modules.PlatformAbstractionLayerModule;
import com.djrapitops.plan.modules.ServerCommandModule;
import com.djrapitops.plan.modules.SystemObjectProvidingModule;
import com.djrapitops.plan.utilities.logging.ErrorLogger;
import dagger.BindsInstance;
import dagger.Component;
import net.minecraft.server.MinecraftServer;
import net.playeranalytics.plan.identification.properties.ForgeServerProperties;
import net.playeranalytics.plan.modules.forge.ForgeServerPropertiesModule;
import net.playeranalytics.plan.modules.forge.ForgeSuperClassBindingModule;
import net.playeranalytics.plan.modules.forge.ForgeTaskModule;
import net.playeranalytics.plugin.PlatformAbstractionLayer;

import javax.inject.Singleton;

/**
 * Dagger component for constructing the required plugin systems on Forge.
 *
 * @author Bennyboy1695
 */
@Singleton
@Component(modules = {
        SystemObjectProvidingModule.class,
        PlatformAbstractionLayerModule.class,
        FiltersModule.class,

        ServerCommandModule.class,
        ForgeServerPropertiesModule.class,
        ForgeSuperClassBindingModule.class,
        ForgeTaskModule.class
})
public interface PlanForgeComponent {

    PlanCommand planCommand();

    PlanSystem system();

    ServerShutdownSave serverShutdownSave();

    ErrorLogger errorLogger();

    @Component.Builder
    interface Builder {

        @BindsInstance
        Builder plan(PlanPlugin plan);

        @BindsInstance
        Builder abstractionLayer(PlatformAbstractionLayer abstractionLayer);

        @BindsInstance
        Builder server(MinecraftServer server);

        @BindsInstance
        Builder serverProperties(ForgeServerProperties serverProperties);

        PlanForgeComponent build();
    }
}
