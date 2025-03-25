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
/*
* Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.vsensor.upgrade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.targets.platform.SystemAdministratorsFilter;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.dao.notification.DefaultNotifications.DefaultNotification;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsValidator;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegacyJwtSettingsServiceImpl implements LegacyJwtSettingsService {

    private static final String YELLOW_COLOR = "#F9D916";

    private final AdminSettingsService adminSettingsService;
    private final Optional<TbClusterService> tbClusterService;
    private final Optional<NotificationCenter> notificationCenter;
    private final JwtSettingsValidator jwtSettingsValidator;

    @Value("${security.jwt.tokenExpirationTime:9000}")
    private Integer tokenExpirationTime;
    @Value("${security.jwt.refreshTokenExpTime:604800}")
    private Integer refreshTokenExpTime;
    @Value("${security.jwt.tokenIssuer:thingsboard.io}")
    private String tokenIssuer;
    @Value("${security.jwt.tokenSigningKey:thingsboardDefaultSigningKey}")
    private String tokenSigningKey;

    private volatile JwtSettings jwtSettings = null; //lazy init

    private static final DefaultNotification jwtSigningKeyIssue = DefaultNotification.builder()
            .name("JWT Signing Key issue notification")
            .type(NotificationType.GENERAL)
            .subject("WARNING: security issue")
            .text("The platform is configured to use default JWT Signing Key. Please change it on the security settings page")
            .icon("warning").color(YELLOW_COLOR)
            .button("Go to settings").link("/security-settings/general")
            .build();

    /**
     * Create JWT admin settings is intended to be called from Upgrade scripts only
     */
    @Override
    public void saveLegacyYmlSettings() {
        log.info("Saving legacy JWT admin settings from YML...");
        if (getJwtSettingsFromDb() == null) {
            saveJwtSettings(getJwtSettingsFromYml());
        }
    }

    @Override
    public void createRandomJwtSettings() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createRandomJwtSettings'");
    }

    @Override
    public JwtSettings saveJwtSettings(JwtSettings jwtSettings) {
        jwtSettingsValidator.validate(jwtSettings);
        final AdminSettings adminJwtSettings = mapJwtToAdminSettings(jwtSettings);
        final AdminSettings existedSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, ADMIN_SETTINGS_JWT_KEY);
        if (existedSettings != null) {
            adminJwtSettings.setId(existedSettings.getId());
        }

        log.info("Saving new JWT admin settings. From this moment, the JWT parameters from YAML and ENV will be ignored");
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminJwtSettings);

        tbClusterService.ifPresent(cs -> cs.broadcastEntityStateChangeEvent(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, ComponentLifecycleEvent.UPDATED));
        return reloadJwtSettings();
    }

    @Override
    public JwtSettings reloadJwtSettings() {
        log.trace("Executing reloadJwtSettings");
        return getJwtSettings(true);
    }

    @Override
    public JwtSettings getJwtSettings() {
        log.trace("Executing getJwtSettings");
        return getJwtSettings(false);
    }

    public JwtSettings getJwtSettings(boolean forceReload) {
        if (this.jwtSettings == null || forceReload) {
            synchronized (this) {
                if (this.jwtSettings == null || forceReload) {
                    JwtSettings result = getJwtSettingsFromDb();
                    if (result == null) {
                        result = getJwtSettingsFromYml();
                        log.warn("Loading the JWT settings from YML since there are no settings in DB. Looks like the upgrade script was not applied.");
                    }
                    if (isSigningKeyDefault(result)) {
                        log.warn("WARNING: The platform is configured to use default JWT Signing Key. " +
                                "This is a security issue that needs to be resolved. Please change the JWT Signing Key using the Web UI. " +
                                "Navigate to \"System settings -> Security settings\" while logged in as a System Administrator.");
                        notificationCenter.ifPresent(notificationCenter -> {
                            notificationCenter.sendGeneralWebNotification(TenantId.SYS_TENANT_ID, new SystemAdministratorsFilter(), jwtSigningKeyIssue.toTemplate(), null);
                        });
                    }
                    this.jwtSettings = result;
                }
            }
        }
        return this.jwtSettings;
    }

    private JwtSettings getJwtSettingsFromYml() {
        return new JwtSettings(this.tokenExpirationTime, this.refreshTokenExpTime, this.tokenIssuer, this.tokenSigningKey);
    }

    private JwtSettings getJwtSettingsFromDb() {
        AdminSettings adminJwtSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, ADMIN_SETTINGS_JWT_KEY);
        return adminJwtSettings != null ? mapAdminToJwtSettings(adminJwtSettings) : null;
    }

    private JwtSettings mapAdminToJwtSettings(AdminSettings adminSettings) {
        Objects.requireNonNull(adminSettings, "adminSettings for JWT is null");
        return JacksonUtil.treeToValue(adminSettings.getJsonValue(), JwtSettings.class);
    }

    private AdminSettings mapJwtToAdminSettings(JwtSettings jwtSettings) {
        Objects.requireNonNull(jwtSettings, "jwtSettings is null");
        AdminSettings adminJwtSettings = new AdminSettings();
        adminJwtSettings.setTenantId(TenantId.SYS_TENANT_ID);
        adminJwtSettings.setKey(ADMIN_SETTINGS_JWT_KEY);
        adminJwtSettings.setJsonValue(JacksonUtil.valueToTree(jwtSettings));
        return adminJwtSettings;
    }

    private boolean isSigningKeyDefault(JwtSettings settings) {
        return TOKEN_SIGNING_KEY_DEFAULT.equals(settings.getTokenSigningKey());
    }

}
