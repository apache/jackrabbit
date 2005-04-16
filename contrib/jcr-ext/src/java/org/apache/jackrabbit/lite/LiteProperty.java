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
package org.apache.jackrabbit.lite;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.PropertyDef;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.base.BaseProperty;

/**
 * TODO
 */
public class LiteProperty extends BaseProperty {

    private Value value;

    private Value[] values;

    private final PropertyDef definition;

    protected LiteProperty(Item item, Value value, PropertyDef definition) {
        super(item);
        this.value = value;
        this.values = null;
        this.definition = definition;
    }

    protected LiteProperty(Item item, Value[] values, PropertyDef definition) {
        super(item);
        this.value = null;
        this.values = values;
        this.definition = definition;
    }

    public Value getValue() throws ValueFormatException, RepositoryException {
        if (value != null) {
            return value;
        } else {
            throw new ValueFormatException("This property is multi-valued");
        }
    }

    public Value[] getValues() throws ValueFormatException, RepositoryException {
        if (values != null) {
            return values;
        } else {
            throw new ValueFormatException("This property is not multi-valued");
        }
    }

    public void setValue(Value value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        if (value != null) {
            this.value = value;
        } else {
            throw new ValueFormatException("This property is multi-valued");
        }
    }

    public void setValue(Value[] values) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        if (values != null) {
            this.values = values;
        } else {
            throw new ValueFormatException("This property is not multi-valued");
        }
    }

}
