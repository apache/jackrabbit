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
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * <code>PropPatchMethod</code>...
 */
public class PropPatchMethod extends DavMethodBase implements DavConstants {

    private static Logger log = Logger.getLogger(PropPatchMethod.class);

    public PropPatchMethod(String uri, DavPropertySet setProperties,
                           DavPropertyNameSet removeProperties) throws IOException {
        super(uri);
        if (setProperties == null || removeProperties == null) {
            throw new IllegalArgumentException("Neither setProperties nor removeProperties must be null.");
        }

        try {
            Document document = BUILDER_FACTORY.newDocumentBuilder().newDocument();
            Element propupdate = DomUtil.createElement(document, XML_PROPERTYUPDATE, NAMESPACE);
            // DAV:set
            Element set = DomUtil.createElement(document, XML_SET, NAMESPACE);
            set.appendChild(setProperties.toXml(document));
            // DAV:remove
            Element remove = DomUtil.createElement(document, XML_REMOVE, NAMESPACE);
            remove.appendChild(removeProperties.toXml(document));

            propupdate.appendChild(set);
            propupdate.appendChild(remove);
            document.appendChild(propupdate);

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
        return DavMethods.METHOD_PROPPATCH;
    }
}