/**
 * Özgün AY
 */
package org.thingsboard.server.dao.vsensor.mongo.repository.transformation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.vsensor.TransformationDocument;

@Repository
public interface TransformationRepository extends MongoRepository<TransformationDocument, String> {
    public Optional<List<TransformationDocument>> findByFromSystemKeyAndFromEntityKeyAndFromKeyAndToSystemKeyAndToEntityKey(
            String fromSystemKey, String fromEntityKey, String fromKey, String toSystemKey, String toEntityKey);

    public Optional<List<TransformationDocument>> findByToSystemKeyAndToEntityKeyAndToKeyAndFromSystemKeyAndFromEntityKey(
            String toSystemKey, String toEntityKey, String toKey, String fromSystemKey, String fromEntityKey);
}
