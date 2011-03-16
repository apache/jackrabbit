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
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
     * 
     * @return
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

    /**
     * Adds the given <code>privileges</code> to the specified
     * <code>target</code> set if they are not present in the specified
     * <code>complement</code> set.
     * 
     * @param privileges
     * @param target
     * @param complement
     */
    protected static void updatePrivileges(Collection<Privilege> privileges, Set<Privilege> target, Set<Privilege> complement) {
        for (Privilege p : privileges) {
            if (!complement.contains(p)) {
                target.add(p);
            }
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
     * @see CompiledPermissions#hasPrivileges(Path, Privilege[])
     */
    public boolean hasPrivileges(Path absPath, Privilege[] privileges) throws RepositoryException {
        Result result = getResult(absPath);
        int builtin = getPrivilegeManagerImpl().getBits(privileges);

        if ((result.allowPrivileges | ~builtin) == -1) {
            // in addition check all custom privileges
            for (Privilege p : privileges) {
                if (getPrivilegeManagerImpl().isCustomPrivilege(p)) {
                    if (!result.customAllow.contains(p)) {
                        if (p.isAggregate()) {
                            // test if aggregated privs were granted individually.
                            for (Privilege aggr : p.getAggregatePrivileges()) {
                                if (!aggr.isAggregate() && !result.customAllow.contains(aggr)) {
                                    // an aggregated custom priv is not allowed -> return false
                                    return false;
                                }
                            }
                        } else {
                            // simple custom allow not allowed -> return false
                            return false;
                        }
                    } // else: custom privilege allowed -> continue.
                } // else: not a custom priv -> already covered.
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * @see CompiledPermissions#getPrivilegeSet(Path)
     */
    public Set<Privilege> getPrivilegeSet(Path absPath) throws RepositoryException {
        Result result = getResult(absPath);
        Set<Privilege> privileges = new HashSet<Privilege>();
        privileges.addAll(getPrivilegeManagerImpl().getPrivileges(result.getPrivileges()));
        privileges.addAll(result.customAllow);
        return privileges;
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

        public static final Result EMPTY = new Result(Permission.NONE, Permission.NONE, PrivilegeRegistry.NO_PRIVILEGE, PrivilegeRegistry.NO_PRIVILEGE);

        private final int allows;
        private final int denies;
        private final int allowPrivileges;
        private final int denyPrivileges;

        private final Set<Privilege> customAllow;
        private final Set<Privilege> customDeny;

        private int hashCode = -1;

        public Result(int allows, int denies, int allowPrivileges, int denyPrivileges) {
            this(allows, denies, allowPrivileges, denyPrivileges, Collections.<Privilege>emptySet(), Collections.<Privilege>emptySet());
        }

        public Result(int allows, int denies, int allowPrivileges, int denyPrivileges,
                      Set<Privilege> customAllow, Set<Privilege> customDeny) {
            this.allows = allows;
            this.denies = denies;
            this.allowPrivileges = allowPrivileges;
            this.denyPrivileges = denyPrivileges;

            this.customAllow = customAllow;
            this.customDeny = customDeny;
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

            Set<Privilege> combinedAllow = new HashSet<Privilege>();
            combinedAllow.addAll(customAllow);
            updatePrivileges(other.customAllow, combinedAllow, customDeny);

            Set<Privilege> combinedDeny = new HashSet<Privilege>();
            combinedDeny.addAll(customDeny);
            updatePrivileges(other.customDeny, combinedDeny, customAllow);
            return new Result(cAllows, cDenies, cAPrivs, cDPrivs, customAllow, customDeny);
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
                h = 37 * h + customAllow.hashCode();
                h = 37 * h + customDeny.hashCode();
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
                       denyPrivileges == other.denyPrivileges &&
                       customAllow.equals(other.customAllow) &&
                       customDeny.equals(other.customDeny);
            }
            return false;
        }
    }
}