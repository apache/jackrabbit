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

import org.apache.commons.collections4.map.LRUMap;
import org.apache.jackrabbit.spi.Path;

import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;
import java.util.Map;
import java.util.Set;

/**
 * <code>AbstractCompiledPermissions</code>...
 */
public abstract class AbstractCompiledPermissions implements CompiledPermissions {

    // cache mapping a Path to a 'Result' containing permissions and privileges.
    private final Map<Path, Result> cache;
    private final Object monitor = new Object();

    protected AbstractCompiledPermissions() {
        cache = new LRUMap<>(1000);
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
                if (absPath == null) {
                    result = buildRepositoryResult();
                } else {
                    result = buildResult(absPath);
                }
                cache.put(absPath, result);
            }
        }
        return result;
    }

    /**
     * Retrieve the result for the specified path.
     * 
     * @param absPath Absolute path to build the result for.
     * @return Result for the specified <code>absPath</code>.
     * @throws RepositoryException If an error occurs.
     */
    protected abstract Result buildResult(Path absPath) throws RepositoryException;

    /**
     * Retrieve the result for repository level operations.
     *
     * @return The result instance for those permissions and privileges granted
     * for repository level operations.
     * @throws RepositoryException
     */
    protected abstract Result buildRepositoryResult() throws RepositoryException;

    /**
     * Retrieve the privilege manager.
     * 
     * @return An instance of privilege manager.
     * @throws javax.jcr.RepositoryException If an error occurs.
     */
    protected abstract PrivilegeManagerImpl getPrivilegeManagerImpl() throws RepositoryException;

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
        Set<Privilege> pvs = getPrivilegeSet(absPath);
        return PrivilegeRegistry.getBits(pvs.toArray(new Privilege[pvs.size()]));
    }

    /**
     * @see CompiledPermissions#hasPrivileges(org.apache.jackrabbit.spi.Path, javax.jcr.security.Privilege[])
     */
    public boolean hasPrivileges(Path absPath, Privilege... privileges) throws RepositoryException {
        Result result = getResult(absPath);

        PrivilegeBits bits = getPrivilegeManagerImpl().getBits(privileges);
        return result.allowPrivileges.includes(bits);
    }

    /**
     * @see CompiledPermissions#getPrivilegeSet(Path)
     */
    public Set<Privilege> getPrivilegeSet(Path absPath) throws RepositoryException {
        Result result = getResult(absPath);
        return getPrivilegeManagerImpl().getPrivileges(result.allowPrivileges);
    }

    /**
     * @see CompiledPermissions#canReadAll()
     */
    public boolean canReadAll() throws RepositoryException {
        return false;
    }

    //--------------------------------------------------------< inner class >---
    /**
     * Result of permission (and optionally privilege) evaluation for a given path.
     */
    public static class Result {

        public static final Result EMPTY = new Result(Permission.NONE, Permission.NONE, PrivilegeBits.EMPTY, PrivilegeBits.EMPTY);
        private final int allows;
        private final int denies;
        private final PrivilegeBits allowPrivileges;
        private final PrivilegeBits denyPrivileges;

        private int hashCode = -1;

        /**
         * @deprecated
         */
        public Result(int allows, int denies, int allowPrivileges, int denyPrivileges) {
            this(allows, denies, PrivilegeBits.getInstance(allowPrivileges), PrivilegeBits.getInstance(denyPrivileges));
        }

        public Result(int allows, int denies, PrivilegeBits allowPrivileges, PrivilegeBits denyPrivileges) {
            this.allows = allows;
            this.denies = denies;
            // make sure privilegebits are unmodifiable -> proper hashcode generation
            this.allowPrivileges = allowPrivileges.unmodifiable();
            this.denyPrivileges = denyPrivileges.unmodifiable();
        }

        public boolean grants(int permissions) {
            return (this.allows | ~permissions) == -1;
        }

        /**
         * @deprecated jackrabbit 2.3 (throws UnsupportedOperationException, use getPrivilegeBits instead)
         */
        public int getPrivileges() {
            throw new UnsupportedOperationException("use #getPrivilegeBits instead.");
        }

        public PrivilegeBits getPrivilegeBits() {
            return allowPrivileges;
        }

        public Result combine(Result other) {
            int cAllows =  allows | Permission.diff(other.allows, denies);
            int cDenies = denies | Permission.diff(other.denies, allows);

            PrivilegeBits cAPrivs = PrivilegeBits.getInstance(allowPrivileges);
            cAPrivs.addDifference(other.allowPrivileges, denyPrivileges);
            PrivilegeBits cdPrivs = PrivilegeBits.getInstance(denyPrivileges);
            cdPrivs.addDifference(other.denyPrivileges, allowPrivileges);

            return new Result(cAllows, cDenies, allowPrivileges, denyPrivileges);
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
                h = 37 * h + allowPrivileges.hashCode();
                h = 37 * h + denyPrivileges.hashCode();
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
                       allowPrivileges.equals(other.allowPrivileges) &&
                       denyPrivileges.equals(other.denyPrivileges);
            }
            return false;
        }
    }
}