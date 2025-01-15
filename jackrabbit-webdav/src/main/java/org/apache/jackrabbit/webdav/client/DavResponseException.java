package org.apache.jackrabbit.webdav.client;

/** Similar to DavException, but unchecked. */
public class DavResponseException extends RuntimeException   {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public DavResponseException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public DavResponseException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
