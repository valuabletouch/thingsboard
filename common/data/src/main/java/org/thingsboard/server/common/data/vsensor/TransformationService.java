/**
 * Özgün AY
 */
package org.thingsboard.server.common.data.vsensor;

import java.util.Optional;
import java.util.UUID;

public interface TransformationService {
    Optional<UUID> getFromKey(String toSystemKey, String toEntityKey, String toKey, String fromSystemKey, String fromEntityKey);

    Optional<UUID> getToKey(String fromSystemKey, String fromEntityKey, String fromKey, String toSystemKey, String toEntityKey);

}
