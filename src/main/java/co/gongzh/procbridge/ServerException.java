package co.gongzh.procbridge;

import org.jetbrains.annotations.Nullable;

public final class ServerException extends RuntimeException {

    private static final String UNKNOWN_SERVER_ERROR = "unknown server error";

    ServerException(@Nullable String message) {
        super(message != null ? message : UNKNOWN_SERVER_ERROR);
    }

    ServerException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

}
