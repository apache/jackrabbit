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
import org.w3c.dom.Element;

/**
 * Factory methods for HTTP response body handlers. Body handlers are used to process the body of an HTTP response.
 */
public class DavBodyHandlers {
    
    
    /**
     * Returns a {@code BodyHandler<Path>} that returns a
     * {@link BodySubscriber BodySubscriber}{@code <Path>} obtained from
     * {@link BodySubscribers#ofFile(Path, OpenOption...)
     * BodySubscribers.ofFile(Path,OpenOption...)}.
     *
     * <p> When the {@code HttpResponse} object is returned, the body has
     * been completely written to the file, and {@link #body()} returns a
     * reference to its {@link Path}.
     *
     * <p> Security manager permission checks are performed in this factory
     * method, when the {@code BodyHandler} is created. Care must be taken
     * that the {@code BodyHandler} is not shared with untrusted code.
     *
     * @param file the file to store the body in
     * @param openOptions any options to use when opening/creating the file
     * @return a response body handler
     * @throws IllegalArgumentException if an invalid set of open options
     *          are specified
     * @throws SecurityException If a security manager has been installed
     *          and it denies {@link SecurityManager#checkWrite(String)
     *          write access} to the file.
     */
    public static BodyHandler<String> ofLockToken() {
        return (responseInfo) -> BodySubscribers.replacing(getLockToken(responseInfo.headers()));
    }


    static String getLockToken(HttpHeaders headers) {
        List<String> ltHeader = headers.allValues(DavConstants.HEADER_LOCK_TOKEN);
        if (ltHeader == null || ltHeader.length == 0) {
            return null;
        } else if (ltHeader.length != 1) {
            LOG.debug("Multiple 'Lock-Token' header fields in response for " + getURI() + ": " + Arrays.asList(ltHeader));
            return null;
        } else {
            String v = ltHeader[0].getValue().trim();
            if (!v.startsWith("<") || !v.endsWith(">")) {
                LOG.debug("Invalid 'Lock-Token' header field in response for " + getURI() + ": " + Arrays.asList(ltHeader));
                return null;
            } else {
                return v.substring(1, v.length() - 1);
            }
        }
    }
}
