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

import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ItemStateProvider;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NoPrefixDeclaredException;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.Path;
import org.apache.jackrabbit.core.QName;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

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
class NodeIndexer {

    /** The <code>NodeState</code> of the node to index */
    private final NodeState node;

    /** The persistent item state provider */
    private final ItemStateProvider stateProvider;

    /**
     * Namespace mappings to use for indexing. This is the internal
     * namespace mapping.
     */
    private final NamespaceMappings mappings;

    /**
     * Creates a new node indexer.
     * @param node the node state to index.
     * @param stateProvider the persistent item state manager to retrieve properties.
     * @param mappings internal namespace mappings.
     */
    private NodeIndexer(NodeState node,
                        ItemStateProvider stateProvider,
                        NamespaceMappings mappings) {
        this.node = node;
        this.stateProvider = stateProvider;
        this.mappings = mappings;
    }

    /**
     * Creates a lucene Document from a node.
     * @param node the node state to index.
     * @param stateProvider the state provider to retrieve property values.
     * @param mappings internal namespace mappings.
     * @return the lucene Document.
     * @throws RepositoryException if an error occurs while reading property
     *   values from the <code>ItemStateProvider</code>.
     */
    public static Document createDocument(NodeState node,
                                          ItemStateProvider stateProvider,
                                          NamespaceMappings mappings)
            throws RepositoryException {
        NodeIndexer indexer = new NodeIndexer(node, stateProvider, mappings);
        return indexer.createDoc();
    }

    /**
     * Creates a lucene Document.
     * @return the lucene Document with the index layout.
     * @throws RepositoryException if an error occurs while reading property
     *   values from the <code>ItemStateProvider</code>.
     */
    private Document createDoc() throws RepositoryException {
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
                NodeState parent = (NodeState) stateProvider.getItemState(
                        new NodeId(node.getParentUUID()));
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
                for (int i = 0; i < values.length; i++) {
                    addValue(doc, values[i], propState.getName());
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
     * @param doc the document.
     * @param value the internal jackrabbit value.
     * @param name the name of the property.
     */
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
