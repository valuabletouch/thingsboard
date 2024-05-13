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
package org.thingsboard.server.vsensor.update.service;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.event.Event;

@Service
@Slf4j
public class LegacyJwtSettingsServiceImpl extends LegacyJwtSettingsService {

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

    void migrateEvents();

}
