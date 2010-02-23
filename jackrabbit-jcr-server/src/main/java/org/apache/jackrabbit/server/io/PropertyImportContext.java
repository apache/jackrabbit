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
package org.apache.jackrabbit.server.io;

import org.apache.jackrabbit.webdav.property.PropEntry;

import javax.jcr.Item;
import java.util.List;

/**
 * <code>PropertyImportContext</code>...
 */
public interface PropertyImportContext extends IOContext {

    /**
     * Returns the import root for the properties to be altered. Note, that
     * a particular implementation may still apply the modifications to
     * child items at any depth as long as property import is consistent with
     * the corresponding export.
     *
     * @return the import root of the resource to import.
     */
    public Item getImportRoot();

    /**
     * Returns a list of properties to be modified by a call to
     * {@link PropertyHandler#importProperties(PropertyImportContext, boolean)}.
     *
     * @return list of properties to be modified
     */
    public List<? extends PropEntry> getChangeList();
}
