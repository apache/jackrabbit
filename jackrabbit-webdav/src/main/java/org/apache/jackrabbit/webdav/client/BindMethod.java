package org.apache.jackrabbit.webdav.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandler;

import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.bind.BindInfo;

/**
 * Represents an HTTP BIND method.
 * 
 * @see <a href="http://webdav.org/specs/rfc5842.html#rfc.section.4">RFC 5842, Section 4</a>
 */
public class BindMethod extends AbstractWebdavMethod<Void> {

    private final BindInfo info;
    
    BindMethod(BindInfo info) {
        this.info = info;
    }

    @Override
    public Builder newRequestBuilder(URI uri) throws IOException {
        Builder builder = super.newRequestBuilder(uri);
        builder.method(DavMethods.METHOD_BIND, DavBodyPublishers.ofXmlSerializable(info));
        return builder;
    }

    @Override
    public BodyHandler<Void> newBodyResponseHandler() {
        return newMultiStatusAwareBodyHandler();
    }

    @Override
    protected boolean succeeded(int statusCode) {
        return statusCode == DavServletResponse.SC_OK || statusCode == DavServletResponse.SC_CREATED;
    }

}
