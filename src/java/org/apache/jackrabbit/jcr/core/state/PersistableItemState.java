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

/**
 * <code>PersistableItemState</code> ...
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.4 $, $Date: 2004/08/02 16:19:48 $
 */
public interface PersistableItemState {

    /**
     * @throws ItemStateException
     */
    public void reload() throws ItemStateException;

    /**
     * @throws ItemStateException
     */
    public void store() throws ItemStateException;

    /**
     * @throws ItemStateException
     */
    public void destroy() throws ItemStateException;
}
