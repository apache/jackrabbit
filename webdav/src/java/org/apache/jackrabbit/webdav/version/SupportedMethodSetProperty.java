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
package org.apache.jackrabbit.webdav.version;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.jdom.Element;

/**
 * The <code>SupportedMethodSetProperty</code>
 */
public class SupportedMethodSetProperty extends DefaultDavProperty implements DeltaVConstants {

    private static Logger log = Logger.getLogger(SupportedMethodSetProperty.class);

    /**
     * Create a new <code>SupportedMethodSetProperty</code> property.
     *
     * @param methods that are supported by the resource having this property.
     */
    public SupportedMethodSetProperty(String[] methods) {
        super(DeltaVConstants.SUPPORTED_METHOD_SET, new Element[methods.length], true);

        // fill the array with the proper elements
        Element[] value = (Element[]) getValue();
        for (int i = 0; i < methods.length; i++) {
            Element methodElem = new Element(DeltaVConstants.XML_SUPPORTED_METHOD, DeltaVConstants.NAMESPACE);
            methodElem.setAttribute("name",methods[i], DeltaVConstants.NAMESPACE);
            value[i] = methodElem;
        }
    }
}