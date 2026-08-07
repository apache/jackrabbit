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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.hc.client5.http.entity.mime.FormBodyPart;
import org.apache.hc.client5.http.entity.mime.FormBodyPartBuilder;
import org.apache.hc.client5.http.entity.mime.InputStreamBody;
import org.apache.hc.client5.http.entity.mime.MimeField;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.core5.http.ContentType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Pins the wire format produced for a davex batch, which has to stay byte for byte what
 * HttpClient 4 wrote in {@code RFC6532} mode. The expectations below were taken from
 * HttpClient 4.5.14 output. The server matches parts by the JCR path in the {@code name}
 * parameter, so a non-ASCII name surviving verbatim is the point of the exercise
 * (JCR-4317); an HttpClient upgrade that changes any of this should fail here rather than
 * in the remoting conformance suite.
 */
public class Rfc6532MultipartEntityTest {

    private static final ContentType TEXT = ContentType.create("text/plain", "UTF-8");
    private static final ContentType BINARY = ContentType.create("jcr-value/binary", "UTF-8");

    private static FormBodyPart part(String name, String value, ContentType type, String encoding) {
        FormBodyPart part = FormBodyPartBuilder.create().setName(name)
                .setBody(new StringBody(value, type)).build();
        part.getHeader().addField(new MimeField("Content-Transfer-Encoding", encoding));
        return part;
    }

    private static String write(List<FormBodyPart> parts) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new Rfc6532MultipartEntity(parts, "B").writeTo(out);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    public void testNonAsciiPartNameSurvivesAsUtf8() throws Exception {
        String name = "/testroot/Test-ä/jcr:content/jcr:data";
        List<FormBodyPart> parts = new ArrayList<FormBodyPart>();
        FormBodyPart binary = FormBodyPartBuilder.create().setName(name)
                .setBody(new InputStreamBody(
                        new ByteArrayInputStream("XYZ".getBytes(StandardCharsets.UTF_8)), BINARY, name))
                .build();
        binary.getHeader().addField(new MimeField("Content-Transfer-Encoding", "binary"));
        parts.add(binary);

        Assert.assertEquals(
                "--B\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + name + "\"\r\n"
                + "Content-Type: jcr-value/binary; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: binary\r\n"
                + "\r\n"
                + "XYZ\r\n"
                + "--B--\r\n",
                write(parts));
    }

    @Test
    public void testDiffPartKeepsTypeAndEncoding() throws Exception {
        List<FormBodyPart> parts = new ArrayList<FormBodyPart>();
        parts.add(part(":diff", "+/testroot/Test-ä : {}", TEXT, "8bit"));

        Assert.assertEquals(
                "--B\r\n"
                + "Content-Disposition: form-data; name=\":diff\"\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: 8bit\r\n"
                + "\r\n"
                + "+/testroot/Test-ä : {}\r\n"
                + "--B--\r\n",
                write(parts));
    }

    /**
     * JCR names may contain quotes and backslashes, which have to be escaped in the
     * Content-Disposition parameters exactly as HttpClient 4 escaped them.
     */
    @Test
    public void testQuotesAndBackslashesInPartNameAreEscaped() throws Exception {
        List<FormBodyPart> parts = new ArrayList<FormBodyPart>();
        parts.add(part("/testroot/na\"me-with\\quote-ä", "v", TEXT, "8bit"));

        Assert.assertTrue(write(parts).contains(
                "name=\"/testroot/na\\\"me-with\\\\quote-ä\""));
    }

    @Test
    public void testContentTypeCarriesNoCharset() {
        // a charset here would make the server decode the UTF-8 part headers as something else
        Assert.assertEquals("multipart/form-data; boundary=B",
                new Rfc6532MultipartEntity(new ArrayList<FormBodyPart>(), "B").getContentType());
    }

    @Test
    public void testLengthIsKnownForStringPartsAndUnknownForStreams() throws Exception {
        List<FormBodyPart> strings = new ArrayList<FormBodyPart>();
        strings.add(part(":diff", "abc", TEXT, "8bit"));
        Rfc6532MultipartEntity known = new Rfc6532MultipartEntity(strings, "B");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        known.writeTo(out);
        Assert.assertEquals(out.size(), known.getContentLength());
        Assert.assertTrue(known.isRepeatable());
        Assert.assertFalse(known.isChunked());

        List<FormBodyPart> streams = new ArrayList<FormBodyPart>();
        streams.add(FormBodyPartBuilder.create().setName("bin")
                .setBody(new InputStreamBody(
                        new ByteArrayInputStream("XYZ".getBytes(StandardCharsets.UTF_8)), BINARY, "bin"))
                .build());
        Rfc6532MultipartEntity unknown = new Rfc6532MultipartEntity(streams, "B");
        Assert.assertEquals(-1, unknown.getContentLength());
        Assert.assertTrue(unknown.isChunked());
    }
}
