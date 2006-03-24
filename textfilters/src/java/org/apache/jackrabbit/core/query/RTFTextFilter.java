/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.rtf.RTFEditorKit;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;

/**
 * Extracts texts from RTF documents.
 */
public class RTFTextFilter implements TextFilter {

    private RTFEditorKit rek = new RTFEditorKit();
    private DefaultStyledDocument doc = new DefaultStyledDocument();

    /**
     * @return <code>true</code> for <code>application/rtf</code>,
     *         <code>false</code> otherwise.
     */
    public boolean canFilter(String mimeType) {
        return "application/rtf".equalsIgnoreCase(mimeType);
    }

    /**
     * Returns a map with a single entry for field {@link FieldNames#FULLTEXT}.
     *
     * @param data     object containing RTF document data
     * @param encoding text encoding is not used, since it is specified in the
     *                 data.
     * @return a map with a single Reader value for field {@link
     *         FieldNames#FULLTEXT}.
     * @throws RepositoryException if data is a multi-value property or if the
     *                             content can't be extracted
     */
    public Map doFilter(PropertyState data, String encoding)
            throws RepositoryException {

        InternalValue[] values = data.getValues();
        if (values.length > 0) {
            try {
                BLOBFileValue blob = (BLOBFileValue) values[0].internalValue();

                doc.remove(0, doc.getLength());
                rek.read(blob.getStream(), doc, 0);
                String text = doc.getText(0, doc.getLength());

                Map result = new HashMap();
                result.put(FieldNames.FULLTEXT, new StringReader(text));
                return result;
            } catch (BadLocationException ble) {
                throw new RepositoryException(ble);
            } catch (IOException ioe) {
                throw new RepositoryException(ioe);
            } catch (IllegalStateException e) {
                throw new RepositoryException(e);
            }
        } else {
            // multi value not supported
            throw new RepositoryException("Multi-valued binary properties not supported.");
        }
    }
}
