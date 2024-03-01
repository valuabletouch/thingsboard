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
package org.thingsboard.server.dao.vsensor.mongo.repository.readingtype;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.javatuples.Pair;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.vsensor.ReadingType;
import org.thingsboard.server.common.data.vsensor.ReadingTypeService;
import org.thingsboard.server.dao.model.vsensor.ReadingTypeDocument;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReadingTypeServiceImpl implements ReadingTypeService {
    private static final String CACHE_NAME = "readingTypes";
    private static final long CACHE_TTL = 15 * 60;
    private static final long CACHE_EVICT_PERIOD = 60 * 1000L;

    private static final List<Pair<String, LocalDateTime>> cacheExpireList = new ArrayList<>();

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ReadingTypeRepository repository;

    @Cacheable(value = CACHE_NAME)
    public Optional<ReadingType> findById(String id) {
        cacheExpireList.add(new Pair<>(id, LocalDateTime.now().plusSeconds(CACHE_TTL)));

        Optional<ReadingTypeDocument> result = repository.findById(id);

        if (result.isPresent()) {
            ReadingTypeDocument readingTypeDocument = result.get();
            return Optional.of(new ReadingType(readingTypeDocument.getId(), readingTypeDocument.getCode()));
        } else {
            return Optional.empty();
        }
    }

    @Cacheable(value = CACHE_NAME)
    public Optional<ReadingType> findByCode(String code) {
        cacheExpireList.add(new Pair<>(code, LocalDateTime.now().plusSeconds(CACHE_TTL)));

        Optional<ReadingTypeDocument> result = repository.findByCode(code);

        if (result.isPresent()) {
            ReadingTypeDocument readingTypeDocument = result.get();

            return Optional.of(new ReadingType(readingTypeDocument.getId(), readingTypeDocument.getCode()));
        } else {
            log.error("No ReadingType found on repository by code: {}", code);

            return Optional.empty();
        }
    }

    @Scheduled(fixedRate = CACHE_EVICT_PERIOD)
    public void evictExpired() {
        for (Pair<String, LocalDateTime> pair : cacheExpireList) {
            if (pair.getValue1().isBefore(LocalDateTime.now())) {
                Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).evict(pair.getValue0());

                cacheExpireList.remove(pair);
            }
        }
    }
}