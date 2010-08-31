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
package org.apache.jackrabbit.commons.predicate;

import javax.jcr.Item;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * The nt file item filter matches all properties that are defined my the
 * nt:file or nt:resource nodetype. the later only, if the respective nodes
 * name is 'jcr:content'.
 *
 * Additionally the properties 'jcr:encoding' can be configured to be excluded.
 *
 */
public class NtFilePredicate implements Predicate {

    public static final String NT_FILE = "nt:file";
    public static final String NT_HIERARCHYNODE = "nt:hierarchyNode";
    public static final String NT_RESOURCE = "nt:resource";
    public static final String JCR_CONTENT = "jcr:content";
    public static final String JCR_ENCODING = "jcr:encoding";
    public static final String JCR_MIMETYPE = "jcr:mimeType";
    public static final String JCR_PRIMARY_TYPE = "jcr:primaryType";

    /**
     * indicates if the jcr:encoding property is to be excluded from this filter.
     */
    protected final boolean ignoreEncoding;

    /**
     * indicates if the jcr:mimeType property is to be excluded from this filter.
     */
    protected final boolean ignoreMimeType;

    public NtFilePredicate() {
        this(false, false);
    }

    public NtFilePredicate(boolean ignoreEncoding, boolean ignoreMimeType) {
        this.ignoreEncoding = ignoreEncoding;
        this.ignoreMimeType = ignoreMimeType;
    }

    /**
     * Returns the <code>ignore encoding</code> flag.
     * @return the <code>ignore encoding</code> flag.
     */
    public boolean isIgnoreEncoding() {
        return ignoreEncoding;
    }

    /**
     * Returns the <code>ignore mime type</code> flag.
     * @return the <code>ignore mime type</code> flag.
     */
    public boolean isIgnoreMimeType() {
        return ignoreMimeType;
    }

    /**
     * @return <code>true</code> if the item is a nt:file or nt:resource property
     * @see org.apache.jackrabbit.commons.predicate.Predicate#evaluate(java.lang.Object)
     */
    public boolean evaluate(Object item) {
        if ( item instanceof Item ) {
            if (!((Item)item).isNode()) {
                try {
                    Property prop = (Property) item;
                    String dnt = prop.getDefinition().getDeclaringNodeType().getName();
                    // exclude all nt:file props
                    if (dnt.equals(NT_FILE) || dnt.equals(NT_HIERARCHYNODE)) {
                        return true;
                    }
                    if (ignoreEncoding && prop.getName().equals(JCR_ENCODING)) {
                        return false;
                    }
                    if (ignoreMimeType && prop.getName().equals(JCR_MIMETYPE)) {
                        return false;
                    }
                    // exclude nt:resource props, if parent is 'jcr:content'
                    if (prop.getParent().getName().equals(JCR_CONTENT)) {
                        if (dnt.equals(NT_RESOURCE)) {
                            return true;
                        }
                        // exclude primary type if nt:resource
                        /*
                        if (prop.getName().equals(JCR_PRIMARY_TYPE)
                                && prop.getValue().getString().equals(NT_RESOURCE)) {
                            return true;
                        }
                        */
                    }
                } catch (RepositoryException re) {
                    return false;
                }
            }
        }
        return false;
    }
}