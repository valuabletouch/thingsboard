/**
 * Özgün AY
 */
package org.thingsboard.server.dao.model.vsensor;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;

@Document(collection = "ReadingTypes")
@Data
@AllArgsConstructor
public class ReadingTypeDocument implements Serializable {

    @Id
    String id;
    String code;
}
