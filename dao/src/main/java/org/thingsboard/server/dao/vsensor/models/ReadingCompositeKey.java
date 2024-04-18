/**
* Özgün AY
*/
package org.thingsboard.server.dao.vsensor.models;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadingCompositeKey implements Serializable {

    @Transient
    private static final long serialVersionUID = -4089175869616037523L;

    private UUID id;
}
