package org.apache.jackrabbit.webdav.client;

import java.io.IOException;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.ByteBuffer;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import org.apache.http.StatusLine;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * Factory methods for HTTP response body handlers. Body handlers are used to process the body of an HTTP response.
 * @see {@link BodyHandler}
 */
public class DavBodyHandlers {

    private static final Logger LOG = LoggerFactory.getLogger(DavBodyHandlers.class);

    private DavBodyHandlers() {
        // prevent instantiation
    }

    public static BodyHandler<String> ofLockToken() {
        return (responseInfo) -> BodySubscribers.replacing(getLockToken(responseInfo.headers()));
    }

    // TODO: exception handling
    static String getLockToken(HttpHeaders headers) {
        List<String> ltHeader = headers.allValues(DavConstants.HEADER_LOCK_TOKEN);
        if (ltHeader.isEmpty()) {
            return null;
        } else if (ltHeader.size() != 1) {
            LOG.debug("Multiple 'Lock-Token' header fields in response {}", ltHeader);
            return null;
        } else {
            String v = ltHeader.get(0).trim();
            if (!v.startsWith("<") || !v.endsWith(">")) {
                LOG.debug("Invalid 'Lock-Token' header field in response: {}", v);
                return null;
            } else {
                return v.substring(1, v.length() - 1);
            }
        }
    }
}
