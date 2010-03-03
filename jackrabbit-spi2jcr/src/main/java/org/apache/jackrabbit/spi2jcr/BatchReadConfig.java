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
package org.apache.jackrabbit.spi2jcr;

import org.apache.jackrabbit.spi.Name;

import java.util.Map;
import java.util.HashMap;

/**
 * <code>BatchReadConfig</code> defines if and how deep child item
 * information should be retrieved, when accessing a <code>Node</code>.
 * The configuration is based on node type names.
 */
public class BatchReadConfig {

    public static final int DEPTH_DEFAULT = 0;
    public static final int DEPTH_INFINITE = -1;

    private Map<Name, Integer> depthMap = new HashMap<Name, Integer>(0);

    /**
     * Return the depth for the given node type name. If the name is
     * not defined in this configuration, the {@link #DEPTH_DEFAULT default value}
     * is returned.
     *
     * @param ntName
     * @return {@link #DEPTH_INFINITE -1} If all child infos should be return or
     * any value greater than {@link #DEPTH_DEFAULT 0} if only parts of the
     * subtree should be returned. If the given nodetype name is not defined
     * in this configuration, the default depth {@link #DEPTH_DEFAULT 0} will
     * be returned.
     */
    public int getDepth(Name ntName) {
        if (depthMap.containsKey(ntName)) {
            return depthMap.get(ntName);
        } else {
            return DEPTH_DEFAULT;
        }
    }

    /**
     * Define the batch-read depth for the given node type name.
     *
     * @param ntName
     * @param depth
     */
    public void setDepth(Name ntName, int depth) {
        if (ntName == null || depth < DEPTH_INFINITE) {
            throw new IllegalArgumentException();
        }
        depthMap.put(ntName, depth);
    }
}
