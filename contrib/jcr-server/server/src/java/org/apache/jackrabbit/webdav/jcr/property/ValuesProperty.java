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
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.value.ValueHelper;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * <code>ValuesProperty</code> extends {@link org.apache.jackrabbit.webdav.property.DavProperty} providing
 * utilities to handle the multiple values of the property item represented
 * by this resource.
 */
public class ValuesProperty extends AbstractDavProperty implements ItemResourceConstants {

    private static Logger log = Logger.getLogger(ValuesProperty.class);

    private final Value[] jcrValues;

    /**
     * Create a new <code>ValuesProperty</code> from the given {@link javax.jcr.Value Value
     * array}.
     *
     * @param values Array of Value objects as obtained from the JCR property.
     */
    public ValuesProperty(Value[] values) throws ValueFormatException, RepositoryException {
	super(JCR_VALUES, false);
	// finally set the value to the DavProperty
	jcrValues = values;
    }
    
    /**
     * Wrap the specified <code>DavProperty</code> in a new <code>ValuesProperty</code>.
     *
     * @param property
     */
    public ValuesProperty(DavProperty property) throws RepositoryException {
	super(JCR_VALUES, false);

	if (!JCR_VALUES.equals(property.getName())) {
	    throw new IllegalArgumentException("ValuesProperty may only be created with a property that has name="+JCR_VALUES.getName());
	}

	List valueElements = new ArrayList();
        Object propValue = property.getValue();
        if (propValue != null) {
            if (isValueElement(propValue)) {
                valueElements.add(propValue);
            } else if (propValue instanceof List) {
	    Iterator elemIt = ((List)property.getValue()).iterator();
	    while (elemIt.hasNext()) {
		Object el = elemIt.next();
		/* make sure, only Elements with name 'value' are used for
		* the 'value' field. any other content (other elements, text,
		* comment etc.) is ignored. NO bad-request/conflict error is
		* thrown.
		*/
                    if (isValueElement(propValue)) {
		    valueElements.add(el);
		}
	    }
            }
        }
	    /* fill the 'value' with the valid 'value' elements found before */
        Element[] elems = (Element[])valueElements.toArray(new Element[valueElements.size()]);
	jcrValues = new Value[elems.length];
	for (int i = 0; i < elems.length; i++) {
            String value = DomUtil.getText(elems[i]);
	    jcrValues[i] = ValueHelper.deserialize(value, PropertyType.STRING, false);
	}
	}

    private static boolean isValueElement(Object obj) {
        return obj instanceof Element && XML_VALUE.equals(((Element)obj).getLocalName());
    }

    /**
     * Converts the value of this property to a {@link javax.jcr.Value value array}.
     *
     * @return Array of Value objects
     * @throws ValueFormatException if convertion of the internal jcr values to
     * the specified value type fails.
     */
    public Value[] getValues(int propertyType) throws ValueFormatException {
        Value[] vs = new Value[jcrValues.length];
        for (int i = 0; i < jcrValues.length; i++) {
            vs[i] = ValueHelper.convert(jcrValues[i], propertyType);
	}
	return jcrValues;
    }

    /**
     * Returns an array of {@link Value}s representing the value of this
     * property.
     *
     * @return an array of {@link Value}s
     * @see #getValues(int)
     */
    public Object getValue() {
	return jcrValues;
    }

    public Element toXml(Document document) {
        Element elem = getName().toXml(document);
        for (int i = 0; i < jcrValues.length; i++) {
            try {
                DomUtil.addChildElement(elem, XML_VALUE, ItemResourceConstants.NAMESPACE, jcrValues[i].getString());
            } catch (RepositoryException e) {
                log.error("Unexpected Error while converting jcr value to String: " + e.getMessage());
    }
        }
        return elem;
    }

}