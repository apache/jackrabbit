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
package org.apache.jackrabbit.core.virtual;

import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;

/**
 * A <code>VirtualValueProvider</code> is used for virtual properties that
 * want to provide values dynamically.
 */
public interface VirtualValueProvider {

    /**
     * Returns the values for the given name
     * @param propName the name of the property
     * @return the values
     */
    InternalValue[] getVirtualValues(Name propName);

}
