package io.github.xfournet.jconfig.cli;

public class JConfigException extends RuntimeException {
    public JConfigException() {
    }

    public JConfigException(String message) {
        super(message);
    }

    public JConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public JConfigException(Throwable cause) {
        super(cause);
    }

    public JConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
