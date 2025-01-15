package org.apache.jackrabbit.webdav.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.ResponseInfo;
import java.util.Arrays;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.bind.BindInfo;
import org.apache.jackrabbit.webdav.client.methods.HttpLock;
import org.apache.jackrabbit.webdav.client.methods.XmlEntity;
import org.apache.jackrabbit.webdav.header.IfHeader;
import org.apache.jackrabbit.webdav.header.TimeoutHeader;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an HTTP LOCK method.
 * 
 * @see <a href="http://webdav.org/specs/rfc4918.html#rfc.section.9.10">RFC 4918, Section 9.10</a>
 */
public class LockMethod extends AbstractWebdavMethod<String> {

    private static final Logger LOG = LoggerFactory.getLogger(LockMethod.class);

    private final LockInfo info;
    private final boolean isRefresh;
    
    LockMethod(LockInfo info) {
        this.info = info;
        this.isRefresh = false;
    }

    // TODO: separate into two classes
    LockMethod(long timeout, Collection<LockInfo> lockTokens) {
        TimeoutHeader th = new TimeoutHeader(timeout);
        super.setHeader(th.getHeaderName(), th.getHeaderValue());
        IfHeader ifh = new IfHeader(lockTokens);
        super.setHeader(ifh.getHeaderName(), ifh.getHeaderValue());
        isRefresh = true;
    }
    
    @Override
    public Builder newRequestBuilder(URI uri) throws IOException {
        Builder builder = super.newRequestBuilder(uri);
        if (isRefresh) {
            builder.method(DavMethods.METHOD_LOCK, HttpRequest.BodyPublishers.noBody());
            TimeoutHeader th = new TimeoutHeader(timeout);
            builder.setHeader(th.getHeaderName(), th.getHeaderValue());
            IfHeader ifh = new IfHeader(lockTokens);
            builder.setHeader(ifh.getHeaderName(), ifh.getHeaderValue());
        } else {
            builder.method(DavMethods.METHOD_LOCK, DavBodyPublishers.ofXmlSerializable(info));
        }
        return builder;
    }

    @Override
    public BodyHandler<String> newBodyResponseHandler() {
        return newMultiStatusAwareBodyHandler(this::getLockToken);
    }

    private String getLockToken(HttpHeaders headers) {
        List<String> ltHeader = headers.allValues(DavConstants.HEADER_LOCK_TOKEN);
        if (ltHeader.size() != 1) {
            LOG.debug("Multiple 'Lock-Token' header fields in response for " + response. + ": " + Arrays.asList(ltHeader));
            return null;
        } else {
            String v = ltHeader.get(0).trim();
            if (!v.startsWith("<") || !v.endsWith(">")) {
                LOG.debug("Invalid 'Lock-Token' header field in response for " + getURI() + ": " + Arrays.asList(ltHeader));
                return null;
            } else {
                return v.substring(1, v.length() - 1);
            }
        }
    }

    @Override
    protected boolean succeeded(int statusCode) {
        return statusCode == DavServletResponse.SC_OK || statusCode == DavServletResponse.SC_CREATED;
    }

}
