/**
 * Özgün AY
 */
package org.thingsboard.server.dao.nosql.mongo;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.model.vsensor.ReadingType;

@Component
public class ReadingTypeServiceImpl implements ReadingTypeService {

    @Autowired
    ReadingTypeRepository repository;

    public Optional<ReadingType> findById(String id) {
        return repository.findById(id);
    }

    public Optional<ReadingType> findByCode(String code) {
        return repository.findByCode(code);
    }

}
