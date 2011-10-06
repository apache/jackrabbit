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
package org.apache.jackrabbit.server.remoting.davex;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.Writer;
import java.io.IOException;
import java.util.Collection;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.json.JsonUtil;

/**
 * <code>JsonWriter</code> traverses a tree of JCR items and writes a JSON object
 * exposing nodes as JSON object members and properties as JSON pairs.
 * <p/>
 * <strong>Note</strong>: Using JSON.org library is deliberately avoided for the
 * following reasons.
 * <ul>
 * <li>JSONObject does not preserve the order of members added, which is required
 * for JCR remoting.</li>
 * <li>JSONObject#numberToString:
 * Double numbers get their trailing '.0' stripped away, which removes
 * the ability to distinguish between JCR values of type {@link PropertyType#DOUBLE}
 * and {@link PropertyType#LONG}.</li>
 * </ul>
 */
class JsonWriter {

    private final Writer writer;
    
    /**
     * Create a new JsonItemWriter
     * 
     * @param writer Writer to which the generated JSON string is written.
     */
    JsonWriter(Writer writer) {
        this.writer = writer;
    }

    /**
     * 
     * @param node
     * @param maxLevels
     * @throws RepositoryException
     * @throws IOException
     */
    void write(Node node, int maxLevels) throws RepositoryException, IOException {
        write(node, 0, maxLevels);
    }

    void write(Collection<Node> nodes, int maxLevels)
            throws RepositoryException, IOException {
        writer.write('{');
        writeKey(writer, "nodes");
        writer.write('{');
        boolean first = true;
        for (Node node : nodes) {
            if (first) {
                first = false;
            } else {
                writer.write(',');
            }
            writeKey(writer, node.getPath());
            write(node, maxLevels);
        }
        writer.write('}');
        writer.write('}');
    }

    private void write(Node node, int currentLevel, int maxLevels)
            throws RepositoryException, IOException {
        // start of node info
        writer.write('{');

        // append the jcr properties as JSON pairs.
        PropertyIterator props = node.getProperties();        
        while (props.hasNext()) {
            Property prop = props.nextProperty();
            writeProperty(writer, prop);
            // add separator: next json pair/member is either a property or
            // a childnode or the special no-children-present pair.
            writer.write(',');
        }

        // for jcr child nodes include member unless the max-depths is reached.
        // in case there are no children at all, append a special pair.
        final NodeIterator children = node.getNodes();
        if (!children.hasNext()) {
            // no child present at all -> add special property.
            writeKeyValue(writer, "::NodeIteratorSize", 0);
        } else {
            // the child nodes
            while (children.hasNext()) {
                final Node n = children.nextNode();
                String name = n.getName();
                int index = n.getIndex();
                if (index > 1) {
                    writeKey(writer, name + "[" + index + "]");
                } else {
                    writeKey(writer, name);
                }
                if (maxLevels < 0 || currentLevel < maxLevels) {
                    write(n, currentLevel + 1, maxLevels);
                } else {
                    /**
                     * In order to be able to compute the set of child-node entries
                     * upon Node creation -> add incomplete "node info" JSON
                     * object for the child node omitting properties and child
                     * information except for the jcr:uuid property (if present
                     * at all).
                     * the latter is required in order to build the correct SPI
                     * ChildInfo for Node n.
                     */
                    writeChildInfo(writer, n);
                }
                if (children.hasNext()) {
                    writer.write(',');
                }
            }
        }

        // end of node info
        writer.write('}');
    }

    /**
     * Write child info without including the complete node info.
     * 
     * @param w
     * @param n
     * @throws RepositoryException
     * @throws IOException
     */
    private static void writeChildInfo(Writer w, Node n) throws RepositoryException, IOException {
        // start child info
        w.write('{');

        // make sure the SPI childInfo can be built correctly on the
        // client side -> pass uuid if present.
        if (n.isNodeType(JcrConstants.MIX_REFERENCEABLE) &&
                n.hasProperty(JcrConstants.JCR_UUID)) {
            writeProperty(w, n.getProperty(JcrConstants.JCR_UUID));
        }

        // end child info
        w.write('}');
    }

    /**
     * Write a single property
     *
     * @param w
     * @param p
     * @throws javax.jcr.RepositoryException
     * @throws java.io.IOException
     */
    private static void writeProperty(Writer w, Property p) throws RepositoryException, IOException {
        // special handling for binaries: we dump the length and not the length
        int type = p.getType();
        if (type == PropertyType.BINARY) {
            // mark binary properties with a leading ':'
            // the value(s) reflect the jcr-values length instead of the binary data.
            String key = ":" + p.getName();
            if (p.isMultiple()) {
                long[] binLengths = p.getLengths();
                writeKeyArray(w, key, binLengths);
            } else {
                writeKeyValue(w, key, p.getLength());
            }
        } else {
            boolean isMultiple = p.isMultiple();
            if (requiresTypeInfo(type) || (isMultiple && p.getValues().length == 0)) {
                /* special property types that have no correspondence in JSON
                   are transported as String. the type is transported with an
                   extra key-value pair, the key having a leading ':' the value
                   reflects the type. 
                   the same applies for multivalued properties consisting of an
                   empty array -> property type guessing would not be possible.
                 */
                writeKeyValue(w, ":" +  p.getName(), PropertyType.nameFromValue(type), true);
            }
            /* append key-value pair containing the jcr value(s).
               for String, Boolean, Double, Long -> types in json available */
            if (isMultiple) {
                writeKeyArray(w, p.getName(), p.getValues());
            } else {
                writeKeyValue(w, p.getName(), p.getValue());
            }
        }
    }

    private static boolean requiresTypeInfo(int type) {
        switch (type) {
            case PropertyType.NAME:
            case PropertyType.PATH:
            case PropertyType.REFERENCE:
            case PropertyType.DATE:
            case PropertyType.WEAKREFERENCE:
            case PropertyType.URI:
            case PropertyType.DECIMAL:
                return true;
            default:
                // any other property type
                return false;
        }
    }

    private static void writeKeyValue(Writer w, String key, String value, boolean hasNext) throws IOException {
        writeKey(w, key);
        w.write(JsonUtil.getJsonString(value));
        if (hasNext) {
            w.write(',');           
        }
    }

    private static void writeKeyValue(Writer w, String key, Value value) throws RepositoryException, IOException {
        writeKey(w, key);
        w.write(getJsonValue(value));
    }

    private static void writeKeyArray(Writer w, String key, Value[] values) throws RepositoryException, IOException {
        writeKey(w, key);
        w.write('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                w.write(',');
            }
            w.write(getJsonValue(values[i]));
        }
        w.write(']');
    }
    
    private static void writeKeyValue(Writer w, String key, long binLength) throws IOException {
        writeKey(w, key);
        w.write(binLength + "");
    }
    
    private static void writeKeyArray(Writer w, String key, long[] binLengths) throws RepositoryException, IOException {
        writeKey(w, key);
        w.write('[');
        for (int i = 0; i < binLengths.length; i++) {
            String delim = (i == 0) ? "" : ",";
            w.write(delim + binLengths[i]);
        }
        w.write(']');
    }

    /**
     *
     * @param w
     * @param key
     * @throws IOException
     */
    private static void writeKey(Writer w, String key) throws IOException {
        w.write(JsonUtil.getJsonString(key));
        w.write(':');
    }

    /**
     * @param v
     * @throws RepositoryException
     * @throws IOException
     */
    private static String getJsonValue(Value v) throws RepositoryException, IOException {

        switch (v.getType()) {
            case PropertyType.BINARY:
                // should never get here
                throw new IllegalArgumentException();

            case PropertyType.BOOLEAN:
            case PropertyType.LONG:
            case PropertyType.DOUBLE:
                return v.getString();

            default:
                return JsonUtil.getJsonString(v.getString());
        }
    }
}
