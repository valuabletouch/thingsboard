/*
Author Ahmet Ertuğrul KAYA
 */
package org.thingsboard.server.update.service;

public interface DatabaseUpgradeService {
    void upgradeDatabase(String fromVersion) throws Exception;

    String getCurrentSchemeVersion() throws Exception;
}
