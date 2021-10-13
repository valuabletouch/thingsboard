/**
 * Özgün AY
 */
package org.thingsboard.server.dao.nosql.mongo;

import java.util.Optional;

import org.thingsboard.server.dao.model.vsensor.ReadingType;

public interface ReadingTypeService {

    Optional<ReadingType> findById(String id);
    
    Optional<ReadingType> findByCode(String code);

}
