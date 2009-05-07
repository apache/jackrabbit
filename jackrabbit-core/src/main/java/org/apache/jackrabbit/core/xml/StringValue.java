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
package org.apache.jackrabbit.core.xml;

import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.util.Base64;
import org.apache.jackrabbit.value.ValueHelper;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.ValueFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * <code>StringValue</code> represents an immutable serialized value.
 */
class StringValue implements TextValue {

    private final String value;

    private final NamePathResolver nsContext;
    private final ValueFactory valueFactory;

    /**
     * Constructs a new <code>StringValue</code> representing the given
     * value.
     *
     * @param value
     * @param nsContext
     * @param valueFactory
     */
    protected StringValue(String value, NamePathResolver nsContext, ValueFactory valueFactory) {
        this.value = value;
        this.nsContext = nsContext;
        this.valueFactory = valueFactory;
    }

    //--------------------------------------------------------< TextValue >

    public Value getValue(int type, NamePathResolver resolver)
            throws ValueFormatException, RepositoryException {
        if (type == PropertyType.NAME || type == PropertyType.PATH) {
            // NAME and PATH require special treatment because
            // they depend on the current namespace context
            // of the xml document

            // convert serialized value to InternalValue using
            // current namespace context of xml document
            InternalValue ival =
                    InternalValue.create(ValueHelper.convert(
                            value, type, valueFactory), nsContext);
            // convert InternalValue to Value using this
            // session's namespace mappings
            return ValueFormat.getJCRValue(ival, resolver, valueFactory);
        } else {
            // all other types
            return ValueHelper.deserialize(value, type, false, valueFactory);
        }
    }

    public InternalValue getInternalValue(int targetType)
            throws ValueFormatException, RepositoryException {
        try {
            if (targetType == PropertyType.BINARY) {
                // base64 encoded BINARY type;
                // decode using Reader
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Base64.decode(value, baos);
                return InternalValue.create(baos.toByteArray());
            } else {
                // convert serialized value to InternalValue using
                // current namespace context of xml document
                return InternalValue.create(ValueHelper.convert(
                        value, targetType, valueFactory), nsContext);
            }
        } catch (IOException e) {
            throw new RepositoryException("Error decoding Base64 content", e);
        }
    }

    public void dispose() {
        // do nothing
    }

}
