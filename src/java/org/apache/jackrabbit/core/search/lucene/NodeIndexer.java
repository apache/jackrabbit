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
package org.apache.jackrabbit.core.search.lucene;

import org.apache.jackrabbit.core.search.NamespaceMappings;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ItemStateProvider;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.Path;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import javax.jcr.PropertyType;
import javax.jcr.NamespaceException;
import java.util.List;
import java.util.Iterator;
import java.util.Calendar;

/**
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public class NodeIndexer {

    private final NodeState node;
    private final ItemStateProvider stateProvider;
    private final String path;
    private final NamespaceMappings mappings;

    private NodeIndexer(NodeState node,
			ItemStateProvider stateMgr,
			String path,
			NamespaceMappings mappings) {
	this.node = node;
	this.stateProvider = stateMgr;
	this.path = path;
	this.mappings = mappings;
    }

    public static Document createDocument(NodeState node,
				   ItemStateProvider stateMgr,
				   String path,
				   NamespaceMappings mappings) {
	NodeIndexer indexer = new NodeIndexer(node, stateMgr, path, mappings);
	return indexer.createDoc();
    }

    private Document createDoc() {
	Document doc = new Document();

	// special fields
	// UUID
	doc.add(new Field(FieldNames.UUID, node.getUUID(), true, true, false));
	// Path
	doc.add(new Field(FieldNames.PATH, path, true, true, false));

	List props = node.getPropertyEntries();
	for (Iterator it = props.iterator(); it.hasNext();) {
	    NodeState.PropertyEntry prop = (NodeState.PropertyEntry) it.next();
	    PropertyId id = new PropertyId(node.getUUID(), prop.getName());
	    try {
		PropertyState propState = (PropertyState) stateProvider.getItemState(id);
		InternalValue[] values = propState.getValues();
		for (int i = 0; i < values.length; i++) {
		    addValue(doc, values[i], propState.getName());
		}
	    } catch (NoSuchItemStateException e) {
		// FIXME do logging? throw?
	    } catch (ItemStateException e) {
		// FIXME do logging? throw?
	    }
	}
	return doc;
    }

    private void addValue(Document doc, InternalValue value, QName name) {
	String fieldName = name.toString();
	try {
	    fieldName = mappings.getPrefix(name.getNamespaceURI()) + ":" + name.getLocalName();
	} catch (NamespaceException e) {
	    // will never happen
	}
	Object internalValue = value.internalValue();
	switch (value.getType()) {
	    case PropertyType.BINARY:
		// don't know how to index -> ignore
		break;
	    case PropertyType.BOOLEAN:
		doc.add(new Field(fieldName,
			internalValue.toString(),
			false,
			true,
			false));
		break;
	    case PropertyType.DATE:
                long millis = ((Calendar) internalValue).getTimeInMillis();
		doc.add(new Field(fieldName,
			DateField.timeToString(millis),
			false,
			true,
			false));
		break;
	    case PropertyType.DOUBLE:
		double doubleVal = ((Double) internalValue).doubleValue();
		doc.add(new Field(fieldName,
			DoubleField.doubleToString(doubleVal),
			false,
			true,
			false));
		break;
	    case PropertyType.LONG:
		long longVal = ((Long) internalValue).longValue();
		doc.add(new Field(fieldName,
			LongField.longToString(longVal),
			false,
			true,
			false));
		break;
	    case PropertyType.REFERENCE:
		String uuid = ((UUID) internalValue).toString();
		doc.add(new Field(fieldName,
			uuid,
			false,
			true,
			false));
		break;
	    case PropertyType.PATH:
                String path = ((Path) internalValue).toString();
		doc.add(new Field(fieldName,
			path,
			false,
			true,
			false));
		break;
	    case PropertyType.STRING:
		// simple String
		doc.add(new Field(fieldName,
			internalValue.toString(),
			false,
			true,
			false));
		// also create fulltext index of this value
		doc.add(new Field(FieldNames.FULLTEXT,
			internalValue.toString(),
			false,
			true,
			true));
		break;
	    case PropertyType.NAME:
		QName qualiName = (QName) internalValue;
		String normValue = internalValue.toString();
		try {
		    normValue = mappings.getPrefix(qualiName.getNamespaceURI())
			    + ":" + qualiName.getLocalName();
		} catch (NamespaceException e) {
		    // will never happen
		}
		doc.add(new Field(fieldName,
			normValue,
			false,
			true,
			false));
		break;
	    default:
		throw new IllegalArgumentException("illegal internal value type");
	}
    }

}
