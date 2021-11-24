/**
 * Özgün AY
 */
package org.thingsboard.server.dao.nosql.mongo.repository.readingtype;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.model.vsensor.ReadingType;

@Service
public interface ReadingTypeRepository extends MongoRepository<ReadingType, String> {
    public Optional<ReadingType> findByCode(String code);
}
