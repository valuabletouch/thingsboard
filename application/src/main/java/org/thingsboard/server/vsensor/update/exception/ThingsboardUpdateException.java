/*
* Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.vsensor.update.exception;

import org.springframework.boot.ExitCodeGenerator;

public class ThingsboardUpdateException extends RuntimeException implements ExitCodeGenerator {

    public ThingsboardUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ThingsboardUpdateException(String message) {
        super(message);
    }

    public int getExitCode() {
        return 1;
    }

}