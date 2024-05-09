/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
* Özgün AY
*/
package org.thingsboard.server.dao.vsensor.mongo.repository.transformation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.vsensor.models.TransformationDocument;

@Repository
public interface TransformationRepository extends MongoRepository<TransformationDocument, String> {
    public Optional<List<TransformationDocument>> findByFromSystemKeyAndFromEntityKeyAndFromKeyAndToSystemKeyAndToEntityKey(
            String fromSystemKey, String fromEntityKey, String fromKey, String toSystemKey, String toEntityKey);

    public Optional<List<TransformationDocument>> findByToSystemKeyAndToEntityKeyAndToKeyAndFromSystemKeyAndFromEntityKey(
            String toSystemKey, String toEntityKey, String toKey, String fromSystemKey, String fromEntityKey);
}
