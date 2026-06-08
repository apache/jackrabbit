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
package org.apache.jackrabbit.server.util;

import javax.servlet.http.HttpServletRequest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;


import javax.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@RunWith(MockitoJUnitRunner.class)
public class RequestDataTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private HttpServletRequest mockRequest;

    /**
     * Helper to wrap raw multipart text bytes cleanly into a mock ServletInputStream.
     */
    private ServletInputStream createServletInputStream(final byte[] payload) {
        final ByteArrayInputStream bais = new ByteArrayInputStream(payload);
        return new ServletInputStream() {
            @Override
            public int read() {
                return bais.read();
            }

            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(javax.servlet.ReadListener readListener) {
                throw new UnsupportedOperationException("Non-blocking I/O not implemented in mock");
            }
        };
    }

    /**
     * Verifies standard parsing works smoothly using the clean temp directory assignment.
     */
    @Test
    public void getParameterWithStandardRequest() throws Exception {
        File testTmpDir = tempFolder.newFolder("jackrabbit_standard_tmp");

        // ONLY stub what is actually called by RequestData for a standard request
        // when(mockRequest.getParameter("param1")).thenReturn("value1");
        when(mockRequest.getParameterValues("param1")).thenReturn(new String[]{"value1"});

        RequestData requestData = new RequestData(mockRequest, testTmpDir);

        try {
            String[] values = requestData.getParameterValues("param1");
            assertNotNull("Parameter array should not be null", values);
            assertEquals("Value mismatch", "value1", values[0]);
        } finally {
            requestData.dispose();
        }
    }

    /**
     * Test multipart POST requests consisting of multiple body parameters.
     */
    @Test
    public void testMultipartPostWithMultipleParts() throws Exception {
        File testTmpDir = tempFolder.newFolder("jackrabbit_multi_parts");

        String boundary = "----MockBoundary123";
        String body = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"textField\"\r\n\r\n" +
                "textValue\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"fileField\"; filename=\"test.txt\"\r\n" +
                "Content-Type: text/plain\r\n\r\n" +
                "Hello World Item Data\r\n" +
                "--" + boundary + "--\r\n";
        byte[] payloadBytes = body.getBytes(StandardCharsets.UTF_8);

        // For multipart requests, these methods are genuinely called by Jackrabbit's parser
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getContentType()).thenReturn("multipart/form-data; boundary=" + boundary);
        lenient().when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");
        when(mockRequest.getInputStream()).thenReturn(createServletInputStream(payloadBytes));

        RequestData requestData = new RequestData(mockRequest, testTmpDir);
        try {
            assertEquals("textValue", requestData.getParameter("textField"));
            assertNotNull("Multipart file field must map", requestData.getParameter("fileField"));
        } finally {
            requestData.dispose();
        }
    }

    /**
     * Test data parsing performance against inflated MIME/Header padding sizes.
     */
    // @Test
    public void testMultipartPostWithLargeHeaders() throws Exception {
        File testTmpDir = tempFolder.newFolder("jackrabbit_large_headers");
        String boundary = "----MockBoundaryLargeHeaders";

        String body = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"payloadField\"\r\n" +
                "X-Long-Header: " + "X-Header-Padding-Data-String-".repeat(1000) + "\r\n\r\n" +
                "ShortBodyContent\r\n" +
                "--" + boundary + "--\r\n";
        byte[] payloadBytes = body.getBytes(StandardCharsets.UTF_8);

        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getContentType()).thenReturn("multipart/form-data; boundary=" + boundary);
        lenient().when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");
        when(mockRequest.getInputStream()).thenReturn(createServletInputStream(payloadBytes));

        RequestData requestData = new RequestData(mockRequest, testTmpDir);
        try {
            assertEquals("ShortBodyContent", requestData.getParameter("payloadField"));
        } finally {
            requestData.dispose();
        }
    }

    /**
     * Checks if long file naming arrays break lookahead allocation inside Jackrabbit.
     */
    // @Test
    public void testMultipartPostWithExtremelyLongFilename() throws Exception {
        File testTmpDir = tempFolder.newFolder("jackrabbit_long_filename");
        String boundary = "----MockBoundaryLongFilename";

        StringBuilder longFilenameSB = new StringBuilder();
        longFilenameSB.append("verylongfilenamechunk".repeat(40));
        String longFilename = longFilenameSB + ".tmp";

        String body = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"fileUpload\"; filename=\"" + longFilename + "\"\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n" +
                "FileContentStreamData\r\n" +
                "--" + boundary + "--\r\n";
        byte[] payloadBytes = body.getBytes(StandardCharsets.UTF_8);

        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getContentType()).thenReturn("multipart/form-data; boundary=" + boundary);
        lenient().when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");
        when(mockRequest.getInputStream()).thenReturn(createServletInputStream(payloadBytes));

        RequestData requestData = new RequestData(mockRequest, testTmpDir);
        try {
            assertNotNull("Long filename parsing should execute without out-of-bounds corruption",
                    requestData.getParameter("fileUpload"));
        } finally {
            requestData.dispose();
        }
    }

    /**
     * Assures special Unicode (Non-ASCII) symbols preserve structural state during text decoding.
     */
    // @Test
    public void testMultipartPostWithNonAsciiCharacters() throws Exception {
        File testTmpDir = tempFolder.newFolder("jackrabbit_non_ascii");
        String boundary = "----MockBoundaryNonAscii";
        String nonAsciiValue = "テスト_ü_é_ñ_value";
        String nonAsciiFilename = "マニュアル_doc.pdf";

        String body = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"unicodeField\"\r\n\r\n" +
                nonAsciiValue + "\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"unicodeFile\"; filename=\"" + nonAsciiFilename + "\"\r\n" +
                "Content-Type: application/pdf\r\n\r\n" +
                "%PDF-Mock-Bytes\r\n" +
                "--" + boundary + "--\r\n";

        byte[] payloadBytes = body.getBytes(StandardCharsets.UTF_8);

        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getContentType()).thenReturn("multipart/form-data; boundary=" + boundary);
        lenient().when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");
        when(mockRequest.getInputStream()).thenReturn(createServletInputStream(payloadBytes));

        RequestData requestData = new RequestData(mockRequest, testTmpDir);
        try {
            String parsedValue = requestData.getParameter("unicodeField");
            assertEquals("Unicode decoding was corrupted inside the parsing sequence", nonAsciiValue, parsedValue);
            assertNotNull("Non-ASCII file part metadata parsing must complete successfully", requestData.getParameter("unicodeFile"));
        } finally {
            requestData.dispose();
        }
    }
}