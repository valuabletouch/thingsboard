/*
* Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.vsensor.update.dao;

import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.dao.Dao;

public interface AuditLogDao extends Dao<AuditLog> {

    void migrateAuditLogs();

}
