package org.apache.jackrabbit.webdav.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.HttpResponse.ResponseInfo;
import java.util.Optional;
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.BaseDavRequest;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Encapsulates functionality both to create HTTP requests as well as to parse responses of a specific WebDAV method.
 */
public abstract class AbstractWebdavMethod<T> {

    protected HttpRequest.Builder newRequestBuilder(URI uri) throws IOException {
        Builder builder = HttpRequest.newBuilder(uri);
        return builder;
    }

    /**
     * Parses the response in case of non-success status code and throws an exception.
     * Also potentially parses multi status response bodies and wraps them in a {@link MultiStatusDavResponseException}.
     * @throws MultiStatusDavResponseException in case of a multi status response
     * @throws DavResponseException in case of a non-success status code
     */
    protected HttpResponse.BodyHandler<Void> newMultiStatusAwareBodyHandler() {
        return newMultiStatusAwareBodyHandler(responseInfo -> {});
    }

    /**
     * Parses the response in case of non-success status code and throws an exception.
     * Also potentially parses multi status response bodies and wraps them in a {@link MultiStatusDavResponseException}.
     * @throws MultiStatusDavResponseException in case of a multi status response
     * @throws DavResponseException in case of a non-success status code
     */
    protected HttpResponse.BodyHandler<T> newMultiStatusAwareBodyHandler(Function<ResponseInfo, T> responseInfoConsumer) {
        return (responseInfo) -> {
            if (succeeded(responseInfo.statusCode())) {
                T response = responseInfoConsumer.apply(null);
                return BodySubscribers.replacing(response);
            } else if (responseInfo.statusCode() == DavServletResponse.SC_MULTI_STATUS) {
                // TODO: evaluate multi status response body also for successful status codes?
                return BodySubscribers.ofByteArrayConsumer(AbstractWebdavMethod::evaluateMultiStatusResponseBody);
            } else {
                throw new DavResponseException(responseInfo.statusCode(), "Unexpected status code: "); 
            }
        };
    }

    public abstract HttpResponse.BodyHandler<T> newBodyResponseHandler();

    private static void evaluateMultiStatusResponseBody(Optional<byte[]> responseBody) {
        if (!responseBody.isPresent()) {
            return;
        }
        // parse as document
        Document dom;
        try {
            dom = DomUtil.parseDocument(responseBody.get());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (SAXException e) {
            // TODO: IO exception?
            throw new IllegalStateException("Unexpected XML parsing error", e);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Unexpected XML parsing parse configuration error", e);
        }
        MultiStatus multiStatus = MultiStatus.createFromXml(dom.getDocumentElement());
        if (multiStatus.containsErrorStatus()) {
            throw new MultiStatusDavResponseException(multiStatus);
        }
    }

    /**
     * Determines whether the given response status code indicates a successful request. 
     * The default implementation treats all
     * 2xx status codes (<a href="http://webdav.org/specs/rfc7231.html#rfc.section.6.3">RFC 7231, Section 6.3</a>) as success.
     * Implementations can further restrict the accepted range of status codes.
     * @param statusCode
     * @return {@code true} in case the status code indicates a successful request, {@code false} otherwise
     */
    protected boolean succeeded(int statusCode) {
        return statusCode >= 200 && statusCode <= 300;
    }
}
