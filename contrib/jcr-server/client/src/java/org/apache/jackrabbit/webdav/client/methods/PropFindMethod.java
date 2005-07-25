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
package org.apache.jackrabbit.webdav.client.methods;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.header.DepthHeader;
import org.jdom.Element;
import org.jdom.Document;

/**
 * <code>PropFindMethod</code>...
 */
public class PropFindMethod extends DavMethodBase {

    private static Logger log = Logger.getLogger(PropFindMethod.class);

    public PropFindMethod(String uri) {
        this(uri, PROPFIND_ALL_PROP, new DavPropertyNameSet(), DEPTH_INFINITY);
    }

    public PropFindMethod(String uri, DavPropertyNameSet propNameSet, int depth) {
        this(uri, PROPFIND_BY_PROPERTY, propNameSet, depth);
    }

    public PropFindMethod(String uri, int propfindType, int depth) {
        this(uri, propfindType, new DavPropertyNameSet(), depth);
    }

    private PropFindMethod(String uri, int propfindType, DavPropertyNameSet propNameSet, int depth) {
        super(uri);

        DepthHeader dh = new DepthHeader(depth);
        setRequestHeader(dh.getHeaderName(), dh.getHeaderValue());
        setRequestHeader("Content-Type","text/xml; charset=UTF-8");

        // build the request body
        Element propfind = new Element(XML_PROPFIND, NAMESPACE);
        switch (propfindType) {
            case PROPFIND_ALL_PROP:
                propfind.addContent(new Element(XML_ALLPROP, NAMESPACE));
                break;
            case PROPFIND_PROPERTY_NAMES:
                propfind.addContent(new Element(XML_PROPNAME, NAMESPACE));
                break;
            default:
                if (propNameSet == null) {
                    propfind.addContent(new Element(XML_PROP, NAMESPACE));
                } else {
                    propfind.addContent(propNameSet.toXml());
                }
                break;
        }
        Document propfindBody = new Document(propfind);
        setRequestBody(propfindBody);
    }

    public String getName() {
        return DavMethods.METHOD_PROPFIND;
    }
}
