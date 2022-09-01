/**
 * Özgün AY
 */
package org.thingsboard.server.dao.vsensor.mongo.repository.transformation;

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
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.vsensor.TransformationService;
import org.thingsboard.server.dao.model.vsensor.TransformationDocument;

@Service
public class TransformationServiceImpl implements TransformationService {
    private static final String CACHE_NAME = "transformations";
    private static final long CACHE_TTL = 24 * 60 * 60l;
    private static final long CACHE_EVICT_PERIOD = 60 * 1000l;

    private static List<Pair<String, LocalDateTime>> cacheExpireList = new ArrayList<>();

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
            throw new RuntimeException("Transformation list is empty.");
        }

        TransformationDocument transformation = list.get(count - 1);

        if (transformation == null) {
            throw new RuntimeException("Transformation is null.");
        }

        Optional<UUID> uuid = Optional.of(fromString(transformation.getFromKey()));

        String key = toSystemKey + ":" + toEntityKey + ":" + toKey + ":" + fromSystemKey + ":" + fromEntityKey;

        cacheExpireList.add(new Pair<>(key, LocalDateTime.now().plusSeconds(CACHE_TTL)));

        return uuid;
    }

    @Cacheable(cacheNames = CACHE_NAME, key = "#fromSystemKey + ':' + #fromEntityKey + ':' + #fromKey + ':' +  #toSystemKey + ':' +  #toEntityKey")
    public Optional<UUID> getToKey(String fromSystemKey, String fromEntityKey, String fromKey, String toSystemKey,
            String toEntityKey) {
        Optional<List<TransformationDocument>> result = repository
                .findByFromSystemKeyAndFromEntityKeyAndFromKeyAndToSystemKeyAndToEntityKey(fromSystemKey, fromEntityKey,
                        fromKey.replace("-", ""), toSystemKey, toEntityKey);

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

        String key = fromSystemKey + ":" + fromEntityKey + ":" + fromKey + ":" + toSystemKey + ":" + toEntityKey;

        cacheExpireList.add(new Pair<>(key, LocalDateTime.now().plusSeconds(CACHE_TTL)));

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
