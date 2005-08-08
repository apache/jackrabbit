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
 * <code>SearchInfo</code> parses the 'searchrequest' element of a SEARCH
 * request body and performs basic validation. Both query language and the
 * query itself can be access from the resulting object.<br>
 * NOTE: The query is expected to be represented by the text contained in the
 * Xml element specifying the query language, thus the 'basicsearch' defined
 * by the Webdav Search Internet Draft is not supported by this implementation.
 * <p/>
 *
 * Example of a valid 'searchrequest' body
 * <pre>
 * &lt;d:searchrequest xmlns:d="DAV:" dcr:="http://www.day.com/jcr/webdav/1.0" &gt;
 *    &lt;dcr:xpath>//sv:node[@sv:name='myapp:paragraph'][1]&lt;/dcr:xpath&gt;
 * &lt;/d:searchrequest&gt;
 * </pre>
 *
 * Would return the following values:
 * <pre>
 *    getLanguageName() -&gt; xpath
 *    getQuery()        -&gt; //sv:node[@sv:name='myapp:paragraph'][1]
 * </pre>
 *
 */
public class SearchInfo implements SearchConstants {

    private static Logger log = Logger.getLogger(SearchInfo.class);

    private final Element languageElem;

    /**
     * Create a new <code>SearchInfo</code> instance.
     *
     * @param language
     * @param languageNamespace
     * @param query
     */
    public SearchInfo(String language, Namespace languageNamespace, String query) {
        languageElem = new Element(language, languageNamespace).setText(query);
    }

    /**
     * Create a new <code>SearchInfo</code> from the specifying document
     * retrieved from the request body.
     *
     * @param searchDocument
     * @throws IllegalArgumentException if the root element's name is other than
     * 'searchrequest' or if it does not contain a single child element specifying
     * the query language to be used.
     */
    public SearchInfo(Document searchDocument) {
        Element searchRequest = searchDocument.getRootElement();
        if (searchRequest == null || !XML_SEARCHREQUEST.equals(searchRequest.getName()))  {
            throw new IllegalArgumentException("The root element must be 'searchrequest'.");
        } else if (searchRequest.getChildren().size() != 1) {
            throw new IllegalArgumentException("A single child element is expected with the 'searchrequest'.");
        }
        Element child = (Element)searchRequest.getChildren().get(0);
        languageElem = (Element) child.detach();
    }

    /**
     * Returns the name of the query language to be used.
     *
     * @return name of the query language
     */
    public String getLanguageName() {
        return languageElem.getName();
    }

    /**
     * Returns the namespace of the language specified with the search request element.
     *
     * @return namespace of the requestes language.
     */
    public Namespace getLanguageNameSpace() {
        return languageElem.getNamespace();
    }

    /**
     * Return the query string.
     *
     * @return query string
     */
    public String getQuery() {
        return languageElem.getText();
    }

    /**
     * Return the xml representation of this <code>SearchInfo</code> instance.
     *
     * @return xml representation
     */
    public Document toXml() {
        Element sRequestElem = new Element(XML_SEARCHREQUEST, NAMESPACE);
        sRequestElem.addContent(languageElem);
        return new Document(sRequestElem);
    }
}