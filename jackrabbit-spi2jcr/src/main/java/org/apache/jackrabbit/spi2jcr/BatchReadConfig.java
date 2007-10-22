/*
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
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

    private Map depthMap = new HashMap(0);

    /**
     * Return the depth for the given qualified node type name. If the name is
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
            return ((Integer) (depthMap.get(ntName))).intValue();
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
        depthMap.put(ntName, new Integer(depth));
    }
}
