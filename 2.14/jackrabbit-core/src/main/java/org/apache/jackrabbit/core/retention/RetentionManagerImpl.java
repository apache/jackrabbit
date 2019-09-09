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
package org.apache.jackrabbit.core.retention;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.ProtectedItemModifier;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.retention.Hold;
import javax.jcr.retention.RetentionManager;
import javax.jcr.retention.RetentionPolicy;
import javax.jcr.version.VersionException;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>RetentionManagerImpl</code>...
 */
public class RetentionManagerImpl extends ProtectedItemModifier implements RetentionManager {

    private static Logger log = LoggerFactory.getLogger(RetentionManagerImpl.class);

    private static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();

    static final Name REP_RETENTION_MANAGEABLE = NAME_FACTORY.create(Name.NS_REP_URI, "RetentionManageable");
    static final Name REP_HOLD = NAME_FACTORY.create(Name.NS_REP_URI, "hold");
    static final Name REP_RETENTION_POLICY = NAME_FACTORY.create(Name.NS_REP_URI, "retentionPolicy");

    private final SessionImpl session;

    /**
     *
     * @param session The editing session.
     */
    public RetentionManagerImpl(SessionImpl session) {
        super(Permission.RETENTION_MNGMT);
        this.session = session;
    }
    
    //---------------------------------------------------< RetentionManager >---
    /**
     * @see RetentionManager#getHolds(String)
     */
    public Hold[] getHolds(String absPath) throws PathNotFoundException,
            AccessDeniedException, RepositoryException {

        NodeImpl n = (NodeImpl) session.getNode(absPath);
        session.getAccessManager().checkPermission(session.getQPath(absPath), Permission.RETENTION_MNGMT);        

        Hold[] holds;
        if (n.isNodeType(REP_RETENTION_MANAGEABLE) && n.hasProperty(REP_HOLD)) {
            holds = HoldImpl.createFromProperty(n.getProperty(REP_HOLD), n.getNodeId());
        } else {
            holds = new Hold[0];
        }
        return holds;
    }

    /**
     * @see RetentionManager#addHold(String, String, boolean) 
     */
    public Hold addHold(String absPath, String name, boolean isDeep) throws
            PathNotFoundException, AccessDeniedException, LockException,
            VersionException, RepositoryException {

        NodeImpl n = (NodeImpl) session.getNode(absPath);
        if (!n.isNodeType(REP_RETENTION_MANAGEABLE)) {
            n.addMixin(REP_RETENTION_MANAGEABLE);
        }

        HoldImpl hold = new HoldImpl(session.getQName(name), isDeep, n.getNodeId(), session);
        Value[] vls;
        if (n.hasProperty(REP_HOLD)) {
            Value[] vs = n.getProperty(REP_HOLD).getValues();
            // check if the same hold already exists
            for (Value v : vs) {
                if (hold.equals(HoldImpl.createFromValue(v, n.getNodeId(), session))) {
                    throw new RepositoryException("Hold already exists.");
                }
            }
            vls = new Value[vs.length + 1];
            System.arraycopy(vs, 0, vls, 0, vs.length);
        } else {
            vls = new Value[1];
        }

        // add the value of the new hold
        vls[vls.length - 1] = hold.toValue(session.getValueFactory());
        setProperty(n, REP_HOLD, vls);
        return hold;
    }

    /**
     * @see RetentionManager#removeHold(String, Hold) 
     */
    public void removeHold(String absPath, Hold hold) throws
            PathNotFoundException, AccessDeniedException, LockException,
            VersionException, RepositoryException {

        NodeImpl n = (NodeImpl) session.getNode(absPath);
        if (hold instanceof HoldImpl
                && n.getNodeId().equals(((HoldImpl) hold).getNodeId())
                && n.isNodeType(REP_RETENTION_MANAGEABLE)
                && n.hasProperty(REP_HOLD)) {

            PropertyImpl p = n.getProperty(REP_HOLD);
            Value[] vls = p.getValues();

            List<Value> newValues = new ArrayList<Value>(vls.length - 1);
            for (Value v : vls) {
                if (!hold.equals(HoldImpl.createFromValue(v, n.getNodeId(), session))) {
                    newValues.add(v);
                }
            }
            if (newValues.size() < vls.length) {
                if (newValues.size() == 0) {
                    removeItem(p);
                } else {
                    setProperty(n, REP_HOLD, newValues.toArray(new Value[newValues.size()]));
                }
            } else {
                // no matching hold.
                throw new RepositoryException("Cannot remove '" + hold.getName() + "' at " + absPath + ".");
            }
        } else {
            // invalid hold or no hold at absPath
            throw new RepositoryException("Cannot remove '" + hold.getName() + "' at " + absPath + ".");
        }
    }

    /**
     * @see RetentionManager#getRetentionPolicy(String) 
     */
    public RetentionPolicy getRetentionPolicy(String absPath) throws
            PathNotFoundException, AccessDeniedException, RepositoryException {

        NodeImpl n = (NodeImpl) session.getNode(absPath);
        session.getAccessManager().checkPermission(session.getQPath(absPath), Permission.RETENTION_MNGMT);

        RetentionPolicy rPolicy = null;
        if (n.isNodeType(REP_RETENTION_MANAGEABLE) && n.hasProperty(REP_RETENTION_POLICY)) {
            String jcrName = n.getProperty(REP_RETENTION_POLICY).getString();
            rPolicy = new RetentionPolicyImpl(jcrName, n.getNodeId(), session);
        }
        
        return rPolicy;
    }

    /**
     * @see RetentionManager#setRetentionPolicy(String, RetentionPolicy)
     */
    public void setRetentionPolicy(String absPath, RetentionPolicy retentionPolicy)
            throws PathNotFoundException, AccessDeniedException, LockException,
            VersionException, RepositoryException {

        NodeImpl n = (NodeImpl) session.getNode(absPath);
        if (!(retentionPolicy instanceof RetentionPolicyImpl)) {
            throw new RepositoryException("Invalid retention policy.");
        }
        Value retentionReference = session.getValueFactory().createValue(retentionPolicy.getName(), PropertyType.NAME);
        if (!n.isNodeType(REP_RETENTION_MANAGEABLE)) {
            n.addMixin(REP_RETENTION_MANAGEABLE);
        }
        setProperty(n, REP_RETENTION_POLICY, retentionReference);
    }

    /**
     * @see RetentionManager#removeRetentionPolicy(String) 
     */
    public void removeRetentionPolicy(String absPath) throws
            PathNotFoundException, AccessDeniedException, LockException,
            VersionException, RepositoryException {

        NodeImpl n = (NodeImpl) session.getNode(absPath);
        if (n.isNodeType(REP_RETENTION_MANAGEABLE) && n.hasProperty(REP_RETENTION_POLICY)) {
            removeItem(n.getProperty(REP_RETENTION_POLICY));
        } else {
            throw new RepositoryException("Cannot remove retention policy at absPath.");
        }
    }
}