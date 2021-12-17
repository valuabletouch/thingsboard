/**
 * Özgün AY
 */
package org.thingsboard.server.dao.nosql.mongo.repository.readingtype;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.javatuples.Pair;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.vsensor.ReadingType;
import org.thingsboard.server.common.data.vsensor.ReadingTypeService;
import org.thingsboard.server.dao.model.vsensor.ReadingTypeDocument;

@Component
public class ReadingTypeServiceImpl implements ReadingTypeService {
    public static final String CACHE_NAME = "readingTypes";
    public static final long CACHE_TTL = 15 * 60 * 1000;
    public static final long CACHE_EVICT_PERIOD = 60 * 1000;

    private static List<Pair<String, LocalDateTime>> cacheExpireList = new ArrayList<Pair<String, LocalDateTime>>();

    @Autowired
    CacheManager cacheManager;

    @Autowired
    ReadingTypeRepository repository;

    @Cacheable(value = CACHE_NAME)
    public Optional<ReadingType> findById(String id) {
        cacheExpireList.add(new Pair<String, LocalDateTime>(id, LocalDateTime.now().plusNanos(CACHE_TTL)));

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
        cacheExpireList.add(new Pair<String, LocalDateTime>(code, LocalDateTime.now().plusNanos(CACHE_TTL)));

        Optional<ReadingTypeDocument> result = repository.findByCode(code);

        if (result.isPresent()) {
            ReadingTypeDocument readingTypeDocument = result.get();
            return Optional.of(new ReadingType(readingTypeDocument.getId(), readingTypeDocument.getCode()));
        } else {
            return Optional.empty();
        }
    }

    @Scheduled(fixedRate = CACHE_EVICT_PERIOD)
    public void evictExpired() {
        for (Pair<String, LocalDateTime> pair : cacheExpireList) {
            if (pair.getValue1().isBefore(LocalDateTime.now())) {
                cacheManager.getCache(CACHE_NAME).evict(pair.getValue0());

                cacheExpireList.remove(pair);
            }
        }
    }
}
