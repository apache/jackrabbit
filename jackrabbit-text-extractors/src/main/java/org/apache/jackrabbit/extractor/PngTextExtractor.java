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
package org.apache.jackrabbit.extractor;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Text extractor for png/apng/mng images. This class extracts the text content
 * from tEXt chunks.
 * <p>can handle image with mime types
 * (<code>image/png, image/apng, image/mng</code>)
 */
public class PngTextExtractor extends AbstractTextExtractor {

    private static byte[] pngHeader = {-119, 80, 78, 71, 13, 10, 26, 10};
    private static byte[] mngHeader = {-119, 77, 78, 71, 13, 10, 26, 10};
    private static byte[] iendChunk = {73, 69, 78, 68};
    private static byte[] tEXtChunk = {116, 69, 88, 116};

    private static String separator = System.getProperty("line.separator");

    /**
     * Logger instance.
     */
    private static final Logger logger =
            LoggerFactory.getLogger(PngTextExtractor.class);

    /**
     * Creates a new <code>PngTextExtractor</code> instance.
     */
    public PngTextExtractor() {
        super(new String[]{"image/png", "image/apng", "image/mng"});
    }

    /**
     * Returns a reader for the text content of the given png image. Returns an
     * empty reader if the png document could not be parsed.
     *
     * @param stream   png image
     * @param type     ignored
     * @param encoding ignored
     * @return reader for the text content of the given png image, or an empty
     *         reader if the image could not be parsed
     * @throws IOException if the png image stream can not be closed
     */
    public Reader extractText(InputStream stream,
                              String type,
                              String encoding)
            throws IOException {
        try {
            CharArrayWriter writer = new CharArrayWriter();
            byte[] header = new byte[8];
            stream.read(header);
            if (!Arrays.equals(pngHeader, header) && (!Arrays.equals(mngHeader, header))) {
                return new StringReader("");
            }
            byte[] length = new byte[4];
            byte[] chunkType = new byte[4];

            stream.read(length);
            stream.read(chunkType);

            String sep = "";
            while (!Arrays.equals(chunkType, iendChunk)) {
                if (Arrays.equals(chunkType, tEXtChunk)) {
                    byte[] txtBytes = new byte[calcLen(length)];
                    stream.read(txtBytes);
                    int nullPos = findOffset(txtBytes, (byte) 0);
                    String key = new String(txtBytes, 0, nullPos, "ISO-8859-1");
                    String value = new String(txtBytes, nullPos + 1, txtBytes.length - (nullPos + 1), "ISO-8859-1");
                    writer.write(key);
                    writer.write(": ");
                    writer.write(value);
                    writer.write(sep);
                    sep = separator;
                } else {
                    stream.skip(calcLen(length));
                }

                stream.skip(4);
                stream.read(length);
                stream.read(chunkType);
            }
            return new CharArrayReader(writer.toCharArray());
        } catch (IOException e) {
            logger.warn("Failed to extract png text content", e);
            return new StringReader("");
        } finally {
            stream.close();
        }
    }

    private int calcLen(byte[] length) {
        int len = 0x00FF & length[0];
        len <<= 8;
        len |= 0x00FF & length[1];
        len <<= 8;
        len |= 0x00FF & length[2];
        len <<= 8;
        len |= 0x00FF & length[3];
        return len;
    }

    int findOffset(byte[] data, byte val) {
        for (int i = 0; i < data.length; i++) {
            if (data[i] == val) {
                return i;
            }
        }

        return -1;
    }
}
