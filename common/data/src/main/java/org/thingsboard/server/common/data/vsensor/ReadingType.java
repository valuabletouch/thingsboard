/**
 * Özgün AY
 */
package org.thingsboard.server.common.data.vsensor;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReadingType implements Serializable {

    String id;
    String code;
}
