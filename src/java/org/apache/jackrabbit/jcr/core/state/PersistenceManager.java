/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.jcr.core.state;

import org.apache.jackrabbit.jcr.core.QName;
import org.apache.jackrabbit.jcr.core.WorkspaceDef;

/**
 * <code>PersistenceManager</code> ...
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.12 $, $Date: 2004/09/03 17:16:22 $
 */
public interface PersistenceManager {

    /**
     * @param wspDef
     * @throws Exception
     */
    public void init(WorkspaceDef wspDef) throws Exception;

    /**
     * @param uuid
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    public PersistentNodeState loadNodeState(String uuid)
	    throws NoSuchItemStateException, ItemStateException;

    /**
     * @param state
     * @throws ItemStateException
     */
    public void reload(PersistentNodeState state) throws ItemStateException;

    /**
     * @param parentUUID
     * @param propName
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    public PersistentPropertyState loadPropertyState(String parentUUID, QName propName)
	    throws NoSuchItemStateException, ItemStateException;

    /**
     * @param state
     * @throws ItemStateException
     */
    public void reload(PersistentPropertyState state) throws ItemStateException;

    /**
     * @param uuid
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    public NodeReferences loadNodeReferences(String uuid)
	    throws NoSuchItemStateException, ItemStateException;

    /**
     * @param refs
     * @throws ItemStateException
     */
    public void reload(NodeReferences refs) throws ItemStateException;

    /**
     * @param state
     * @throws ItemStateException
     */
    public void store(PersistentNodeState state) throws ItemStateException;

    /**
     * @param state
     * @throws ItemStateException
     */
    public void store(PersistentPropertyState state) throws ItemStateException;

    /**
     * @param refs
     * @throws ItemStateException
     */
    public void store(NodeReferences refs) throws ItemStateException;

    /**
     * @param state
     * @throws ItemStateException
     */
    public void destroy(PersistentNodeState state) throws ItemStateException;

    /**
     * @param state
     * @throws ItemStateException
     */
    public void destroy(PersistentPropertyState state) throws ItemStateException;

    /**
     * @param refs
     * @throws ItemStateException
     */
    public void destroy(NodeReferences refs) throws ItemStateException;

    //------------------------------------------------------< factory methods >
    /**
     * @param uuid
     * @param nodeTypeName
     * @return
     */
    public PersistentNodeState createNodeStateInstance(String uuid, QName nodeTypeName);

    /**
     * @param name
     * @param parentUUID
     * @return
     */
    public PersistentPropertyState createPropertyStateInstance(QName name, String parentUUID);

    /**
     * @param uuid
     * @return
     */
    public NodeReferences createNodeReferencesInstance(String uuid);
}
