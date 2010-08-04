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
package org.apache.jackrabbit.core;

import java.util.ArrayList;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.management.DataStoreGarbageCollector;
import org.apache.jackrabbit.api.management.RepositoryManager;
import org.apache.jackrabbit.core.RepositoryImpl.WorkspaceInfo;
import org.apache.jackrabbit.core.data.GarbageCollector;
import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.version.InternalVersionManagerImpl;

/**
 * The repository manager implementation.
 */
public class RepositoryManagerImpl implements RepositoryManager {

    private final TransientRepository tr;

    RepositoryManagerImpl(TransientRepository tr) {
        this.tr = tr;
    }

    public DataStoreGarbageCollector createDataStoreGarbageCollector() throws RepositoryException {
        RepositoryImpl rep = tr.getRepository();
        if (rep == null) {
            throw new RepositoryException("Repository is stopped");
        }
        ArrayList<PersistenceManager> pmList = new ArrayList<PersistenceManager>();
        InternalVersionManagerImpl vm = (InternalVersionManagerImpl) rep.getVersionManager();
        PersistenceManager pm = vm.getPersistenceManager();
        pmList.add(pm);
        String[] wspNames = rep.getWorkspaceNames();
        Session[] sessions = new Session[wspNames.length];
        for (int i = 0; i < wspNames.length; i++) {
            String wspName = wspNames[i];
            WorkspaceInfo wspInfo = rep.getWorkspaceInfo(wspName);
            // this will initialize the workspace if required
            SessionImpl session = SystemSession.create(rep, wspInfo.getConfig());
            // mark this session as 'active' so the workspace does not get disposed
            // by the workspace-janitor until the garbage collector is done
            rep.onSessionCreated(session);
            // the workspace could be disposed again, so re-initialize if required
            // afterwards it will not be disposed because a session is registered
            wspInfo.initialize();
            sessions[i] = session;
            pm = wspInfo.getPersistenceManager();
            pmList.add(pm);
        }
        IterablePersistenceManager[] ipmList = new IterablePersistenceManager[pmList.size()];
        for (int i = 0; i < pmList.size(); i++) {
            pm = pmList.get(i);
            if (!(pm instanceof IterablePersistenceManager)) {
                ipmList = null;
                break;
            }
            ipmList[i] = (IterablePersistenceManager) pm;
        }
        GarbageCollector gc = new GarbageCollector(rep, null, ipmList, sessions);
        return gc;
    }

    public void stop() {
        tr.shutdown();
    }

}
