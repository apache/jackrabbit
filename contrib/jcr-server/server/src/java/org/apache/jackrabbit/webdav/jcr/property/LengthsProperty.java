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
package org.apache.jackrabbit.webdav.jcr.property;

import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.jdom.Element;

/**
 * <code>LengthsProperty</code> extends {@link org.apache.jackrabbit.webdav.property.DavProperty} providing
 * utilities to handle the multiple lengths of the property item represented
 * by this resource.
 */
public class LengthsProperty extends AbstractDavProperty implements ItemResourceConstants {

    private final Element[] value;

    /**
     * Create a new <code>LengthsProperty</code> from the given long array.
     *
     * @param lengths as retrieved from the JCR property
     */
    public LengthsProperty(long[] lengths) {
	super(JCR_LENGTHS, true);

	Element[] elems = new Element[lengths.length];
	for (int i = 0; i < lengths.length; i++) {
	    elems[i] = new Element(XML_LENGTH, ItemResourceConstants.NAMESPACE);
	    elems[i].addContent(String.valueOf(lengths[i]));
	}
	this.value = elems;
    }

    /**
     * Returns an array of {@link Element}s representing the value of this
     * property.
     *
     * @return an array of {@link Element}s
     */
    public Object getValue() {
	return value;
    }
}