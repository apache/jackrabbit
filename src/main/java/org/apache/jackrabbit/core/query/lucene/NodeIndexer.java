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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.query.TextFilter;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.PathFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.io.Reader;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates a lucene <code>Document</code> object from a {@link javax.jcr.Node}.
 */
public class NodeIndexer {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(NodeIndexer.class);

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
     * List of text filters in use.
     */
    protected final List textFilters;

    /**
     * Creates a new node indexer.
     *
     * @param node          the node state to index.
     * @param stateProvider the persistent item state manager to retrieve properties.
     * @param mappings      internal namespace mappings.
     * @param textFilters   List of {@link org.apache.jackrabbit.core.query.TextFilter}s.
     */
    protected NodeIndexer(NodeState node,
                          ItemStateManager stateProvider,
                          NamespaceMappings mappings,
                          List textFilters) {
        this.node = node;
        this.stateProvider = stateProvider;
        this.mappings = mappings;
        this.textFilters = textFilters;
    }

    /**
     * Creates a lucene Document from a node.
     *
     * @param node          the node state to index.
     * @param stateProvider the state provider to retrieve property values.
     * @param mappings      internal namespace mappings.
     * @param textFilters   list of text filters to use for indexing binary
     *                      properties.
     * @return the lucene Document.
     * @throws RepositoryException if an error occurs while reading property
     *                             values from the <code>ItemStateProvider</code>.
     */
    public static Document createDocument(NodeState node,
                                          ItemStateManager stateProvider,
                                          NamespaceMappings mappings,
                                          List textFilters)
            throws RepositoryException {
        NodeIndexer indexer = new NodeIndexer(node, stateProvider, mappings, textFilters);
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
        doc.add(new Field(FieldNames.UUID, node.getNodeId().getUUID().toString(), true, true, false));
        try {
            // parent UUID
            if (node.getParentId() == null) {
                // root node
                doc.add(new Field(FieldNames.PARENT, "", true, true, false));
                doc.add(new Field(FieldNames.LABEL, "", false, true, false));
            } else {
                doc.add(new Field(FieldNames.PARENT, node.getParentId().toString(), true, true, false));
                NodeState parent = (NodeState) stateProvider.getItemState(node.getParentId());
                NodeState.ChildNodeEntry child = parent.getChildNodeEntry(node.getNodeId());
                String name = NameFormat.format(child.getName(), mappings);
                doc.add(new Field(FieldNames.LABEL, name, false, true, false));
            }
        } catch (NoSuchItemStateException e) {
            throwRepositoryException(e);
        } catch (ItemStateException e) {
            throwRepositoryException(e);
        } catch (NoPrefixDeclaredException e) {
            // will never happen, because this.mappings will dynamically add
            // unknown uri<->prefix mappings
        }

        Set props = node.getPropertyNames();
        for (Iterator it = props.iterator(); it.hasNext();) {
            QName propName = (QName) it.next();
            PropertyId id = new PropertyId(node.getNodeId(), propName);
            try {
                PropertyState propState = (PropertyState) stateProvider.getItemState(id);
                InternalValue[] values = propState.getValues();
                for (int i = 0; i < values.length; i++) {
                    addValue(doc, values[i], propState.getName());
                }
                if (values.length > 1) {
                    // real multi-valued
                    addMVPName(doc, propState.getName());
                }
            } catch (NoSuchItemStateException e) {
                throwRepositoryException(e);
            } catch (ItemStateException e) {
                throwRepositoryException(e);
            }
        }
        return doc;
    }

    /**
     * Wraps the exception <code>e</code> into a <code>RepositoryException</code>
     * and throws the created exception.
     *
     * @param e the base exception.
     */
    private void throwRepositoryException(Exception e)
            throws RepositoryException {
        String msg = "Error while indexing node: " + node.getNodeId() + " of "
            + "type: " + node.getNodeTypeName();
        throw new RepositoryException(msg, e);
    }

    /**
     * Adds a {@link FieldNames#MVP} field to <code>doc</code> with the resolved
     * <code>name</code> using the internal search index namespace mapping.
     *
     * @param doc  the lucene document.
     * @param name the name of the multi-value property.
     */
    private void addMVPName(Document doc, QName name) {
        try {
            String propName = NameFormat.format(name, mappings);
            doc.add(new Field(FieldNames.MVP, propName, false, true, false));
        } catch (NoPrefixDeclaredException e) {
            // will never happen, prefixes are created dynamically
        }
    }

    /**
     * Adds a value to the lucene Document.
     *
     * @param doc   the document.
     * @param value the internal jackrabbit value.
     * @param name  the name of the property.
     */
    private void addValue(Document doc, InternalValue value, QName name) {
        String fieldName = name.getLocalName();
        try {
            fieldName = NameFormat.format(name, mappings);
        } catch (NoPrefixDeclaredException e) {
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
     * <p/>
     * This implementation checks if this {@link #node} is of type nt:resource
     * and if that is the case, tries to extract text from the data atom using
     * the {@link #textFilters}.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addBinaryValue(Document doc, String fieldName, Object internalValue) {
        // 'check' if node is of type nt:resource
        try {
            String jcrData = mappings.getPrefix(QName.NS_JCR_URI) + ":data";
            if (!jcrData.equals(fieldName)) {
                // don't know how to index
                return;
            }
            if (node.hasPropertyName(QName.JCR_MIMETYPE)) {
                PropertyState dataProp = (PropertyState) stateProvider.getItemState(
                        new PropertyId(node.getNodeId(), QName.JCR_DATA));
                PropertyState mimeTypeProp =
                        (PropertyState) stateProvider.getItemState(
                                new PropertyId(node.getNodeId(), QName.JCR_MIMETYPE));

                // jcr:encoding is not mandatory
                String encoding = null;
                if (node.hasPropertyName(QName.JCR_ENCODING)) {
                    PropertyState encodingProp =
                            (PropertyState) stateProvider.getItemState(
                                    new PropertyId(node.getNodeId(), QName.JCR_ENCODING));
                    encoding = encodingProp.getValues()[0].internalValue().toString();
                }

                String mimeType = mimeTypeProp.getValues()[0].internalValue().toString();
                Map fields = Collections.EMPTY_MAP;
                for (Iterator it = textFilters.iterator(); it.hasNext();) {
                    TextFilter filter = (TextFilter) it.next();
                    // use the first filter that can handle the mimeType
                    if (filter.canFilter(mimeType)) {
                        fields = filter.doFilter(dataProp, encoding);
                        break;
                    }
                }

                for (Iterator it = fields.keySet().iterator(); it.hasNext();) {
                    String field = (String) it.next();
                    Reader r = (Reader) fields.get(field);
                    doc.add(Field.Text(field, r));
                }
            }
        } catch (ItemStateException e) {
            log.warn("Exception while indexing binary property: " + e.toString());
            log.debug("Dump: ", e);
        } catch (RepositoryException e) {
            log.warn("Exception while indexing binary property: " + e.toString());
            log.debug("Dump: ", e);
        }
    }

    /**
     * Adds the string representation of the boolean value to the document as
     * the named field.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addBooleanValue(Document doc, String fieldName, Object internalValue) {
        doc.add(new Field(FieldNames.PROPERTIES,
                FieldNames.createNamedValue(fieldName, internalValue.toString()),
                false,
                true,
                false));
    }

    /**
     * Adds the calendar value to the document as the named field. The calendar
     * value is converted to an indexable string value using the {@link DateField}
     * class.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addCalendarValue(Document doc, String fieldName, Object internalValue) {
        long millis = ((Calendar) internalValue).getTimeInMillis();
        doc.add(new Field(FieldNames.PROPERTIES,
                FieldNames.createNamedValue(fieldName, DateField.timeToString(millis)),
                false,
                true,
                false));
    }

    /**
     * Adds the double value to the document as the named field. The double
     * value is converted to an indexable string value using the
     * {@link DoubleField} class.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addDoubleValue(Document doc, String fieldName, Object internalValue) {
        double doubleVal = ((Double) internalValue).doubleValue();
        doc.add(new Field(FieldNames.PROPERTIES,
                FieldNames.createNamedValue(fieldName, DoubleField.doubleToString(doubleVal)),
                false,
                true,
                false));
    }

    /**
     * Adds the long value to the document as the named field. The long
     * value is converted to an indexable string value using the {@link LongField}
     * class.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addLongValue(Document doc, String fieldName, Object internalValue) {
        long longVal = ((Long) internalValue).longValue();
        doc.add(new Field(FieldNames.PROPERTIES,
                FieldNames.createNamedValue(fieldName, LongField.longToString(longVal)),
                false,
                true,
                false));
    }

    /**
     * Adds the reference value to the document as the named field. The value's
     * string representation is added as the reference data. Additionally the
     * reference data is stored in the index.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addReferenceValue(Document doc, String fieldName, Object internalValue) {
        String uuid = internalValue.toString();
        doc.add(new Field(FieldNames.PROPERTIES,
                FieldNames.createNamedValue(fieldName, uuid),
                true, // store
                true,
                false));
    }

    /**
     * Adds the path value to the document as the named field. The path
     * value is converted to an indexable string value using the name space
     * mappings with which this class has been created.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addPathValue(Document doc, String fieldName, Object internalValue) {
        Path path = (Path) internalValue;
        String pathString = path.toString();
        try {
            pathString = PathFormat.format(path, mappings);
        } catch (NoPrefixDeclaredException e) {
            // will never happen
        }
        doc.add(new Field(FieldNames.PROPERTIES,
                FieldNames.createNamedValue(fieldName, pathString),
                false,
                true,
                false));
    }

    /**
     * Adds the string value to the document both as the named field and for
     * full text indexing.
     *
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
     * @param internalValue The value for the field to add to the document.
     */
    protected void addStringValue(Document doc, String fieldName, Object internalValue) {
        String stringValue = String.valueOf(internalValue);

        // simple String
        doc.add(new Field(FieldNames.PROPERTIES,
                FieldNames.createNamedValue(fieldName, stringValue),
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
        fieldName = fieldName.substring(0, idx + 1)
                + FieldNames.FULLTEXT_PREFIX + fieldName.substring(idx + 1);
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
     * @param doc           The document to which to add the field
     * @param fieldName     The name of the field to add
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
        doc.add(new Field(FieldNames.PROPERTIES,
                FieldNames.createNamedValue(fieldName, normValue),
                false,
                true,
                false));
    }
}
