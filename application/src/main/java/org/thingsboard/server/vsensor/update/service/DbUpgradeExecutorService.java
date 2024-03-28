/*
* Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.vsensor.update.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

@Component
@Profile("update")
public class DbUpgradeExecutorService extends DbCallbackExecutorService {

}
