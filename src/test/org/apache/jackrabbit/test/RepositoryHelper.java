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
package org.apache.jackrabbit.test;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
public class RepositoryHelper {

    public Repository getRepository() throws RepositoryException {
        try {
            RepositoryStub repStub = RepositoryStub.getInstance();
            return repStub.getRepository();
        } catch (RepositoryStubException e) {
            throw new RepositoryException("Failed to get Repository instance.", e);
        }
    }

    public Session getSuperuserSession() throws RepositoryException {
        try {
            RepositoryStub repStub = RepositoryStub.getInstance();
            return repStub.getRepository().login(repStub.getSuperuserCredentials(), null);
        } catch (RepositoryStubException e) {
            throw new RepositoryException("Failed to login to Repository.", e);
        }
    }

    public Session getReadWriteSession() throws RepositoryException {
        try {
            RepositoryStub repStub = RepositoryStub.getInstance();
            return repStub.getRepository().login(repStub.getReadWriteCredentials(), null);
        } catch (RepositoryStubException e) {
            throw new RepositoryException("Failed to login to Repository.", e);
        }
    }

    public Session getReadOnlySession() throws RepositoryException {
        try {
            RepositoryStub repStub = RepositoryStub.getInstance();
            return repStub.getRepository().login(repStub.getReadOnlyCredentials(), null);
        } catch (RepositoryStubException e) {
            throw new RepositoryException("Failed to login to Repository.", e);
        }
    }

    public String getProperty(String name) throws RepositoryException {
        try {
            return RepositoryStub.getInstance().getProperty(name);
        } catch (RepositoryStubException e) {
            throw new RepositoryException("Failed to obtain Repository instance.", e);
        }
    }
}
