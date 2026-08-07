/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.spi2davex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.hc.client5.http.entity.mime.FormBodyPart;
import org.apache.hc.client5.http.entity.mime.MimeField;
import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;

/**
 * A multipart entity that writes part headers as UTF-8, as described by
 * <a href="https://www.rfc-editor.org/rfc/rfc6532">RFC 6532</a>.
 * <p>
 * The server matches parts by the JCR path carried in the {@code name} parameter of
 * {@code Content-Disposition}, so a part name has to survive verbatim (JCR-4317).
 * HttpClient 4 achieved this with {@code HttpMultipartMode.RFC6532}, but HttpClient 5
 * offers no equivalent through {@code MultipartEntityBuilder}: its {@code EXTENDED} and
 * {@code STRICT} modes replace a non-ASCII part name with {@code '?'}, and its
 * {@code LEGACY} mode preserves the name but omits the per-part {@code Content-Type} and
 * {@code Content-Transfer-Encoding} headers. HttpClient 5 also advertises
 * {@code charset=ISO-8859-1} on the entity content type, which makes a server decode
 * those UTF-8 header bytes as Latin-1.
 * <p>
 * This entity therefore does the framing itself. The parts are still built with
 * {@code FormBodyPartBuilder}, whose generated headers are byte for byte what HttpClient 4
 * produced; only the assembly differs. The output is byte identical to that of HttpClient 4
 * in {@code RFC6532} mode, which {@code Rfc6532MultipartEntityTest} verifies.
 */
final class Rfc6532MultipartEntity implements HttpEntity {

    private static final byte[] CR_LF = { '\r', '\n' };
    private static final byte[] TWO_HYPHENS = { '-', '-' };

    private final List<FormBodyPart> parts;
    private final String boundary;
    private final long contentLength;

    Rfc6532MultipartEntity(List<FormBodyPart> parts, String boundary) {
        this.parts = parts;
        this.boundary = boundary;
        this.contentLength = computeContentLength();
    }

    /**
     * @return the total length, or -1 as soon as any part has an unknown length, which
     *         matches what HttpClient 4 reported for a stream body
     */
    private long computeContentLength() {
        long total = 0;
        for (FormBodyPart part : parts) {
            long length = part.getBody().getContentLength();
            if (length < 0) {
                return -1;
            }
            total += length;
        }
        ByteArrayOutputStream framing = new ByteArrayOutputStream();
        try {
            writeTo(framing, false);
        } catch (IOException e) {
            return -1;
        }
        return total + framing.size();
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        writeTo(out, true);
    }

    private void writeTo(OutputStream out, boolean writeContent) throws IOException {
        byte[] boundaryBytes = boundary.getBytes(StandardCharsets.ISO_8859_1);
        for (FormBodyPart part : parts) {
            out.write(TWO_HYPHENS);
            out.write(boundaryBytes);
            out.write(CR_LF);
            for (MimeField field : part.getHeader()) {
                // the field body already carries any parameters, fully formatted
                out.write(field.getName().getBytes(StandardCharsets.UTF_8));
                out.write(':');
                out.write(' ');
                out.write(field.getBody().getBytes(StandardCharsets.UTF_8));
                out.write(CR_LF);
            }
            out.write(CR_LF);
            if (writeContent) {
                part.getBody().writeTo(out);
            }
            out.write(CR_LF);
        }
        out.write(TWO_HYPHENS);
        out.write(boundaryBytes);
        out.write(TWO_HYPHENS);
        out.write(CR_LF);
    }

    @Override
    public String getContentType() {
        // deliberately without a charset parameter, so that a server does not decode the
        // UTF-8 part headers as anything else
        return "multipart/form-data; boundary=" + boundary;
    }

    @Override
    public long getContentLength() {
        return contentLength;
    }

    @Override
    public boolean isRepeatable() {
        return contentLength != -1;
    }

    @Override
    public boolean isChunked() {
        return contentLength == -1;
    }

    @Override
    public boolean isStreaming() {
        return contentLength == -1;
    }

    @Override
    public String getContentEncoding() {
        return null;
    }

    @Override
    public InputStream getContent() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writeTo(buffer, true);
        return new ByteArrayInputStream(buffer.toByteArray());
    }

    @Override
    public Supplier<List<? extends Header>> getTrailers() {
        return null;
    }

    @Override
    public Set<String> getTrailerNames() {
        return Collections.emptySet();
    }

    @Override
    public void close() throws IOException {
        // the part bodies are closed by the caller via Utils.removeParts
    }
}
