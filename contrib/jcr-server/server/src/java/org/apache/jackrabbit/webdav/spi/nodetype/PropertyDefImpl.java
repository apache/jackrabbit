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
package org.apache.jackrabbit.webdav.spi.nodetype;

import org.apache.log4j.Logger;
import org.jdom.Element;

import javax.jcr.nodetype.PropertyDef;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * <code>PropertyDefImpl</code>...
 */
public final class PropertyDefImpl extends ItemDefImpl implements PropertyDef {

    private static Logger log = Logger.getLogger(PropertyDefImpl.class);

    private final int type;
    private final String[] valueConstraints;
    private final Value[] defaultValues;
    private final boolean isMultiple;

    private PropertyDefImpl(PropertyDef definition) {
	super(definition);

	type = definition.getRequiredType();
	valueConstraints = definition.getValueConstraints();
	defaultValues = definition.getDefaultValues();
	isMultiple = definition.isMultiple();
    }

    public static PropertyDefImpl create(PropertyDef definition) {
	if (definition instanceof PropertyDefImpl) {
	    return (PropertyDefImpl)definition;
	} else {
	    return new PropertyDefImpl(definition);
	}
    }

    public int getRequiredType() {
	return type;
    }

    public String[] getValueConstraints() {
	return valueConstraints;
    }

    public Value[] getDefaultValues() {
	return defaultValues;
    }

    public boolean isMultiple() {
	return isMultiple;
    }

    //-------------------------------------< implementation specific method >---
    public Element toXml() {
	Element elem = super.toXml();

        elem.setAttribute(ATTR_MULTIPLE, Boolean.toString(isMultiple()));
        elem.setAttribute(ATTR_REQUIREDTYPE, PropertyType.nameFromValue(getRequiredType()));

        // default values may be 'null'
        Value[] values = getDefaultValues();
        if (values != null) {
            Element dvElement = new Element(XML_DEFAULTVALUES);
            for (int i = 0; i < values.length; i++) {
                try {
                    Element valElem = new Element(XML_DEFAULTVALUE).setText(values[i].getString());
                    dvElement.addContent(valElem);
                } catch (RepositoryException e) {
                    // should not occur
                    log.error(e.getMessage());
                }
            }
            elem.addContent(dvElement);
        }
        // value constraints array is never null.
        Element constrElem = new Element(XML_VALUECONSTRAINTS);
        String[] constraints = getValueConstraints();
        for (int i = 0; i < constraints.length; i++) {
            constrElem.addContent(new Element(XML_VALUECONSTRAINT).setText(constraints[i]));
        }
        elem.addContent(constrElem);

        return elem;
    }

    public String getElementName() {
	return XML_PROPERTYDEF;
    }
}