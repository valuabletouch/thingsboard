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

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.install.*;
import org.thingsboard.server.service.install.update.CacheCleanupService;
import org.thingsboard.server.service.install.update.DataUpdateService;
import org.thingsboard.server.vsensor.upgrade.exception.ThingsboardUpgradeException;

import static org.thingsboard.server.vsensor.upgrade.service.DefaultDataUpdateService.getEnv;

import java.util.List;

@Service
@Profile("upgrade")
@Slf4j
public class ThingsboardUpgradeService {

    private final static String INSTALL_UPGRADE_ENV_NAME = "upgrade.upgrade";

    @Value("${" + INSTALL_UPGRADE_ENV_NAME + ":false}")
    private Boolean isUpgrade;

    @Value("${upgrade.from_version:1.2.3}")
    private String upgradeFromVersion;

    @Value("${state.persistToTelemetry:false}")
    private boolean persistToTelemetry;

    @Autowired
    private EntityDatabaseSchemaService entityDatabaseSchemaService;

    @Autowired
    private DatabaseUpgradeService databaseEntitiesUpgradeService;

    // @Autowired(required = false)
    // private DatabaseTsUpgradeService databaseTsUpgradeService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    @Autowired
    private DataUpdateService dataUpdateService;

    @Autowired
    private CacheCleanupService cacheCleanupService;

    @Autowired
    private InstallScripts installScripts;

    public void performInstall() {
        // TODO: Fix the upgrade process
        // try {
        //     if (!isUpgrade) {
        //         Throwable e = new Throwable("Value of " + INSTALL_UPGRADE_ENV_NAME + " is " + isUpgrade);
        //         throw new ThingsboardUpgradeException("Value of " + INSTALL_UPGRADE_ENV_NAME + " is not set to true", e);
        //     }

        //     upgradeFromVersion = databaseEntitiesUpgradeService.getCurrentSchemeVersion();

        //     List<String> versions = Lists.newArrayList("3.2.0", "3.2.1", "3.2.2", "3.3.2", "3.3.3", "3.3.4", "3.4.0",
        //             "3.4.1", "3.4.4", "3.5.0", "3.5.1", "3.6.0", "3.6.1", "3.6.2", "3.6.3", "3.6.4"); // Oldest to
        //                                                                                               // newest

        //     int index = versions.indexOf(upgradeFromVersion);

        //     if (index == -1) {
        //         Throwable e = new Throwable("Supported versions are " + versions);
        //         throw new ThingsboardUpgradeException("Current version is " + upgradeFromVersion + " and not supported",
        //                 e);
        //     }

        //     String upgradeToVersion = versions.get(versions.size() - 1);

        //     log.info("Starting ThingsBoard Upgrade from version {} to version {}", upgradeFromVersion,
        //             upgradeToVersion);

        //     cacheCleanupService.clearCache();

        //     for (int i = index; i < versions.size(); i++) {
        //         String version = versions.get(i);
        //         switch (version) {
        //             case "3.2.0":
        //                 log.info("Upgrading ThingsBoard from version 3.2.0 to 3.2.1 ...");
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.2.0");
        //                 break;
        //             case "3.2.1":
        //                 log.info("Upgrading ThingsBoard from version 3.2.1 to 3.2.2 ...");
        //                 if (databaseTsUpgradeService != null) {
        //                     databaseTsUpgradeService.upgradeDatabase("3.2.1");
        //                 }
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.2.1");
        //                 break;
        //             case "3.2.2":
        //                 log.info("Upgrading ThingsBoard from version 3.2.2 to 3.3.0 ...");
        //                 if (databaseTsUpgradeService != null) {
        //                     databaseTsUpgradeService.upgradeDatabase("3.2.2");
        //                 }
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.2.2");
        //                 systemDataLoaderService.createOAuth2Templates();
        //                 break;
        //             case "3.3.2":
        //                 log.info("Upgrading ThingsBoard from version 3.3.2 to 3.3.3 ...");
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.3.2");
        //                 dataUpdateService.updateData("3.3.2");
        //                 break;
        //             case "3.3.3":
        //                 log.info("Upgrading ThingsBoard from version 3.3.3 to 3.3.4 ...");
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.3.3");
        //                 break;
        //             case "3.3.4":
        //                 log.info("Upgrading ThingsBoard from version 3.3.4 to 3.4.0 ...");
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.3.4");
        //                 dataUpdateService.updateData("3.3.4");
        //                 break;
        //             case "3.4.0":
        //                 log.info("Upgrading ThingsBoard from version 3.4.0 to 3.4.1 ...");
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.4.0");
        //                 dataUpdateService.updateData("3.4.0");
        //                 break;
        //             case "3.4.1":
        //                 log.info("Upgrading ThingsBoard from version 3.4.1 to 3.4.4 ...");
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.4.1");
        //                 dataUpdateService.updateData("3.4.1");
        //                 break;
        //             case "3.4.4":
        //                 log.info("Upgrading ThingsBoard from version 3.4.4 to 3.5.0 ...");
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.4.4");
        //                 break;
        //             case "3.5.0":
        //                 log.info("Upgrading ThingsBoard from version 3.5.0 to 3.5.1 ...");
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.5.0");
        //                 break;
        //             case "3.5.1":
        //                 log.info("Upgrading ThingsBoard from version 3.5.1 to 3.6.0 ...");
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.5.1");
        //                 dataUpdateService.updateData("3.5.1");
        //                 systemDataLoaderService.updateDefaultNotificationConfigs(true);
        //                 break;
        //             case "3.6.0":
        //                 log.info("Upgrading ThingsBoard from version 3.6.0 to 3.6.1 ...");
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.6.0");
        //                 dataUpdateService.updateData("3.6.0");
        //                 break;
        //             case "3.6.1":
        //                 log.info("Upgrading ThingsBoard from version 3.6.1 to 3.6.2 ...");
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.6.1");
        //                 if (!getEnv("SKIP_IMAGES_MIGRATION", false)) {
        //                     installScripts.setUpdateImages(true);
        //                 } else {
        //                     log.info("Skipping images migration. Run the upgrade with fromVersion as '3.6.2-images' to migrate");
        //                 }
        //                 break;
        //             case "3.6.2":
        //                 log.info("Upgrading ThingsBoard from version 3.6.2 to 3.6.3 ...");
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.6.2");
        //                 systemDataLoaderService.updateDefaultNotificationConfigs(true);
        //                 break;
        //             case "3.6.3":
        //                 log.info("Upgrading ThingsBoard from version 3.6.3 to 3.6.4 ...");
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.6.3");
        //                 break;
        //             case "3.6.4":
        //                 log.info("Upgrading ThingsBoard from version 3.6.4 to 3.7.0 ...");
        //                 databaseEntitiesUpgradeService.upgradeDatabase("3.6.4");
        //                 dataUpdateService.updateData("3.6.4");
        //                 entityDatabaseSchemaService.createCustomerTitleUniqueConstraintIfNotExists();
        //                 systemDataLoaderService.updateDefaultNotificationConfigs(false);
        //                 systemDataLoaderService.updateJwtSettings();
        //                 //TODO DON'T FORGET to update switch statement in the CacheCleanupService if you need to clear the cache
        //                 break;
        //             default:
        //                 throw new ThingsboardUpgradeException(
        //                         "Unable to upgrade ThingsBoard, unsupported fromVersion: " + upgradeFromVersion);
        //         }
        //     }

        //     entityDatabaseSchemaService.createOrUpdateViewsAndFunctions();
        //     entityDatabaseSchemaService.createOrUpdateDeviceInfoView(persistToTelemetry);
        //     log.info("Updating system data...");
        //     dataUpdateService.upgradeRuleNodes();
        //     systemDataLoaderService.loadSystemWidgets();
        //     installScripts.loadSystemLwm2mResources();
        //     installScripts.loadSystemImages();
        //     if (installScripts.isUpdateImages()) {
        //         installScripts.updateImages();
        //     }
        //     log.info("Upgrade finished successfully!");

        // } catch (Exception e) {
        //     log.error("Unexpected error during ThingsBoard installation!", e);
        //     throw new ThingsboardUpgradeException("Unexpected error during ThingsBoard installation!", e);
        // } finally {
        //     SpringApplication.exit(context);
        // }
    }
}
