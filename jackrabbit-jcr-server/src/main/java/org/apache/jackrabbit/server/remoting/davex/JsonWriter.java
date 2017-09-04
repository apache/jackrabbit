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

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.json.JsonUtil;

/**
 * <code>JsonWriter</code> traverses a tree of JCR items and writes a JSON object
 * exposing nodes as JSON object members and properties as JSON pairs.
 * <p>
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

    void write(Node node, int maxLevels) throws RepositoryException, IOException {
        write(node, 0, maxLevels);
    }

    void write(Collection<Node> nodes, int maxLevels)
            throws RepositoryException, IOException {
        writer.write('{');
        writeKey("nodes");
        writer.write('{');
        boolean first = true;
        for (Node node : nodes) {
            if (first) {
                first = false;
            } else {
                writer.write(',');
            }
            writeKey(node.getPath());
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
            writeProperty(prop);
            // add separator: next json pair/member is either a property or
            // a childnode or the special no-children-present pair.
            writer.write(',');
        }

        // for jcr child nodes include member unless the max-depths is reached.
        // in case there are no children at all, append a special pair.
        final NodeIterator children = node.getNodes();
        if (!children.hasNext()) {
            // no child present at all -> add special property.
            writeKeyValue("::NodeIteratorSize", 0);
        } else {
            // the child nodes
            while (children.hasNext()) {
                final Node n = children.nextNode();
                String name = n.getName();
                int index = n.getIndex();
                if (index > 1) {
                    writeKey(name + "[" + index + "]");
                } else {
                    writeKey(name);
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
                    writeChildInfo(n);
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
     * @param n
     * @throws RepositoryException
     * @throws IOException
     */
    private void writeChildInfo(Node n) throws RepositoryException, IOException {
        // start child info
        writer.write('{');

        // make sure the SPI childInfo can be built correctly on the
        // client side -> pass uuid if present.
        if (n.isNodeType(JcrConstants.MIX_REFERENCEABLE) &&
                n.hasProperty(JcrConstants.JCR_UUID)) {
            writeProperty(n.getProperty(JcrConstants.JCR_UUID));
        }

        // end child info
        writer.write('}');
    }

    /**
     * Write a single property
     *
     * @param p
     * @throws javax.jcr.RepositoryException
     * @throws java.io.IOException
     */
    private void writeProperty(Property p) throws RepositoryException, IOException {
        // special handling for binaries: we dump the length and not the length
        int type = p.getType();
        if (type == PropertyType.BINARY) {
            // mark binary properties with a leading ':'
            // the value(s) reflect the jcr-values length instead of the binary data.
            String key = ":" + p.getName();
            if (p.isMultiple()) {
                long[] binLengths = p.getLengths();
                writeKeyArray(key, binLengths);
            } else {
                writeKeyValue(key, p.getLength());
            }
        } else {
            boolean isMultiple = p.isMultiple();
            if (requiresTypeInfo(p) || (isMultiple && p.getValues().length == 0)) {
                /* special property types that have no correspondence in JSON
                   are transported as String. the type is transported with an
                   extra key-value pair, the key having a leading ':' the value
                   reflects the type.
                   the same applies for multivalued properties consisting of an
                   empty array -> property type guessing would not be possible.
                 */
                writeKeyValue(":" +  p.getName(), PropertyType.nameFromValue(type), true);
            }
            /* append key-value pair containing the jcr value(s).
               for String, Boolean, Double, Long -> types in json available */
            if (isMultiple) {
                writeKeyArray(p.getName(), p.getValues());
            } else {
                writeKeyValue(p.getName(), p.getValue());
            }
        }
    }

    private static boolean requiresTypeInfo(Property p) throws RepositoryException {
        switch (p.getType()) {
            case PropertyType.NAME:
            case PropertyType.PATH:
            case PropertyType.REFERENCE:
            case PropertyType.DATE:
            case PropertyType.WEAKREFERENCE:
            case PropertyType.URI:
            case PropertyType.DECIMAL:
            case PropertyType.DOUBLE:
                return true;
            default:
                // any other property type
                return false;
        }
    }

    private void writeKeyValue(String key, String value, boolean hasNext) throws IOException {
        writeKey(key);
        writer.write(JsonUtil.getJsonString(value));
        if (hasNext) {
            writer.write(',');
        }
    }

    private void writeKeyValue(String key, Value value) throws RepositoryException, IOException {
        writeKey(key);
        writeJsonValue(value);
    }

    private void writeKeyArray(String key, Value[] values) throws RepositoryException, IOException {
        writeKey(key);
        writer.write('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                writer.write(',');
            }
            writeJsonValue(values[i]);
        }
        writer.write(']');
    }

    private void writeKeyValue(String key, long binLength) throws IOException {
        writeKey(key);
        writer.write(String.valueOf(binLength));
    }

    private void writeKeyArray(String key, long[] binLengths) throws RepositoryException, IOException {
        writeKey(key);
        writer.write('[');
        for (int i = 0; i < binLengths.length; i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(String.valueOf(binLengths[i]));
        }
        writer.write(']');
    }

   private void writeKey(String key) throws IOException {
        writer.write(JsonUtil.getJsonString(key));
        writer.write(':');
    }

    private void writeJsonValue(Value v) throws RepositoryException, IOException {

        switch (v.getType()) {
            case PropertyType.BINARY:
                // should never get here
                throw new IllegalArgumentException();

            case PropertyType.BOOLEAN:
            case PropertyType.LONG:
                writer.write(v.getString());
                break;
            case PropertyType.DOUBLE:
                double d = v.getDouble();
                String str = v.getString();
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    str = JsonUtil.getJsonString(str);
                }
                writer.write(str);
                break;

            default:
                writer.write(JsonUtil.getJsonString(v.getString()));
        }
    }
}
