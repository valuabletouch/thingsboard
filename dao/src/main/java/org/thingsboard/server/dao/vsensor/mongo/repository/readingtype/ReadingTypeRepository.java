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
package org.thingsboard.server.dao.vsensor.mongo.repository.readingtype;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.vsensor.models.ReadingTypeDocument;

@Repository
public interface ReadingTypeRepository extends MongoRepository<ReadingTypeDocument, String> {
    public Optional<ReadingTypeDocument> findByCode(String code);
    public Optional<ReadingTypeDocument> findById(String id);
    public Optional<List<ReadingTypeDocument>> findAllByCodeIn(List<String> codes);
    public Optional<List<ReadingTypeDocument>> findAllByIdIn(List<String> ids);
}
