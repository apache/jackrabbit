package org.apache.jackrabbit.webdav.client;

import org.apache.jackrabbit.webdav.MultiStatus;

/** Exception thrown when a multi-status response indicating a failure is received. */
public class MultiStatusDavResponseException extends DavResponseException {

    private static final long serialVersionUID = 1L;

    private final MultiStatus multiStatus;
    public MultiStatusDavResponseException(MultiStatus multiStatus) {
        super(207, "Multi-Status response" + multiStatus.toString());
        this.multiStatus = multiStatus;
    }

    public MultiStatus getMultiStatus() {
        return multiStatus;
    }
}
