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

import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <code>QueryGrammerSet</code> is a {@link DavProperty} that
 * encapsulates the 'supported-query-grammer-set' as defined by the
 * Webdav SEARCH internet draft.
 */
public class QueryGrammerSet extends AbstractDavProperty implements SearchConstants {

    private List queryLanguages = new ArrayList();

    /**
     * Create a new <code>QueryGrammerSet</code> from the given query languages
     * string array. The default {@link SearchConstants#NAMESPACE} is assumed.
     * @param qLanguages
     */
    public QueryGrammerSet(String[] qLanguages) {
        super(QUERY_GRAMMER_SET, true);
        if (qLanguages != null) {
            for (int i = 0; i < qLanguages.length; i++) {
                queryLanguages.add(new Element(qLanguages[i], SearchConstants.NAMESPACE));
            }
        }
    }

    /**
     * Add another query language to this set.
     *
     * @param qLanguage
     * @param namespace
     */
    public void addQueryLanguage(String qLanguage, Namespace namespace) {
        if (namespace == null) {
            namespace = SearchConstants.NAMESPACE;
        }
        queryLanguages.add(new Element(qLanguage, namespace));
    }

    /**
     * Return a String array containing the URIs of the query
     * languages supported.
     *
     * @return names of the supported query languages
     */
    public String[] getQueryLanguages() {
        int size = queryLanguages.size();
        if (size > 0) {
            String[] qLangStr = new String[size];
            Element[] elements = (Element[]) queryLanguages.toArray(new Element[size]);
            for (int i = 0; i < elements.length; i++) {
                qLangStr[i] = elements[i].getNamespaceURI() + elements[i].getName();
            }
            return qLangStr;
        } else {
            return new String[0];
        }
    }

    /**
     * Return the Xml representation of this property according to the definition
     * of the 'supported-query-grammer-set'.
     *
     * @return Xml representation
     * @see SearchConstants#QUERY_GRAMMER_SET
     * @see org.apache.jackrabbit.webdav.property.DavProperty#toXml()
     */
    public Element toXml() {
        Element elem = getName().toXml();
        Iterator qlIter = queryLanguages.iterator();
        while (qlIter.hasNext()) {
            Element grammer = new Element(XML_GRAMMER, SearchConstants.NAMESPACE).addContent((Element)qlIter.next());
            Element sqg = new Element(XML_QUERY_GRAMMAR, SearchConstants.NAMESPACE).addContent(grammer);
            elem.addContent(sqg);
        }
        return elem;
    }

    /**
     * Returns the list of supported query languages.
     *
     * @return list of supported query languages.
     * @see org.apache.jackrabbit.webdav.property.DavProperty#getValue()
     */
    public Object getValue() {
        return queryLanguages;
    }
}