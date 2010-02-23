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
package org.apache.jackrabbit.webdav.jcr.property;

import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <code>LengthsProperty</code> extends {@link org.apache.jackrabbit.webdav.property.DavProperty} providing
 * utilities to handle the multiple lengths of the property item represented
 * by this resource.
 */
public class LengthsProperty extends AbstractDavProperty<long[]> implements ItemResourceConstants {

    private final long[] value;

    /**
     * Create a new <code>LengthsProperty</code> from the given long array.
     *
     * @param lengths as retrieved from the JCR property
     */
    public LengthsProperty(long[] lengths) {
        super(JCR_LENGTHS, true);
        this.value = lengths;
    }

    /**
     * Returns an array of {@link long}s representing the value of this
     * property.
     *
     * @return an array of {@link long}s
     */
    public long[] getValue() {
        return value;
    }

    /**
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     */
    @Override
    public Element toXml(Document document) {
        Element elem = getName().toXml(document);
        for (long length : value) {
            String txtContent = String.valueOf(length);
            DomUtil.addChildElement(elem, XML_LENGTH, ItemResourceConstants.NAMESPACE, txtContent);
        }
        return elem;
    }

}
