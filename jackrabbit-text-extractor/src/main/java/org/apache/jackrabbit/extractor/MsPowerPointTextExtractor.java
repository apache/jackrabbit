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

import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.util.LittleEndian;

import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;

/**
 * Text extractor for Microsoft PowerPoint presentations.
 */
public class MsPowerPointTextExtractor extends AbstractTextExtractor {

    /**
     * Force loading of dependent class.
     */
    static {
        POIFSReader.class.getName();
    }

    /**
     * Creates a new <code>MsPowerPointTextExtractor</code> instance.
     */
    public MsPowerPointTextExtractor() {
        super(new String[]{"application/vnd.ms-powerpoint",
                           "application/mspowerpoint"});
    }

    //-------------------------------------------------------< TextExtractor >

    /**
     * {@inheritDoc}
     */
    public Reader extractText(InputStream stream,
                              String type,
                              String encoding) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MsPowerPointListener listener = new MsPowerPointListener(baos);
            POIFSReader reader = new POIFSReader();
            reader.registerListener(listener);
            reader.read(stream);
            return new InputStreamReader(
                    new ByteArrayInputStream(baos.toByteArray()));
        } finally {
            stream.close();
        }
    }

    //------------------------------------------------< MsPowerPointListener >

    /**
     * Reader listener.
     */
    private class MsPowerPointListener implements POIFSReaderListener {
        private OutputStream os;

        MsPowerPointListener(OutputStream os) {
            this.os = os;
        }

        public void processPOIFSReaderEvent(POIFSReaderEvent event) {
            try {
                if (!event.getName().equalsIgnoreCase("PowerPoint Document")) {
                    return;
                }
                DocumentInputStream input = event.getStream();
                byte[] buffer = new byte[input.available()];
                input.read(buffer, 0, input.available());
                for (int i = 0; i < buffer.length - 20; i++) {
                    long type = LittleEndian.getUShort(buffer, i + 2);
                    long size = LittleEndian.getUInt(buffer, i + 4);
                    if (type == 4008) {
                        os.write(buffer, i + 4 + 1, (int) size + 3);
                        i = i + 4 + 1 + (int) size - 1;
                    }
                }
            } catch (Exception e) {

            }
        }
    }
}
