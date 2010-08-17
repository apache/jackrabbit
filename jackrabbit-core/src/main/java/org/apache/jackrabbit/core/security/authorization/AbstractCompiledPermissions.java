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

import javax.jcr.RepositoryException;
import java.util.Map;

/**
 * <code>AbstractCompiledPermissions</code>...
 */
public abstract class AbstractCompiledPermissions implements CompiledPermissions {

    // cache mapping a Path to a 'Result' containing permissions and privileges.
    private final Map<Path, Result> cache;
    private final Object monitor = new Object();

    @SuppressWarnings("unchecked")
    protected AbstractCompiledPermissions() {
        cache = new LRUMap(1000);
    }

    /**
     *
     * @param absPath Absolute path to return the result for.
     * @return the <code>Result</code> for the give <code>absPath</code>.
     * @throws RepositoryException if an error occurs.
     */
    public Result getResult(Path absPath) throws RepositoryException {
        Result result;
        synchronized (monitor) {
            result = cache.get(absPath);
            if (result == null) {
                result = buildResult(absPath);
                cache.put(absPath, result);
            }
        }
        return result;
    }

    /**
     *
     * @param absPath Absolute path to build the result for.
     * @return Result for the specified <code>absPath</code>.
     * @throws RepositoryException If an error occurs.
     */
    protected abstract Result buildResult(Path absPath) throws RepositoryException;

    /**
     * Removes all entries from the cache.
     */
    protected void clearCache() {
        synchronized (monitor) {
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
    /**
     *
     */
    public static class Result {

        public static final Result EMPTY = new Result(Permission.NONE, Permission.NONE, PrivilegeRegistry.NO_PRIVILEGE, PrivilegeRegistry.NO_PRIVILEGE);

        private final int allows;
        private final int denies;
        private final int allowPrivileges;
        private final int denyPrivileges;

        private int hashCode = -1;

        public Result(int allows, int denies, int allowPrivileges, int denyPrivileges) {
            this.allows = allows;
            this.denies = denies;
            this.allowPrivileges = allowPrivileges;
            this.denyPrivileges = denyPrivileges;
        }

        public boolean grants(int permissions) {
            return (this.allows | ~permissions) == -1;
        }

        public int getPrivileges() {
            return allowPrivileges;
        }

        public Result combine(Result other) {
            int cAllows =  allows | Permission.diff(other.allows, denies);
            int cDenies = denies | Permission.diff(other.denies, allows);
            int cAPrivs = allowPrivileges | Permission.diff(other.allowPrivileges, denyPrivileges);
            int cDPrivs = denyPrivileges | Permission.diff(other.denyPrivileges, allowPrivileges);
            return new Result(cAllows, cDenies, cAPrivs, cDPrivs);
        }

        /**
         * @see Object#hashCode()
         */
        @Override
        public int hashCode() {
            if (hashCode == -1) {
                int h = 17;
                h = 37 * h + allows;
                h = 37 * h + denies;
                h = 37 * h + allowPrivileges;
                h = 37 * h + denyPrivileges;
                hashCode = h;
            }
            return hashCode;
        }

        /**
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (object instanceof Result) {
                Result other = (Result) object;
                return allows == other.allows &&
                       denies == other.denies &&
                       allowPrivileges == other.allowPrivileges &&
                       denyPrivileges == other.denyPrivileges;
            }
            return false;
        }
    }
}