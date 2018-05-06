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
package org.apache.jackrabbit.rmi.value;

import javax.jcr.PropertyType;

/**
 * Boolean value.
 */
class BooleanValue extends AbstractValue {

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 5266937874230536517L;

    /**
     * The boolean value
     */
    private final boolean value;

    /**
     * Creates an instance for the given boolean value.
     */
    public BooleanValue(boolean value) {
        this.value = value;
    }

    /**
     * Returns {@link PropertyType#BOOLEAN}.
     */
    public int getType() {
        return PropertyType.BOOLEAN;
    }

    /**
     * Returns the boolean value.
     */
    @Override
    public boolean getBoolean() {
        return value;
    }

    /**
     * The boolean is converted using {@link Boolean#toString()}.
     */
    public String getString() {
        return Boolean.toString(value);
    }

}
