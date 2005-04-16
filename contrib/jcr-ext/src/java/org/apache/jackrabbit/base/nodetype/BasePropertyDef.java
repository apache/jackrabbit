/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.base.nodetype;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDef;

/**
 * TODO
 */
public class BasePropertyDef extends BaseItemDef implements PropertyDef {

    /** {@inheritDoc} */
    public int getRequiredType() {
        return PropertyType.UNDEFINED;
    }

    /** {@inheritDoc} */
    public String[] getValueConstraints() {
        return null;
    }

    /** {@inheritDoc} */
    public Value[] getDefaultValues() {
        return null;
    }

    /** {@inheritDoc} */
    public boolean isMultiple() {
        return false;
    }

}
