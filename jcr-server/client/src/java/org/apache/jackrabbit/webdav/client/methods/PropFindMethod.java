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
package org.apache.jackrabbit.webdav.client.methods;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.header.DepthHeader;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xml.serialize.OutputFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

/**
 * <code>PropFindMethod</code>...
 */
public class PropFindMethod extends DavMethodBase {

    private static Logger log = Logger.getLogger(PropFindMethod.class);

    public PropFindMethod(String uri) throws IOException {
        this(uri, PROPFIND_ALL_PROP, new DavPropertyNameSet(), DEPTH_INFINITY);
    }

    public PropFindMethod(String uri, DavPropertyNameSet propNameSet, int depth)
        throws IOException {
        this(uri, PROPFIND_BY_PROPERTY, propNameSet, depth);
    }

    public PropFindMethod(String uri, int propfindType, int depth)
        throws IOException {
        this(uri, propfindType, new DavPropertyNameSet(), depth);
    }

    private PropFindMethod(String uri, int propfindType, DavPropertyNameSet propNameSet,
                           int depth) throws IOException {
        super(uri);

        DepthHeader dh = new DepthHeader(depth);
        setRequestHeader(dh.getHeaderName(), dh.getHeaderValue());

        setRequestHeader(DavConstants.HEADER_CONTENT_TYPE, "text/xml; charset=UTF-8");

        // build the request body
        try {
            // create the document and attach the root element
            Document document = BUILDER_FACTORY.newDocumentBuilder().newDocument();
            Element propfind = DomUtil.createElement(document, XML_PROPFIND, NAMESPACE);
            document.appendChild(propfind);

            // fill the propfind element
        switch (propfindType) {
            case PROPFIND_ALL_PROP:
                    propfind.appendChild(DomUtil.createElement(document, XML_ALLPROP, NAMESPACE));
                break;
            case PROPFIND_PROPERTY_NAMES:
                    propfind.appendChild(DomUtil.createElement(document, XML_PROPNAME, NAMESPACE));
                break;
            default:
                if (propNameSet == null) {
                        propfind.appendChild(DomUtil.createElement(document, XML_PROP, NAMESPACE));
                } else {
                        propfind.appendChild(propNameSet.toXml(document));
                }
                break;
        }

            // set the request body
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            OutputFormat format = new OutputFormat("xml", "UTF-8", true);
            XMLSerializer serializer = new XMLSerializer(out, format);
            serializer.setNamespaces(true);
            serializer.asDOMSerializer().serialize(document);
            setRequestBody(out.toString());

        } catch (ParserConfigurationException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * @see org.apache.commons.httpclient.HttpMethod#getName()
     */
    public String getName() {
        return DavMethods.METHOD_PROPFIND;
    }
}