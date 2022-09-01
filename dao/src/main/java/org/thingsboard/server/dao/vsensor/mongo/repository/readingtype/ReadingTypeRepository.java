/**
 * Özgün AY
 */
package org.thingsboard.server.dao.vsensor.mongo.repository.readingtype;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.vsensor.ReadingTypeDocument;

@Repository
public interface ReadingTypeRepository extends MongoRepository<ReadingTypeDocument, String> {
    public Optional<ReadingTypeDocument> findByCode(String code);
}
