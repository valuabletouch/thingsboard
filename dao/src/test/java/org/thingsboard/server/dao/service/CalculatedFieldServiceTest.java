/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.service;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.calculated_field.CalculatedField;
import org.thingsboard.server.common.data.calculated_field.CalculatedFieldConfig;
import org.thingsboard.server.common.data.calculated_field.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.dao.calculated_field.CalculatedFieldService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DaoSqlTest
public class CalculatedFieldServiceTest extends AbstractServiceTest {

    @Autowired
    private CalculatedFieldService calculatedFieldService;
    @Autowired
    private DeviceService deviceService;

    private ListeningExecutorService executor;

    @Before
    public void before() {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));
    }

    @After
    public void after() {
        executor.shutdownNow();
    }

    @Test
    public void testSaveCalculatedField() {
        Device device = createTestDevice();
        CalculatedField calculatedField = getCalculatedField(device.getId(), device.getId());
        CalculatedField savedCalculatedField = calculatedFieldService.save(calculatedField);

        assertThat(savedCalculatedField).isNotNull();
        assertThat(savedCalculatedField.getId()).isNotNull();
        assertThat(savedCalculatedField.getCreatedTime()).isGreaterThan(0);
        assertThat(savedCalculatedField.getTenantId()).isEqualTo(calculatedField.getTenantId());
        assertThat(savedCalculatedField.getEntityId()).isEqualTo(calculatedField.getEntityId());
        assertThat(savedCalculatedField.getType()).isEqualTo(calculatedField.getType());
        assertThat(savedCalculatedField.getName()).isEqualTo(calculatedField.getName());
        assertThat(savedCalculatedField.getConfiguration()).isEqualTo(calculatedField.getConfiguration());
        assertThat(savedCalculatedField.getVersion()).isEqualTo(calculatedField.getVersion());

        savedCalculatedField.setName("Test CF");

        CalculatedField updatedCalculatedField = calculatedFieldService.save(savedCalculatedField);

        assertThat(updatedCalculatedField).isEqualTo(savedCalculatedField);

        calculatedFieldService.deleteCalculatedField(tenantId, savedCalculatedField.getId());
    }

    @Test
    public void testSaveCalculatesFieldWithNonExistingDeviceId() {
        DeviceId deviceId = new DeviceId(UUID.fromString("038f8668-c9fd-4f00-8501-ce20f2f93c22"));
        CalculatedField calculatedField = getCalculatedField(deviceId, deviceId);

        assertThatThrownBy(() -> calculatedFieldService.save(calculatedField))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Device with id [" + calculatedField.getEntityId().getId() + "] does not exist.");
    }

    @Test
    public void testSaveCalculatedFieldWithExistingName() {
        Device device = createTestDevice();
        CalculatedField calculatedField = getCalculatedField(device.getId(), device.getId());
        calculatedFieldService.save(calculatedField);

        assertThatThrownBy(() -> calculatedFieldService.save(calculatedField))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Calculated Field with such name is already in exists!");
    }

    @Test
    public void testSaveCalculatedFieldWithExistingExternalId() {
        Device device = createTestDevice();
        CalculatedField calculatedField = getCalculatedField(device.getId(), device.getId());
        calculatedField.setExternalId(new CalculatedFieldId(UUID.fromString("2ef69d0a-89cf-4868-86f8-c50551d87ebe")));

        calculatedFieldService.save(calculatedField);

        calculatedField.setName("Test 2");
        assertThatThrownBy(() -> calculatedFieldService.save(calculatedField))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Calculated Field with such external id already exists!");
    }

    @Test
    public void testFindCalculatedFieldById() {
        CalculatedField savedCalculatedField = saveValidCalculatedField();
        CalculatedField fetchedCalculatedField = calculatedFieldService.findById(tenantId, savedCalculatedField.getId());

        assertThat(fetchedCalculatedField).isEqualTo(savedCalculatedField);

        calculatedFieldService.deleteCalculatedField(tenantId, savedCalculatedField.getId());
    }

    @Test
    public void testDeleteCalculatedField() {
        CalculatedField savedCalculatedField = saveValidCalculatedField();

        calculatedFieldService.deleteCalculatedField(tenantId, savedCalculatedField.getId());

        assertThat(calculatedFieldService.findById(tenantId, savedCalculatedField.getId())).isNull();
    }

    @Test
    public void testSaveCalculatedFieldLinkIfCalculatedFieldForSuchEntityExists() {
        CalculatedField savedCalculatedField = saveValidCalculatedField();
        CalculatedFieldLink calculatedFieldLink = getCalculatedFieldLink(savedCalculatedField);

        assertThatThrownBy(() -> calculatedFieldService.saveCalculatedFieldLink(tenantId, calculatedFieldLink))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Calculated Field for such entity id is already exists!");
    }

    private CalculatedField saveValidCalculatedField() {
        Device device = createTestDevice();
        CalculatedField calculatedField = getCalculatedField(device.getId(), device.getId());
        return calculatedFieldService.save(calculatedField);
    }

    private CalculatedField getCalculatedField(EntityId entityId, EntityId referencedEntityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setTenantId(tenantId);
        calculatedField.setEntityId(entityId);
        calculatedField.setType("Simple");
        calculatedField.setName("Test Calculated Field");
        calculatedField.setConfigurationVersion(1);
        calculatedField.setConfiguration(getCalculatedFieldConfig(referencedEntityId));
        calculatedField.setVersion(1L);
        return calculatedField;
    }

    private CalculatedFieldLink getCalculatedFieldLink(CalculatedField calculatedField) {
        CalculatedFieldLink calculatedFieldLink = new CalculatedFieldLink();
        calculatedFieldLink.setTenantId(tenantId);
        calculatedFieldLink.setEntityId(calculatedField.getEntityId());
//        calculatedFieldLink.setConfiguration(calculatedField.getConfiguration());
        calculatedFieldLink.setCalculatedFieldId(calculatedField.getId());
        return calculatedFieldLink;
    }

    private CalculatedFieldConfig getCalculatedFieldConfig(EntityId referencedEntityId) {
        CalculatedFieldConfig config = new CalculatedFieldConfig();

        CalculatedFieldConfig.Argument argument = new CalculatedFieldConfig.Argument();
        argument.setEntityId(referencedEntityId);
        argument.setType("TIME_SERIES");
        argument.setKey("temperature");

        config.setArguments(Map.of("T", argument));

        CalculatedFieldConfig.Output output = new CalculatedFieldConfig.Output();
        output.setType("TIME_SERIES");
        output.setExpression("T - (100 - H) / 5");

        config.setOutput(output);

        return config;
    }

    private Device createTestDevice() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Test");
        return deviceService.saveDevice(device);
    }

}
