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
package org.apache.jackrabbit.core.integration.random.operation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

/**
 * <code>SetProperty</code> sets a string property with a random text of length
 * 5.
 */
public class SetProperty extends Operation {

    private static final Logger log = LoggerFactory.getLogger(SetProperty.class);

    private final String name;

    public SetProperty(Session s, String path, String name) {
        super(s, path);
        this.name = name;
    }

    public NodeIterator execute() throws Exception {
        Node n = getNode();
        String value = getRandomText(5);
        String path = n.setProperty(name, value).getPath();
        log.info(path + ": " + value);
        return wrapWithIterator(getNode());
    }
}
