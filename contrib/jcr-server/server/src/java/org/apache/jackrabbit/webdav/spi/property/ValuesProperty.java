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
package org.apache.jackrabbit.webdav.spi.property;

import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.spi.ItemResourceConstants;
import org.apache.jackrabbit.core.util.ValueHelper;
import org.jdom.Element;

import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * <code>ValuesProperty</code> extends {@link org.apache.jackrabbit.webdav.property.DavProperty} providing
 * utilities to handle the multiple values of the property item represented
 * by this resource.
 */
public class ValuesProperty extends AbstractDavProperty implements ItemResourceConstants {

    private final Element[] value;

    /**
     * Wrap the specified <code>DavProperty</code> in a new <code>ValuesProperty</code>.
     *
     * @param property
     */
    public ValuesProperty(DavProperty property) {
	super(JCR_VALUES, false);

	if (!JCR_VALUES.equals(property.getName())) {
	    throw new IllegalArgumentException("ValuesProperty may only be created with a property that has name="+JCR_VALUES.getName());
	}

	Element[] elems = new Element[0];
	if (property.getValue() instanceof List) {
	    Iterator elemIt = ((List)property.getValue()).iterator();
	    ArrayList valueElements = new ArrayList();
	    while (elemIt.hasNext()) {
		Object el = elemIt.next();
		/* make sure, only Elements with name 'value' are used for
		* the 'value' field. any other content (other elements, text,
		* comment etc.) is ignored. NO bad-request/conflict error is
		* thrown.
		*/
		if (el instanceof Element && "value".equals(((Element)el).getName())) {
		    valueElements.add(el);
		}
	    }
	    /* fill the 'value' with the valid 'value' elements found before */
	    elems = (Element[])valueElements.toArray(new Element[valueElements.size()]);
	} else {
	    new IllegalArgumentException("ValuesProperty may only be created with a property that has a list of 'value' elements as content.");
	}
	// finally set the value to the DavProperty
	value = elems;
    }

    /**
     * Create a new <code>ValuesProperty</code> from the given {@link javax.jcr.Value Value
     * array}.
     *
     * @param values Array of Value objects as obtained from the JCR property.
     */
    public ValuesProperty(Value[] values) throws ValueFormatException, RepositoryException {
	super(JCR_VALUES, false);

	Element[] propValue = new Element[values.length];
	for (int i = 0; i < values.length; i++) {
	    propValue[i] = new Element(XML_VALUE, ItemResourceConstants.NAMESPACE);
	    propValue[i].addContent(values[i].getString());
	}
	// finally set the value to the DavProperty
	value = propValue;
    }

    /**
     * Converts the value of this property to a {@link javax.jcr.Value value array}.
     * Please note, that the convertion is done by using the {@link org.apache.jackrabbit.core.util.ValueHelper}
     * class that is not part of the JSR170 API.
     *
     * @return Array of Value objects
     * @throws RepositoryException
     */
    public Value[] getValues(int propertyType) throws ValueFormatException, RepositoryException {
	Element[] propValue = (Element[])getValue();
	Value[] values = new Value[propValue.length];
	for (int i = 0; i < propValue.length; i++) {
	    values[i] = ValueHelper.convert(propValue[i].getText(), propertyType);
	}
	return values;
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