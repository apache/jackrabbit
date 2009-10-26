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
package org.apache.jackrabbit.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.core.xml.ProtectedNodeImporter;
import org.apache.jackrabbit.core.xml.ProtectedPropertyImporter;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * <code>XmlImportConfig</code>...
 */
public class ImportConfig {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(ImportConfig.class);

    private final List<BeanConfig> protectedNodeImporters;
    private final List<BeanConfig> protectedPropertyImporters;

    public ImportConfig() {
        protectedNodeImporters = Collections.emptyList();
        protectedPropertyImporters = Collections.emptyList();
    }

    public ImportConfig(List<BeanConfig> protectedNodeImporters, List<BeanConfig> protectedPropertyImporters) {
        this.protectedNodeImporters = protectedNodeImporters;
        this.protectedPropertyImporters = protectedPropertyImporters;
    }

    public List<ProtectedNodeImporter> getProtectedNodeImporters() {
        List<ProtectedNodeImporter> pnis =
            new ArrayList<ProtectedNodeImporter>();
        for (BeanConfig bc : protectedNodeImporters) {
            try {
                pnis.add(bc.newInstance(ProtectedNodeImporter.class));
            } catch (ConfigurationException e) {
                log.warn(e.getMessage());
            }
        }
        return pnis;
    }

    public List<ProtectedPropertyImporter> getProtectedPropertyImporters() {
        List<ProtectedPropertyImporter> ppis =
            new ArrayList<ProtectedPropertyImporter>();
        for (BeanConfig bc : protectedPropertyImporters) {
            try {
                ppis.add(bc.newInstance(ProtectedPropertyImporter.class));
            } catch (ConfigurationException e) {
                log.warn(e.getMessage());
            }
        }
        return ppis;
    }
}