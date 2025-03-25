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
package org.thingsboard.server.common.data.vsensor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class Reading implements Serializable {

    private UUID tenantId;

    private UUID dataSourceId;

    private UUID readingTypeId;

    private String readAt;

    private String dataType;

    private Boolean valueBoolean;

    private Long valueLong;

    private BigDecimal valueDecimal;

    private String valueString;

    private String valueDatetime;

    private String valueJson;
}
