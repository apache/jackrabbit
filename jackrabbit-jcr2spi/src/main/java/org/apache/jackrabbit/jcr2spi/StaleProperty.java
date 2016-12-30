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

package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.commons.AbstractProperty;

import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;
import java.math.BigDecimal;

/**
 * This implementation of {@link Property} throws an {@link InvalidItemStateException} on
 * all method calls.
 */
public class StaleProperty extends AbstractProperty {
    public void setValue(Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw createIISE();
    }

    public void setValue(BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw createIISE();
    }

    public Value getValue() throws ValueFormatException, RepositoryException {
        throw createIISE();
    }

    public Value[] getValues() throws ValueFormatException, RepositoryException {
        throw createIISE();
    }

    public Binary getBinary() throws ValueFormatException, RepositoryException {
        throw createIISE();
    }

    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        throw createIISE();
    }

    public PropertyDefinition getDefinition() throws RepositoryException {
        throw createIISE();
    }

    public boolean isMultiple() throws RepositoryException {
        throw createIISE();
    }

    public String getName() throws RepositoryException {
        throw createIISE();
    }

    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        throw createIISE();
    }

    public Session getSession() throws RepositoryException {
        throw createIISE();
    }

    public boolean isNew() {
        return false;
    }

    public boolean isModified() {
        return false;
    }

    public boolean isSame(Item otherItem) throws RepositoryException {
        throw createIISE();
    }

    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        throw createIISE();
    }

    public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException {
        throw createIISE();
    }

    private InvalidItemStateException createIISE() {
        return new InvalidItemStateException("property does not exist anymore");
    }
}
