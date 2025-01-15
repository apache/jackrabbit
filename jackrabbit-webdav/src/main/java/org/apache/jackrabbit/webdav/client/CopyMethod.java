package org.apache.jackrabbit.webdav.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavServletResponse;

/**
 * Represents an HTTP COPY method.
 * 
 * @see <a href="http://webdav.org/specs/rfc4918.html#rfc.section.9.8">RFC 4918, Section 9.8</a>
 */
public class CopyMethod extends AbstractWebdavMethod<Void> {

    private final URI dest;
    private final boolean overwrite;
    private final boolean shallow;
    
    CopyMethod(URI dest, boolean overwrite, boolean shallow) {
        this.dest = dest;
        this.overwrite = overwrite;
        this.shallow = shallow;
    }

    @Override
    public Builder newRequestBuilder(URI uri) throws IOException {
        Builder builder = super.newRequestBuilder(uri);
        builder.method(DavMethods.METHOD_COPY, HttpRequest.BodyPublishers.noBody());
        builder.setHeader(DavConstants.HEADER_DESTINATION, dest.toASCIIString());
        if (!overwrite) {
            builder.setHeader(DavConstants.HEADER_OVERWRITE, "F");
        }
        if (shallow) {
            builder.setHeader("Depth", "0");
        }
        return builder;
    }

    @Override
    public BodyHandler<Void> newBodyResponseHandler() {
        return newMultiStatusAwareBodyHandler();
    }

    @Override
    protected boolean succeeded(int statusCode) {
        return statusCode == DavServletResponse.SC_CREATED || statusCode == DavServletResponse.SC_NO_CONTENT;
    }

}
