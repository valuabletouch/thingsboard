/**
 * Özgün AY
 */
package org.thingsboard.server.dao.nosql.mongo.repository.transformation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.model.vsensor.Transformation;

@Service
public interface TransformationRepository extends MongoRepository<Transformation, String> {
    public Optional<List<Transformation>> findByFromSystemKeyAndFromEntityKeyAndToSystemKeyAndToEntityKeyAndFromKey(String fromSystemKey, String fromEntityKey, String toSystemKey, String toEntityKey, String FromKey);
}
