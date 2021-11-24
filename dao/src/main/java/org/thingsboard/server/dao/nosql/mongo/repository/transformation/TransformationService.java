/**
 * Özgün AY
 */
package org.thingsboard.server.dao.nosql.mongo.repository.transformation;

import java.util.Optional;
import java.util.UUID;

public interface TransformationService {

    Optional<UUID> getId(String fromSystemKey, String fromEntityKey, String toSystemKey, String toEntityKey, String fromKey);

}
