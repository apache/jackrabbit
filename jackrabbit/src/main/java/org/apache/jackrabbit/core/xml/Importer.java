/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.core.xml;

import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.core.NodeId;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * The <code>Importer</code> interface ...
 */
public interface Importer {

    /**
     * @throws RepositoryException
     */
    void start() throws RepositoryException;

    /**
     * @param nodeInfo
     * @param propInfos list of <code>PropInfo</code> instances
     * @param nsContext prefix mappings of current context
     * @throws RepositoryException
     */
    void startNode(NodeInfo nodeInfo, List propInfos, NamespaceResolver nsContext)
            throws RepositoryException;

    /**
     * @param nodeInfo
     * @throws RepositoryException
     */
    void endNode(NodeInfo nodeInfo) throws RepositoryException;

    /**
     * @throws RepositoryException
     */
    void end() throws RepositoryException;

    //--------------------------------------------------------< inner classes >
    static class NodeInfo {
        private QName name;
        private QName nodeTypeName;
        private QName[] mixinNames;
        private NodeId id;

        public NodeInfo() {
        }

        public NodeInfo(QName name, QName nodeTypeName, QName[] mixinNames,
                        NodeId id) {
            this.name = name;
            this.nodeTypeName = nodeTypeName;
            this.mixinNames = mixinNames;
            this.id = id;
        }

        public void setName(QName name) {
            this.name = name;
        }

        public QName getName() {
            return name;
        }

        public void setNodeTypeName(QName nodeTypeName) {
            this.nodeTypeName = nodeTypeName;
        }

        public QName getNodeTypeName() {
            return nodeTypeName;
        }

        public void setMixinNames(QName[] mixinNames) {
            this.mixinNames = mixinNames;
        }

        public QName[] getMixinNames() {
            return mixinNames;
        }

        public void setId(NodeId id) {
            this.id = id;
        }

        public NodeId getId() {
            return id;
        }
    }

    static class PropInfo {
        private QName name;
        private int type;
        private TextValue[] values;

        public PropInfo() {
        }

        public PropInfo(QName name, int type, TextValue[] values) {
            this.name = name;
            this.type = type;
            this.values = values;
        }

        public void setName(QName name) {
            this.name = name;
        }

        public QName getName() {
            return name;
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public void setValues(TextValue[] values) {
            this.values = values;
        }

        public TextValue[] getValues() {
            return values;
        }
    }

    /**
     * <code>TextValue</code> represents a serialized property value read
     * from a System or Document View XML document.
     */
    interface TextValue {
        /**
         * Returns the length of the serialized value.
         *
         * @return the length of the serialized value
         * @throws IOException if an I/O error occurs
         */
        long length() throws IOException;

        /**
         * Retrieves the serialized value.
         *
         * @return the serialized value
         * @throws IOException if an I/O error occurs
         */
        String retrieve() throws IOException;

        /**
         * Returns a <code>Reader</code> for reading the serialized value.
         *
         * @return a <code>Reader</code> for reading the serialized value.
         * @throws IOException if an I/O error occurs
         */
        Reader reader() throws IOException;
    }
}
