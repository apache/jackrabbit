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
package org.apache.jackrabbit.value;

import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.PathFormat;
import org.apache.jackrabbit.name.NameFormat;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>ValueFormat</code>...
 */
public class ValueFormat {

    public static QValue getQValue(Value jcrValue, NamespaceResolver nsResolver) throws RepositoryException {
        if (jcrValue == null) {
            throw new IllegalArgumentException("null value");
        }
        if (jcrValue.getType() == PropertyType.BINARY) {
            try {
                return QValue.create(jcrValue.getStream());
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        } else {
            return getQValue(jcrValue.getString(), jcrValue.getType(), nsResolver);
        }
    }

    public static QValue[] getQValues(Value[] jcrValues, NamespaceResolver nsResolver) throws RepositoryException {
        if (jcrValues == null) {
            throw new IllegalArgumentException("null value");
        }
        List qValues = new ArrayList();
        for (int i = 0; i < jcrValues.length; i++) {
            if (jcrValues[i] != null) {
                qValues.add(getQValue(jcrValues[i], nsResolver));
            }
        }
        return (QValue[]) qValues.toArray(new QValue[qValues.size()]);
    }

    public static QValue getQValue(String jcrValue, int propertyType, NamespaceResolver nsResolver) throws RepositoryException {
        QValue qValue;
        switch (propertyType) {
            case PropertyType.STRING:
            case PropertyType.BOOLEAN:
            case PropertyType.DOUBLE:
            case PropertyType.LONG:
            case PropertyType.DATE:
            case PropertyType.REFERENCE:
                qValue = QValue.create(jcrValue, propertyType);
                break;
            case PropertyType.BINARY:
                qValue = QValue.create(jcrValue.getBytes());
                break;
            case PropertyType.NAME:
                try {
                    QName qName = NameFormat.parse(jcrValue, nsResolver);
                    qValue = QValue.create(qName);
                } catch (NameException e) {
                    throw new RepositoryException(e);
                }
                break;
            case PropertyType.PATH:
                try {
                    Path qPath = PathFormat.parse(jcrValue, nsResolver).getNormalizedPath();
                    qValue = QValue.create(qPath);
                } catch (NameException e) {
                    throw new RepositoryException(e);
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid property type.");
        }
        return qValue;
    }

    /**
     * @param nsResolver
     * @return
     * @throws RepositoryException
     */
    public static Value getJCRValue(QValue qualifiedValue,
                                    NamespaceResolver nsResolver,
                                    ValueFactory factory)
        throws RepositoryException {
        Value jcrValue;
        int propertyType = qualifiedValue.getType();
        switch (propertyType) {
            case PropertyType.STRING:
            case PropertyType.BOOLEAN:
            case PropertyType.DATE:
            case PropertyType.DOUBLE:
            case PropertyType.LONG:
            case PropertyType.REFERENCE:
                jcrValue = factory.createValue(qualifiedValue.getString(), propertyType);
                break;
            case PropertyType.PATH:
                try {
                    Path qPath = Path.valueOf(qualifiedValue.getString());
                    jcrValue = factory.createValue(PathFormat.format(qPath, nsResolver), propertyType);
                } catch (NoPrefixDeclaredException npde) {
                    // should never get here...
                    throw new RepositoryException("internal error: encountered unregistered namespace", npde);
                }
                break;
            case PropertyType.NAME:
                try {
                    QName qName = QName.valueOf(qualifiedValue.getString());
                    jcrValue = factory.createValue(NameFormat.format(qName, nsResolver), propertyType);
                } catch (NoPrefixDeclaredException npde) {
                    // should never get here...
                    throw new RepositoryException("internal error: encountered unregistered namespace", npde);
                }
                break;
            case PropertyType.BINARY:
                jcrValue = factory.createValue(qualifiedValue.getStream());
                break;
            default:
                throw new RepositoryException("illegal internal value type");
        }
        return jcrValue;
    }
}