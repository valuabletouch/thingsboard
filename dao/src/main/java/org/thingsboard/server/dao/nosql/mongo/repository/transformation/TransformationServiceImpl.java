/**
 * Özgün AY
 */
package org.thingsboard.server.dao.nosql.mongo.repository.transformation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.javatuples.Pair;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.vsensor.TransformationService;
import org.thingsboard.server.dao.model.vsensor.TransformationDocument;

@Component
public class TransformationServiceImpl implements TransformationService {
    private static final String CACHE_NAME = "transformations";
    public static final long CACHE_TTL = 24 * 60 * 60 * 1000;
    private static final long CACHE_EVICT_PERIOD = 60 * 1000;

    private static List<Pair<String, LocalDateTime>> cacheExpireList = new ArrayList<Pair<String, LocalDateTime>>();

    @Autowired
    TransformationRepository repository;

    @Autowired
    CacheManager cacheManager;

    @Cacheable(cacheNames = CACHE_NAME, key = "#fromSystemKey + ':' + #fromEntityKey + ':' +  #toSystemKey + ':' +  #toEntityKey + ':' + #fromKey")
    public Optional<UUID> getId(String fromSystemKey, String fromEntityKey, String toSystemKey, String toEntityKey,
            String fromKey) {
        Optional<List<TransformationDocument>> result = repository
                .findByFromSystemKeyAndFromEntityKeyAndToSystemKeyAndToEntityKeyAndFromKey(fromSystemKey, fromEntityKey,
                        toSystemKey, toEntityKey, fromKey.replace("-", ""));

        if (!result.isPresent()) {
            throw new RuntimeException("No transformation found.");
        }

        List<TransformationDocument> list = result.get();

        int count = list.size();

        if (count == 0) {
            throw new RuntimeException("Transformation list is empty.");
        }

        TransformationDocument transformation = list.get(count - 1);

        if (transformation == null) {
            throw new RuntimeException("Transformation is null.");
        }

        Optional<UUID> uuid = Optional.of(fromString(transformation.getToKey()));

        String key = fromSystemKey + ":" + fromEntityKey + ":" + toSystemKey + ":" + toEntityKey + ":" + fromKey;

        cacheExpireList.add(new Pair<String, LocalDateTime>(key, LocalDateTime.now().plusNanos(CACHE_TTL)));

        return uuid;
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

    private static UUID fromString(String text) {
        return UUID.fromString(text.replaceFirst(
                "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5"));
    }
}
