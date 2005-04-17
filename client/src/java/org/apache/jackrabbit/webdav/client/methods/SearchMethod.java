/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.webdav.client.methods;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.search.SearchInfo;
import org.apache.jackrabbit.webdav.DavMethods;
import org.jdom.Namespace;

/**
 * <code>SearchMethod</code>...
 */
public class SearchMethod extends DavMethodBase {

    private static Logger log = Logger.getLogger(SearchMethod.class);

    public SearchMethod(String uri, String statement, String language) {
        this(uri, statement, language, Namespace.NO_NAMESPACE);
    }

    public SearchMethod(String uri, String statement, String language, Namespace languageNamespace) {
        super(uri);
        if (language != null && statement != null) {
            // build the request body
            SearchInfo sInfo = new SearchInfo(language, languageNamespace, statement);
            setRequestBody(sInfo.toXml());
        }
    }
    
    public String getName() {
        return DavMethods.METHOD_SEARCH;
    }
}