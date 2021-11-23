/**
 * Özgün AY
 */
package org.thingsboard.server.dao.model.vsensor;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;

@Document(collection = "Transformations")
@Data
@AllArgsConstructor
public class Transformation implements Serializable {

    @Id
    String id;
    String fromEntityKey;
    String toEntityKey;
    String fromSystemKey;
    String toSystemKey;
    String fromKey;
    String toKey;
}
