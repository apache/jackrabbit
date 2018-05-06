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
package org.apache.jackrabbit.spi.commons.nodetype;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DummyNamespaceResolver;

import javax.jcr.nodetype.PropertyDefinitionTemplate;

import junit.framework.TestCase;

/**
 * <code>PropertyDefinitionTemplateImplTest</code>...
 */
public class PropertyDefinitionTemplateImplTest extends TestCase {

    private PropertyDefinitionTemplate tmpl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        NamePathResolver resolver = new DefaultNamePathResolver(new DummyNamespaceResolver());
        tmpl = new PropertyDefinitionTemplateImpl(resolver);
    }

    public void testInvalidPropertyType() {
        try {
            tmpl.setRequiredType(-1);
            fail("-1 isn't a valid property type.");
        } catch (IllegalArgumentException e) {
            // success
        }
        try {
            tmpl.setRequiredType(Integer.MAX_VALUE);
            fail(Integer.MAX_VALUE + " isn't a valid property type.");
        } catch (IllegalArgumentException e) {
            // success
        }
    }

    public void testInvalidOPVFlag() {
        try {
            tmpl.setOnParentVersion(-1);
            fail("-1 isn't a valid OPV flag.");
        } catch (IllegalArgumentException e) {
            // success
        }
        try {
            tmpl.setOnParentVersion(Integer.MAX_VALUE);
            fail(Integer.MAX_VALUE + " isn't a valid OPV flag.");
        } catch (IllegalArgumentException e) {
            // success
        }
    }

    // TODO: add tests for default values on empty template NOT specified by the specification
    
}