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
package org.apache.jackrabbit.jcr2spi.xml;

import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;

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
        private final QName name;
        private final QName nodeTypeName;
        private final QName[] mixinNames;
        private String uuid;

        public NodeInfo(QName name, QName nodeTypeName, QName[] mixinNames, String uuid) {
            this.name = name;
            this.nodeTypeName = nodeTypeName;
            this.mixinNames = mixinNames;
            this.uuid = uuid;
        }

        public QName getName() {
            return name;
        }

        public QName getNodeTypeName() {
            return nodeTypeName;
        }

        public QName[] getMixinNames() {
            return mixinNames;
        }

        public void setUUID(String uuid) {
            this.uuid = uuid;
        }

        public String getUUID() {
            return uuid;
        }
    }

    static class PropInfo {
        private final QName name;
        private final int type;
        private final TextValue[] values;

        public PropInfo(QName name, int type, TextValue[] values) {
            this.name = name;
            this.type = type;
            this.values = values;
        }

        public QName getName() {
            return name;
        }

        public int getType() {
            return type;
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
