/*
* Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.vsensor.update.configuration;

import lombok.Data;

@Data
@Deprecated
public class TbRuleEngineQueueSubmitStrategyConfiguration {

    private String type;
    private int batchSize;

}