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
package org.apache.jackrabbit.core.search.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NoPrefixDeclaredException;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.Path;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * Creates a lucene <code>Document</code> object from a {@link javax.jcr.Node}.
 *
 * todo add support for indexing of nt:resource. e.g. when mime type is text/*
 */
public class NodeIndexer {

    /**
     * The <code>NodeState</code> of the node to index
     */
    protected final NodeState node;
    /**
     * The persistent item state provider
     */
    protected final ItemStateManager stateProvider;

    /**
     * Namespace mappings to use for indexing. This is the internal
     * namespace mapping.
     */
    protected final NamespaceMappings mappings;

    /**
     * Creates a new node indexer.
     *
     * @param node          the node state to index.
     * @param stateProvider the persistent item state manager to retrieve properties.
     * @param mappings      internal namespace mappings.
     */
    protected NodeIndexer(NodeState node,
                        ItemStateManager stateProvider,
                        NamespaceMappings mappings) {
        this.node = node;
        this.stateProvider = stateProvider;
        this.mappings = mappings;
    }

    /**
     * Creates a lucene Document from a node.
     *
     * @param node          the node state to index.
     * @param stateProvider the state provider to retrieve property values.
     * @param mappings      internal namespace mappings.
     * @return the lucene Document.
     * @throws RepositoryException if an error occurs while reading property
     *                             values from the <code>ItemStateProvider</code>.
     */
    public static Document createDocument(NodeState node,
                                          ItemStateManager stateProvider,
                                          NamespaceMappings mappings)
            throws RepositoryException {
        NodeIndexer indexer = new NodeIndexer(node, stateProvider, mappings);
        return indexer.createDoc();
    }

    /**
     * Creates a lucene Document.
     *
     * @return the lucene Document with the index layout.
     * @throws RepositoryException if an error occurs while reading property
     *                             values from the <code>ItemStateProvider</code>.
     */
    protected Document createDoc() throws RepositoryException {
        Document doc = new Document();

        // special fields
        // UUID
        doc.add(new Field(FieldNames.UUID, node.getUUID(), true, true, false));
        try {
            // parent UUID
            if (node.getParentUUID() == null) {
                // root node
                doc.add(new Field(FieldNames.PARENT, "", true, true, false));
                doc.add(new Field(FieldNames.LABEL, "", false, true, false));
            } else {
                doc.add(new Field(FieldNames.PARENT, node.getParentUUID(), true, true, false));
                NodeState parent = (NodeState) stateProvider.getItemState(new NodeId(node.getParentUUID()));
                List entries = parent.getChildNodeEntries(node.getUUID());
                for (Iterator it = entries.iterator(); it.hasNext();) {
                    NodeState.ChildNodeEntry child = (NodeState.ChildNodeEntry) it.next();
                    String name = child.getName().toJCRName(mappings);
                    doc.add(new Field(FieldNames.LABEL, name, false, true, false));
                }
            }
        } catch (NoSuchItemStateException e) {
            throw new RepositoryException("Error while indexing node: " + node.getUUID(), e);
        } catch (ItemStateException e) {
            throw new RepositoryException("Error while indexing node: " + node.getUUID(), e);
        } catch (NoPrefixDeclaredException e) {
            // will never happen, because this.mappings will dynamically add
            // unknown uri<->prefix mappings
        }

        List props = node.getPropertyEntries();
        for (Iterator it = props.iterator(); it.hasNext();) {
            NodeState.PropertyEntry prop = (NodeState.PropertyEntry) it.next();
            PropertyId id = new PropertyId(node.getUUID(), prop.getName());
            try {
                PropertyState propState = (PropertyState) stateProvider.getItemState(id);
                InternalValue[] values = propState.getValues();
                if (propState.isMultiValued()) {
                    // multi valued
                    if (values.length == 1) {
                        // also index as if single value property
                        addValue(doc, values[0], propState.getName(), false);
                    }
                    for (int i = 0; i < values.length; i++) {
                        addValue(doc, values[i], propState.getName(), true);
                    }
                } else {
                    // single value
                    // do we have a value at all?
                    if (values.length == 1) {
                        addValue(doc, values[0], propState.getName(), false);
                    }
                }
            } catch (NoSuchItemStateException e) {
                throw new RepositoryException("Error while indexing node: " + node.getUUID(), e);
            } catch (ItemStateException e) {
                throw new RepositoryException("Error while indexing node: " + node.getUUID(), e);
            }
        }
        return doc;
    }

    /**
     * Adds a value to the lucene Document.
     *
     * @param doc   the document.
     * @param value the internal jackrabbit value.
     * @param name  the name of the property.
     * @param multiValued if <code>true</code> the value is treated as a
     *   multivalued.
     */
    private void addValue(Document doc, InternalValue value, QName name, boolean multiValued) {
        String fieldName = name.getLocalName();
        try {
            StringBuffer tmp = new StringBuffer();
            tmp.append(mappings.getPrefix(name.getNamespaceURI()));
            tmp.append(':');
            if (multiValued) {
                tmp.append(FieldNames.MVP_PREFIX);
            }
            tmp.append(name.getLocalName());
            fieldName = tmp.toString();
        } catch (NamespaceException e) {
            // will never happen
        }
        Object internalValue = value.internalValue();
        switch (value.getType()) {
            case PropertyType.BINARY:
                addBinaryValue(doc, fieldName, internalValue);
                break;
            case PropertyType.BOOLEAN:
                addBooleanValue(doc, fieldName, internalValue);
                break;
            case PropertyType.DATE:
                addCalendarValue(doc, fieldName, internalValue);
                break;
            case PropertyType.DOUBLE:
                addDoubleValue(doc, fieldName, internalValue);
                break;
            case PropertyType.LONG:
                addLongValue(doc, fieldName, internalValue);
                break;
            case PropertyType.REFERENCE:
                addReferenceValue(doc, fieldName, internalValue);
                break;
            case PropertyType.PATH:
                addPathValue(doc, fieldName, internalValue);
                break;
            case PropertyType.STRING:
                addStringValue(doc, fieldName, internalValue);
                break;
            case PropertyType.NAME:
                addNameValue(doc, fieldName, internalValue);
                break;
            default:
                throw new IllegalArgumentException("illegal internal value type");
        }
    }

    /**
     * Adds the binary value to the document as the named field.
     * <p>
     * This implementation does nothing as binary indexing is not implemented
     * here.
     * 
     * @param doc The document to which to add the field
     * @param fieldName The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addBinaryValue(Document doc, String fieldName, Object internalValue) {
        // don't know how to index -> ignore
    }
    
    /**
     * Adds the string representation of the boolean value to the document as
     * the named field.
     * 
     * @param doc The document to which to add the field
     * @param fieldName The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addBooleanValue(Document doc, String fieldName, Object internalValue) {
        doc.add(new Field(fieldName,
            internalValue.toString(),
            false,
            true,
            false));
    }

    /**
     * Adds the calendar value to the document as the named field. The calendar
     * value is converted to an indexable string value using the {@link DateField}
     * class.
     * 
     * @param doc The document to which to add the field
     * @param fieldName The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addCalendarValue(Document doc, String fieldName, Object internalValue) {
        long millis = ((Calendar) internalValue).getTimeInMillis();
        doc.add(new Field(fieldName,
                DateField.timeToString(millis),
                false,
                true,
                false));
    }
    
    /**
     * Adds the double value to the document as the named field. The double
     * value is converted to an indexable string value using the
     * {@link DoubleField} class.
     * 
     * @param doc The document to which to add the field
     * @param fieldName The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addDoubleValue(Document doc, String fieldName, Object internalValue) {
        double doubleVal = ((Double) internalValue).doubleValue();
        doc.add(new Field(fieldName,
                DoubleField.doubleToString(doubleVal),
                false,
                true,
                false));
    }
    
    /**
     * Adds the long value to the document as the named field. The long
     * value is converted to an indexable string value using the {@link LongField}
     * class.
     * 
     * @param doc The document to which to add the field
     * @param fieldName The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addLongValue(Document doc, String fieldName, Object internalValue) {
        long longVal = ((Long) internalValue).longValue();
        doc.add(new Field(fieldName,
                LongField.longToString(longVal),
                false,
                true,
                false));
    }
    
    /**
     * Adds the reference value to the document as the named field. The value's
     * string representation is added as the reference data. Additionally the
     * reference data is stored in the index.
     * 
     * @param doc The document to which to add the field
     * @param fieldName The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addReferenceValue(Document doc, String fieldName, Object internalValue) {
        String uuid = internalValue.toString();
        doc.add(new Field(fieldName,
                uuid,
                true, // store
                true,
                false));
    }
    
    /**
     * Adds the path value to the document as the named field. The path
     * value is converted to an indexable string value using the name space
     * mappings with which this class has been created.
     * 
     * @param doc The document to which to add the field
     * @param fieldName The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addPathValue(Document doc, String fieldName, Object internalValue) {
        Path path = (Path) internalValue;
        String pathString = path.toString();
        try {
            pathString = path.toJCRPath(mappings);
        } catch (NoPrefixDeclaredException e) {
            // will never happen
        }
        doc.add(new Field(fieldName,
                pathString,
                false,
                true,
                false));
    }

    /**
     * Adds the string value to the document both as the named field and for
     * full text indexing.
     * 
     * @param doc The document to which to add the field
     * @param fieldName The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addStringValue(Document doc, String fieldName, Object internalValue) {
        String stringValue = String.valueOf(internalValue);

        // simple String
        doc.add(new Field(fieldName,
                stringValue,
                false,
                true,
                false));
        // also create fulltext index of this value
        doc.add(new Field(FieldNames.FULLTEXT,
                stringValue,
                false,
                true,
                true));
        // create fulltext index on property
        int idx = fieldName.indexOf(':');
        fieldName = fieldName.substring(0, idx + 1) +
                FieldNames.FULLTEXT_PREFIX +
                fieldName.substring(idx + 1);
        doc.add(new Field(fieldName, stringValue,
                false,
                true,
                true));
    }
    
    /**
     * Adds the name value to the document as the named field. The name
     * value is converted to an indexable string treating the internal value
     * as a qualified name and mapping the name space using the name space
     * mappings with which this class has been created.
     * 
     * @param doc The document to which to add the field
     * @param fieldName The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addNameValue(Document doc, String fieldName, Object internalValue) {
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
    }
}
