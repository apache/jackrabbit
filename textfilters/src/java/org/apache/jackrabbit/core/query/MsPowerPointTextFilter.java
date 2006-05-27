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
package org.apache.jackrabbit.core.query;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.util.LittleEndian;

/**
 * Extracts texts from MS PowerPoint document binary data. Taken from Jakarta Slide
 * class <code>org.apache.slide.extractor.MSPowerPointExtractor</code>
 */
public class MsPowerPointTextFilter implements TextFilter {

    /**
     * Force loading of dependent class.
     */
    static {
        POIFSReader.class.getName();
    }

	/**
	 * Reader
	 */
	private class MsPowerPointListener implements POIFSReaderListener {
		private OutputStream os;

		MsPowerPointListener(OutputStream os) {
			this.os = os;
		}

		public void processPOIFSReaderEvent(POIFSReaderEvent event) {
			try {
				if (!event.getName().equalsIgnoreCase("PowerPoint Document"))
					return;
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

	/**
	 * @return <code>true</code> for <code>application/vnd.ms-powerpoint</code>,
	 *         <code>false</code> otherwise.
	 */
	public boolean canFilter(String mimeType) {
		return "application/vnd.ms-powerpoint".equalsIgnoreCase(mimeType)
				|| "application/mspowerpoint".equalsIgnoreCase(mimeType);
	}

	/**
	 * Returns a map with a single entry for field {@link FieldNames#FULLTEXT}.
	 * 
	 * @param data
	 *            object containing MS PowerPoint document data.
	 * @param encoding
	 *            text encoding is not used, since it is specified in the data.
	 * @return a map with a single Reader value for field
	 *         {@link FieldNames#FULLTEXT}.
	 * @throws RepositoryException
	 *             if data is a multi-value property or it does not contain
	 *             valid MS PowerPoint document.
	 */
	public Map doFilter(PropertyState data, String encoding)
			throws RepositoryException {
		InternalValue[] values = data.getValues();

		if (values.length == 1) {
			final BLOBFileValue blob = (BLOBFileValue) values[0].internalValue();

            LazyReader reader = new LazyReader() {
                protected void initializeReader() throws IOException {
                    InputStream in;
                    try {
                        in = blob.getStream();
                    } catch (RepositoryException e) {
                        throw new IOException(e.getMessage());
                    }
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        MsPowerPointListener listener = new MsPowerPointListener(baos);
                        POIFSReader reader = new POIFSReader();
                        reader.registerListener(listener);
                        reader.read(in);

                        delegate = new InputStreamReader(
                            new ByteArrayInputStream(baos.toByteArray()));
                    } finally {
                        in.close();
                    }
                }
            };

            Map result = new HashMap();
            result.put(FieldNames.FULLTEXT, reader);

            return result;
		} else {
			// multi value not supported
			throw new RepositoryException(
					"Multi-valued binary properties not supported.");
		}
	}

}
