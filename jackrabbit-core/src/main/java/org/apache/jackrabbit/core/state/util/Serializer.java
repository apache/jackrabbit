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
package org.apache.jackrabbit.core.state.util;

import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Legacy class kept for backward compatibility reasons.
 * @deprecated use {@link org.apache.jackrabbit.core.persistence.util.Serializer}
 *             instead.
 */
public final class Serializer {

    /**
     * @deprecated use {@link org.apache.jackrabbit.core.persistence.util.Serializer#serialize(NodeState, OutputStream)}
     */
    public static void serialize(NodeState state, OutputStream stream)
            throws Exception {
        // delegate to replacement
        org.apache.jackrabbit.core.persistence.util.Serializer.serialize(state, stream);
    }

    /**
     * @deprecated use {@link org.apache.jackrabbit.core.persistence.util.Serializer#deserialize(NodeState, InputStream)}
     */
    public static void deserialize(NodeState state, InputStream stream)
            throws Exception {
        // delegate to replacement
        org.apache.jackrabbit.core.persistence.util.Serializer.deserialize(state, stream);
    }

    /**
     * @deprecated use {@link org.apache.jackrabbit.core.persistence.util.Serializer#serialize(PropertyState, OutputStream, org.apache.jackrabbit.core.persistence.util.BLOBStore)}
     */
    public static void serialize(PropertyState state,
                                 OutputStream stream,
                                 BLOBStore blobStore)
            throws Exception {
        // delegate to replacement
        org.apache.jackrabbit.core.persistence.util.Serializer.serialize(state, stream, blobStore);
    }

    /**
     * @deprecated use {@link org.apache.jackrabbit.core.persistence.util.Serializer#deserialize(PropertyState, InputStream, org.apache.jackrabbit.core.persistence.util.BLOBStore)}
     */
    public static void deserialize(PropertyState state,
                                   InputStream stream,
                                   BLOBStore blobStore)
            throws Exception {
        // delegate to replacement
        org.apache.jackrabbit.core.persistence.util.Serializer.deserialize(state, stream, blobStore);
    }

    /**
     * @deprecated use {@link org.apache.jackrabbit.core.persistence.util.Serializer#serialize(NodeReferences, OutputStream)}
     */
    public static void serialize(NodeReferences refs, OutputStream stream)
            throws Exception {
        // delegate to replacement
        org.apache.jackrabbit.core.persistence.util.Serializer.serialize(refs, stream);
    }

    /**
     * @deprecated user {@link org.apache.jackrabbit.core.persistence.util.Serializer#deserialize(NodeReferences, InputStream)}
     */
    public static void deserialize(NodeReferences refs, InputStream stream)
            throws Exception {
        // delegate to replacement
        org.apache.jackrabbit.core.persistence.util.Serializer.deserialize(refs, stream);
    }

}
