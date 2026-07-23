/*
 * Copyright 2026 WaterdogTEAM
 * Licensed under the GNU General Public License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.waterdogpe.transfer;

import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import dev.waterdog.waterdogpe.network.protocol.Signals;
import dev.waterdog.waterdogpe.network.protocol.handler.downstream.InitialHandler;
import dev.waterdog.waterdogpe.network.protocol.registry.FakeDefinitionRegistry;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.cloudburstmc.protocol.bedrock.packet.ItemComponentPacket;
import org.cloudburstmc.protocol.common.DefinitionRegistry;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ItemComponentRegistryTest {

    private TransferTestHarness harness;
    private InitialHandler handler;

    @BeforeEach
    void setUp() {
        this.harness = new TransferTestHarness();
        when(this.harness.loginData.getProtocol()).thenReturn(ProtocolVersion.MINECRAFT_PE_1_21_60);
        ClientConnection connection = this.harness.newDownstream(this.harness.newServer("items"));
        this.handler = new InitialHandler(this.harness.player, connection);
    }

    @AfterEach
    void tearDown() {
        this.harness.close();
    }

    @Test
    void refreshesCompleteRegistryWhenLaterComponentPacketIsSuppressed() {
        ItemDefinition firstItem = new SimpleItemDefinition("custom:first", 1000, true);
        ItemComponentPacket firstPacket = new ItemComponentPacket();
        firstPacket.getItems().add(firstItem);

        assertSame(PacketSignal.UNHANDLED, this.handler.handle(firstPacket));

        ItemDefinition phone = new SimpleItemDefinition("custom:phone", 2000, true);
        ItemDefinition food = new SimpleItemDefinition("custom:food", 2001, true);
        ItemComponentPacket secondPacket = new ItemComponentPacket();
        secondPacket.getItems().add(phone);
        secondPacket.getItems().add(food);

        assertSame(Signals.CANCEL, this.handler.handle(secondPacket));

        ArgumentCaptor<DefinitionRegistry<ItemDefinition>> captor = ArgumentCaptor.forClass(DefinitionRegistry.class);
        verify(this.harness.codecHelper, times(2)).setItemDefinitions(captor.capture());
        FakeDefinitionRegistry<ItemDefinition> registry =
                (FakeDefinitionRegistry<ItemDefinition>) captor.getAllValues().get(1);

        assertSame(phone, registry.getDefinition(2000));
        assertSame(phone, registry.getDefinition("custom:phone"));
        assertSame(food, registry.getDefinition(2001));
        assertSame(food, registry.getDefinition("custom:food"));
    }
}
