/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import org.apache.log4j.Logger;
import org.apache.jackrabbit.core.state.PropertyState;

import javax.jcr.RepositoryException;
import java.util.Map;
import java.util.Iterator;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import sun.misc.Service;

/**
 * Implements a service that looks up {@link TextFilter} implementations that
 * are registered in a jar file as providers for
 * <code>META-INF/services/org.apache.jackrabbit.core.query.TextFilterService</code>
 * E.g. the jackrabbit jar file contains entries for some {@link TextFilter}
 * implementations such as {@link TextPlainTextFilter}. Custom
 * {@link TextFilter} implementations may be added to Jackrabbit by packaging
 * them into a jar file together with a
 * <code>META-INF/services/org.apache.jackrabbit.core.query.TextFilterService</code>
 * file that contains the names of the custom {@link TextFilter} classes. Those
 * filters are then automatically loaded by Jackrabbit on startup.
 * <p/>
 * See also: <a href="http://java.sun.com/products/jdk/1.3/docs/guide/jar/jar.html">
 * JAR File Specification</a>
 * <p/>
 * {@link TextFilter} implementations are asked if they can handle a certain
 * mime type ({@link TextFilter#canFilter(String)} and if one of them returns
 * <code>true</code> the text representation is created with {@link
 * TextFilter#doFilter(PropertyState)}
 */
public class TextFilterService {

    /** Hidden constructor. */
    private TextFilterService() { }

    /**
     * Logger instance for this class.
     */
    private static final Logger log = Logger.getLogger(TextFilterService.class);

    /**
     * List of all {@link TextFilter}s known to the system.
     */
    private static final List filters = new ArrayList();

    /**
     * Initializes the {@link #filters} list.
     */
    static {
        Iterator it = Service.providers(TextFilterService.class);
        while (it.hasNext()) {
            filters.add(it.next());
        }
    }

    /**
     * Extracts text from a binary property which claims to be of a certain
     * mime-type. This metod eventually calls
     * {@link TextFilter#doFilter(PropertyState, String)}.
     *
     * @param data     the binary data
     * @param mimeType the mime type
     * @param encoding the encoding of the binary data or <code>null</code> if
     *   the data does not have an encoding or it is unknown.
     * @return the extracted content
     * @throws RepositoryException if an error occurs while creating the index
     *                             layout. This includes the case where
     *                             <code>data</code> is not according to
     *                             <code>mimeType</code>.
     */
    public static Map extractText(PropertyState data,
                                  String mimeType,
                                  String encoding) throws RepositoryException {
        TextFilter filter = getFilter(mimeType);
        if (filter != null) {
            return filter.doFilter(data, encoding);
        } else {
            return Collections.EMPTY_MAP;
        }
    }

    /**
     * Looks up the {@link TextFilter} that can extract text from binary content
     * with a certain <code>mimeType</code>.
     *
     * @param mimeType the mime type of the content to filter.
     * @return the {@link TextFilter} indexer instance or <code>null</code> if
     *         there is no indexer that can handle content of
     *         <code>mimeType</code>.
     */
    private static TextFilter getFilter(String mimeType) {
        log.debug("Find TextFilter for mime-type: " + mimeType);
        for (Iterator it = filters.iterator(); it.hasNext();) {
            TextFilter filter = (TextFilter) it.next();
            if (filter.canFilter(mimeType)) {
                log.debug("Found TextFilter implementation: " + filter.getClass().getName());
                return filter;
            }
        }
        log.debug("No TextFilter found");
        return null;
    }
}
