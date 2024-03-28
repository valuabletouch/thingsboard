/*
* Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.vsensor.update.service;

public interface DatabaseUpgradeService {
    void upgradeDatabase(String fromVersion) throws Exception;

    String getCurrentSchemeVersion() throws Exception;
}
