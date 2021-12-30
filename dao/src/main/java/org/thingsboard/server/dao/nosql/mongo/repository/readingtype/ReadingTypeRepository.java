/**
 * Özgün AY
 */
package org.thingsboard.server.dao.nosql.mongo.repository.readingtype;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.model.vsensor.ReadingTypeDocument;

@Service
public interface ReadingTypeRepository extends MongoRepository<ReadingTypeDocument, String> {
    public Optional<ReadingTypeDocument> findByCode(String code);
}
