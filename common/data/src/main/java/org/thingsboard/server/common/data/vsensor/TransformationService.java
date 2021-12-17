/**
 * Özgün AY
 */
package org.thingsboard.server.common.data.vsensor;

import java.util.Optional;
import java.util.UUID;

public interface TransformationService {

    Optional<UUID> getId(String fromSystemKey, String fromEntityKey, String toSystemKey, String toEntityKey, String fromKey);

}
