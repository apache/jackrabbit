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
package org.apache.jackrabbit.core.query.lucene;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.jackrabbit.core.query.TextFilter;
import org.apache.jackrabbit.extractor.CompositeTextExtractor;
import org.apache.jackrabbit.extractor.DelegatingTextExtractor;
import org.apache.jackrabbit.extractor.EmptyTextExtractor;
import org.apache.jackrabbit.extractor.TextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backwards-compatible Jackrabbit text extractor component. This class
 * implements the following functionality:
 * <ul>
 *   <li>
 *     Parses the configured {@link TextExtractor} and {@link TextFilter}
 *     class names and instantiates the configured classes.
 *   </li>
 *   <li>
 *     Acts as the delegate extractor for any configured
 *     {@link DelegatingTextExtractor} instances.
 *   </li>
 *   <li>
 *     Maintains a {@link CompositeTextExtractor} instance that contains
 *     all the configured extractors and to which all text extraction calls
 *     are delegated.
 *   </li>
 *   <li>
 *     Creates a {@link TextFilterExtractor} adapter for a configured
 *     {@link TextFilter} instance when it is first used and adds that adapter
 *     to the composite extractor for use in text extraction.
 *   </li>
 *   <li>
 *     Logs a warning and creates a dummy {@link EmptyTextExtractor} instance
 *     for any unsupported content types when first detected. The dummy
 *     extractor is added to the composite extractor to prevent future
 *     warnings about the same content type.
 *   </li>
 * </ul>
 */
public class JackrabbitTextExtractor implements TextExtractor {

    /**
     * Logger instance.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(JackrabbitTextExtractor.class);

    /**
     * Set of content types that are known to be supported by the
     * composite extractor.
     */
    private final Set types = new HashSet();

    /**
     * Composite extractor used to for all text extration tasks. Contains
     * all the {@link TextExtractor} instances for directly supported content
     * types, the {@link TextFilterExtractor} adapters for backwards
     * compatibility with configured {@link TextFilter} instances that have
     * already been used, and the dummy {@link EmptyTextExtractor} instances
     * created for unsupported content types.
     */
    private final CompositeTextExtractor extractor =
        new CompositeTextExtractor();

    /**
     * Configured {@link TextFilter} instances. Used for backwards
     * compatibility with existing configuration files and {@link TextFilter}
     * implementations.
     */
    private final Collection filters = new ArrayList();

    /**
     * Creates a Jackrabbit text extractor containing the configured component
     * classes.
     *
     * @param classes configured {@link TextExtractor} (and {@link TextFilter})
     *                class names (space- or comma-separated)
     */
    public JackrabbitTextExtractor(String classes) {
        logger.debug("JackrabbitTextExtractor({})", classes);
        StringTokenizer tokenizer = new StringTokenizer(classes, ", \t\n\r\f");
        while (tokenizer.hasMoreTokens()) {
            String name = tokenizer.nextToken();
            try {
                Object object = Class.forName(name).newInstance();
                if (object instanceof DelegatingTextExtractor) {
                    ((DelegatingTextExtractor) object)
                        .setDelegateTextExtractor(this);
                }
                if (object instanceof TextExtractor) {
                    extractor.addTextExtractor((TextExtractor) object);
                } else if (object instanceof TextFilter) {
                    filters.add(object);
                } else {
                    logger.warn("Unknown text extractor class: {}", name);
                }
            } catch (ClassNotFoundException e) {
                logger.warn("Extractor class not found: " + name, e);
            } catch (LinkageError e) {
                logger.warn("Extractor dependency not found: " + name, e);
            } catch (IllegalAccessException e) {
                logger.warn("Extractor constructor not accessible: " + name, e);
            } catch (InstantiationException e) {
                logger.warn("Extractor instantiation failed: " + name, e);
            }
        }

        types.addAll(Arrays.asList(extractor.getContentTypes()));
    }

    //-------------------------------------------------------< TextExtractor >

    /**
     * Returns the content types that the component extractors are known
     * to support.
     *
     * @return supported content types
     */
    public String[] getContentTypes() {
        return extractor.getContentTypes(); // and then some
    }

    /**
     * Extracts the text content from the given binary stream. The given
     * content type is used to look up a configured text extractor to which
     * to delegate the request.
     * <p>
     * If a matching extractor is not found, then the configured text filters
     * searched for an instance that claims to support the given content type.
     * A text extractor adapter is created for that filter and saved in the
     * extractor map for future use before delegating the request to the
     * adapter.
     * <p>
     * If not even a text filter is found for the given content type, a warning
     * is logged and an empty text extractor is created for that content type
     * and saved in the extractor map for future use before delegating the
     * request to the empty extractor.
     *
     * @param stream binary stream
     * @param type content type
     * @param encoding character encoding, or <code>null</code>
     * @return reader for the text content of the binary stream
     * @throws IOException if the binary stream can not be read
     */
    public Reader extractText(InputStream stream, String type, String encoding)
            throws IOException {
        logger.debug("extractText(stream, {}, {})", type, encoding);
        if (!types.contains(type)) {
            Iterator iterator = filters.iterator();
            while (iterator.hasNext()) {
                TextFilter filter = (TextFilter) iterator.next();
                if (filter.canFilter(type)) {
                    types.add(type);
                    extractor.addTextExtractor(
                            new TextFilterExtractor(type, filter));
                    break;
                }
            }
        }

        if (!types.contains(type)) {
            logger.debug("Full text indexing of {} is not supported", type);
            types.add(type);
            extractor.addTextExtractor(new EmptyTextExtractor(type));
        }

        return extractor.extractText(stream, type, encoding);
    }

}
