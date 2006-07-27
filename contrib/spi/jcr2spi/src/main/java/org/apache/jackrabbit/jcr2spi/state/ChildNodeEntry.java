/*
 * $URL:$
 * $Id:$
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
package org.apache.jackrabbit.jcr2spi.state;

import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.QName;

/**
 * <code>ChildNodeEntry</code>...
 *
 * @author mreutegg
 * @version $Rev:$, $Date:$
 */
public interface ChildNodeEntry {

    /**
     * the cvs/svn id
     */
    static final String CVS_ID = "$URL:$ $Rev:$ $Date:$";

    NodeId getId();

    QName getName();

    int getIndex();
}
