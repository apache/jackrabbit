/*
 * Copyright 2005 The Apache Software Foundation.
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
package org.apache.jackrabbit.webdav.search;

import org.apache.log4j.Logger;
import org.jdom.*;

/**
 * <code>SearchRequest</code> parses the 'searchrequest' element of a SEARCH
 * request body and performs basic validation. Both query language and the
 * query itself can be access from the resulting object.<br>
 * NOTE: The query is expected to be represented by the text contained in the
 * Xml element specifying the query language, thus the 'basicsearch' defined
 * by the Webdav Search Internet Draft is not supported by this implementation.
 * <p/>
 *
 * Example of a valid 'searchrequest' body
 * <pre>
 * &lt;d:searchrequest xmlns:d="DAV:" jcr:="http://www.day.com/jcr/webdav/1.0" &gt;
 *    &lt;jcr:xpath>//sv:node[@sv:name='myapp:paragraph'][1]&lt;/jcr:xpath&gt;
 * &lt;/d:searchrequest&gt;
 * </pre>
 *
 * Would return the following values:
 * <pre>
 *    getLanguageName() -&gt; xpath
 *    getQuery()    -&gt; //sv:node[@sv:name='myapp:paragraph'][1]
 * </pre>
 *
 */
public class SearchRequest implements SearchConstants {

    private static Logger log = Logger.getLogger(SearchRequest.class);

    private final Element language;

    /**
     * Create a new <code>SearchRequest</code> from the specified element.
     *
     * @param searchRequest
     * @throws IllegalArgumentException if the element's name is other than
     * 'searchrequest' or if it does not contain a single child element specifying
     * the query language to be used.
     */
    public SearchRequest(Element searchRequest) {
        if (searchRequest == null || !XML_SEARCHREQUEST.equals(searchRequest.getName()))  {
            throw new IllegalArgumentException("The root element must be 'searchrequest'.");
        } else if (searchRequest.getChildren().size() != 1) {
            throw new IllegalArgumentException("A single child element is expected with the 'searchrequest'.");
        }
        Element child = (Element)searchRequest.getChildren().get(0);
        language = (Element) child.detach();
    }

    /**
     * Create a new <code>SearchRequest</code> from the specifying document
     * retrieved from the request body.
     *
     * @param searchDocument
     * @see #SearchRequest(Element)
     */
    public SearchRequest(Document searchDocument) {
        this(searchDocument.getRootElement());
    }

    /**
     * Returns the name of the query language to be used.
     *
     * @return name of the query language
     */
    public String getLanguageName() {
        return language.getName();
    }

    /**
     * Returns the namespace of the language specified with the search request element.
     *
     * @return namespace of the requestes language.
     */
    public Namespace getLanguageNameSpace() {
        return language.getNamespace();
    }

    /**
     * Return the query string.
     *
     * @return query string
     */
    public String getQuery() {
        return language.getText();
    }
}