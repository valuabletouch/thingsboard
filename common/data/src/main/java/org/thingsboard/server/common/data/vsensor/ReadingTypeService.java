/**
 * Özgün AY
 */
package org.thingsboard.server.common.data.vsensor;

import java.util.Optional;

public interface ReadingTypeService {

    Optional<ReadingType> findById(String id);

    Optional<ReadingType> findByCode(String code);

}
