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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Enumeration;
import java.io.InputStream;
import java.io.IOException;

/**
 * <code>BatchReadConfig</code> defines if and how deep child item
 * information should be retrieved, when accessing a <code>Node</code>.
 * The configuration is based on node type names.
 */
class BatchReadConfig {

    private static Logger log = LoggerFactory.getLogger(BatchReadConfig.class);

    private static final String NAME_DEFAULT = "default";
    public static final int DEPTH_DEFAULT = 0;
    public static final int DEPTH_INFINITE = -1;

    private int defaultDepth = DEPTH_DEFAULT;
    private final Map<String, Integer> depthMap = new HashMap<String, Integer>();

    /**
     * Create an empty batch-read config.
     */
    BatchReadConfig() {}

    /**
     * Load the batch read configuration.
     * 
     * @param in An input stream.
     * @throws IOException If an error occurs.
     */
    public void load(InputStream in) throws IOException {
        Properties props = new Properties();
        props.load(in);
        add(props);
    }

    /**
     * Add the configuration entries present in the given properties.
     *
     * @param props
     */
    public void add(Properties props) {
        for (Enumeration<?> en = props.propertyNames(); en.hasMoreElements();) {
            String name = en.nextElement().toString();
            String depthStr = props.getProperty(name);
            try {
                int depth = Integer.parseInt(depthStr);
                if (depth < DEPTH_INFINITE) {
                    log.warn("invalid depth " + depthStr + " -> ignoring.");
                    continue;
                }
                if (NAME_DEFAULT.equals(name)) {
                    setDefaultDepth(depth);
                } else {
                    setDepth(name, depth);
                }
            } catch (NumberFormatException e) {
                // invalid entry in the properties file -> ignore
                log.warn("Invalid depth value for name " + name + ". " + depthStr + " cannot be parsed into an integer.");
            }
        }
    }

    /**
     * Return the depth for the given node type name. If the name is
     * not defined in this configuration, the {@link #DEPTH_DEFAULT default value}
     * is returned.
     *
     * @param ntName The jcr name of the node type.
     * @return {@link #DEPTH_INFINITE -1} If all child infos should be return or
     * any value greater than {@link #DEPTH_DEFAULT 0} if only parts of the
     * subtree should be returned. If the given nodetype name is not defined
     * in this configuration, the default depth {@link #DEPTH_DEFAULT 0} will
     * be returned.
     */
    public int getDepth(String ntName) {
        if (depthMap.containsKey(ntName)) {
            return depthMap.get(ntName);
        } else {
            return defaultDepth;
        }
    }

    /**
     * Return the depth for the given node or the default depth if the config
     * does not provide an specific entry for the given node.
     *
     * @param node The node for with depth information should be retrieved.
     * @return {@link #DEPTH_INFINITE -1} If all child infos should be return or
     * any value greater than {@link #DEPTH_DEFAULT 0} if only parts of the
     * subtree should be returned.
     */
    public int getDepth(Node node) {
        int depth = defaultDepth;
        try {
            String ntName = node.getPrimaryNodeType().getName();
            if (depthMap.containsKey(ntName)) {
                depth = depthMap.get(ntName);
            }
        } catch (RepositoryException e) {
            // ignore and return default.
        }
        return depth;
    }

    /**
     * Define the batch-read depth for the given node type name.
     *
     * @param ntName jcr name of the node type for which <code>depth</code> is defined.
     * @param depth Depth for the specified node type name.
     * @throws IllegalArgumentException if <code>ntName</code> is <code>null</code>
     * or <code>depth</code> is lower than {@link #DEPTH_INFINITE}.
     */
    public void setDepth(String ntName, int depth) {
        if (ntName == null || depth < DEPTH_INFINITE) {
            throw new IllegalArgumentException();
        }
        depthMap.put(ntName, depth);
    }

    /**
     * Returns the default depth.
     *
     * @return  the default depth.
     */
    public int getDefaultDepth() {
        return defaultDepth;
    }

    /**
     * Set the default depth.
     *
     * @param depth The default depth.
     * @throws IllegalArgumentException if <code>depth</code> is lower than
     * {@link #DEPTH_INFINITE}.
     */
    public void setDefaultDepth(int depth) {
        if (depth < -1) {
            throw new IllegalArgumentException();
        }
        defaultDepth = depth;
    }
}
