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
/**
* Özgün AY
*/
package org.thingsboard.server.dao.vsensor.mongo.repository.transformation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.vsensor.TransformationService;
import org.thingsboard.server.dao.vsensor.models.TransformationDocument;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TransformationServiceImpl implements TransformationService {
    private static final String CACHE_NAME = "transformations";
    private static final long CACHE_TTL = 24 * 60 * 60l;
    private static final long CACHE_EVICT_PERIOD = 60 * 1000l;

    private static List<AbstractMap.SimpleEntry<String, LocalDateTime>> cacheExpireList = new CopyOnWriteArrayList<>();

    @Autowired
    private TransformationRepository repository;

    @Autowired
    private CacheManager cacheManager;

    @Cacheable(cacheNames = CACHE_NAME, key = "#toSystemKey + ':' +  #toEntityKey + ':' + #toKey + ':' + #fromSystemKey + ':' + #fromEntityKey")
    public Optional<UUID> getFromKey(String toSystemKey, String toEntityKey, String toKey, String fromSystemKey,
            String fromEntityKey) {
        Optional<List<TransformationDocument>> result = repository
                .findByToSystemKeyAndToEntityKeyAndToKeyAndFromSystemKeyAndFromEntityKey(
                        toSystemKey, toEntityKey, toKey.replace("-", ""), fromSystemKey, fromEntityKey);

        if (!result.isPresent()) {
            throw new RuntimeException("No transformation found.");
        }

        List<TransformationDocument> list = result.get();

        int count = list.size();

        if (count == 0) {
            log.warn("Transformation list is empty for toSystemKey: {}, toEntityKey: {}, toKey: {}, fromSystemKey: {}, fromEntityKey: {}",
                    toSystemKey, toEntityKey, toKey, fromSystemKey, fromEntityKey);

            return Optional.empty();
        }

        TransformationDocument transformation = list.get(count - 1);

        if (transformation == null) {
            log.warn("Transformation is null for toSystemKey: {}, toEntityKey: {}, toKey: {}, fromSystemKey: {}, fromEntityKey: {}",
                    toSystemKey, toEntityKey, toKey, fromSystemKey, fromEntityKey);

            return Optional.empty();
        }

        Optional<UUID> uuid = Optional.of(fromString(transformation.getFromKey()));

        String key = toSystemKey + ":" + toEntityKey + ":" + toKey + ":" + fromSystemKey + ":" + fromEntityKey;

        cacheExpireList.add(new AbstractMap.SimpleEntry<>(key, LocalDateTime.now().plusSeconds(CACHE_TTL)));

        return uuid;
    }

    @Cacheable(cacheNames = CACHE_NAME, key = "#fromSystemKey + ':' + #fromEntityKey + ':' + #fromKey + ':' +  #toSystemKey + ':' +  #toEntityKey")
    public Optional<UUID> getToKey(String fromSystemKey, String fromEntityKey, String fromKey, String toSystemKey,
            String toEntityKey) {
        Optional<List<TransformationDocument>> result = repository
                .findByFromSystemKeyAndFromEntityKeyAndFromKeyAndToSystemKeyAndToEntityKey(fromSystemKey, fromEntityKey,
                        fromKey.replace("-", ""), toSystemKey, toEntityKey);

        if (!result.isPresent()) {
            log.warn("No transformation found for fromSystemKey: {}, fromEntityKey: {}, fromKey: {}, toSystemKey: {}, toEntityKey: {}",
                    fromSystemKey, fromEntityKey, fromKey, toSystemKey, toEntityKey);

            return Optional.empty();
        }

        List<TransformationDocument> list = result.get();

        int count = list.size();

        if (count == 0) {
            log.warn("Transformation list is empty for fromSystemKey: {}, fromEntityKey: {}, fromKey: {}, toSystemKey: {}, toEntityKey: {}",
                    fromSystemKey, fromEntityKey, fromKey, toSystemKey, toEntityKey);

            return Optional.empty();
        }

        TransformationDocument transformation = list.get(count - 1);

        if (transformation == null) {
            log.warn("Transformation is null for fromSystemKey: {}, fromEntityKey: {}, fromKey: {}, toSystemKey: {}, toEntityKey: {}",
                    fromSystemKey, fromEntityKey, fromKey, toSystemKey, toEntityKey);

            return Optional.empty();
        }

        Optional<UUID> uuid = Optional.of(fromString(transformation.getToKey()));

        String key = fromSystemKey + ":" + fromEntityKey + ":" + fromKey + ":" + toSystemKey + ":" + toEntityKey;

        cacheExpireList.add(new AbstractMap.SimpleEntry<>(key, LocalDateTime.now().plusSeconds(CACHE_TTL)));

        return uuid;
    }

    @Scheduled(fixedRate = CACHE_EVICT_PERIOD)
    public void evictExpired() {
        for (AbstractMap.SimpleEntry<String, LocalDateTime> pair : cacheExpireList) {
            if (pair.getValue().isBefore(LocalDateTime.now())) {
                cacheManager.getCache(CACHE_NAME).evict(pair.getKey());

                cacheExpireList.remove(pair);
            }
        }
    }

    private static UUID fromString(String text) {
        return UUID.fromString(text.replaceFirst(
                "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5"));
    }
}