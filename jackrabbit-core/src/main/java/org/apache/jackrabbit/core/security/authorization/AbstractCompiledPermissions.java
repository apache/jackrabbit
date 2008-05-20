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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.commons.collections.map.LRUMap;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * <code>AbstractCompiledPermissions</code>...
 */
public abstract class AbstractCompiledPermissions implements CompiledPermissions {

    private static Logger log = LoggerFactory.getLogger(AbstractCompiledPermissions.class);

    // cache mapping a Path to a 'Result' containing permissions and privileges.
    private final LRUMap cache;

    protected AbstractCompiledPermissions() {
        cache = new LRUMap(1000);
    }

    /**
     *
     * @param absPath
     * @return
     */
    protected Result getResult(Path absPath) throws RepositoryException {
        Result result;
        synchronized (cache) {
            result = (Result) cache.get(absPath);
            if (result == null) {
                result = buildResult(absPath);
                cache.put(absPath, result);
            }
        }
        return result;
    }

    /**
     *
     * @param absPath
     * @return
     * @throws RepositoryException
     */
    protected abstract Result buildResult(Path absPath) throws RepositoryException;

    /**
     *
     */
    protected void clearCache() {
        synchronized (cache) {
            cache.clear();
        }
    }

    //------------------------------------------------< CompiledPermissions >---
    /**
     * @see CompiledPermissions#close()
     */
    public void close() {
        clearCache();
    }

    /**
     * @see CompiledPermissions#grants(Path, int)
     */
    public boolean grants(Path absPath, int permissions) throws RepositoryException {
        return getResult(absPath).grants(permissions);
    }

    /**
     * @see CompiledPermissions#getPrivileges(Path)
     */
    public int getPrivileges(Path absPath) throws RepositoryException {
        return getResult(absPath).getPrivileges();
    }

    /**
     * @see CompiledPermissions#canReadAll()
     */
    public boolean canReadAll() throws RepositoryException {
        return false;
    }

    //--------------------------------------------------------< inner class >---

    protected class Result {

        private final int permissions;
        private final int privileges;

        public Result(int permissions, int privileges) {
            this.permissions = permissions;
            this.privileges = privileges;
        }

        public boolean grants(int permissions) {
            return (this.permissions | ~permissions) == -1;
        }

        public int getPrivileges() {
            return privileges;
        }
    }
}