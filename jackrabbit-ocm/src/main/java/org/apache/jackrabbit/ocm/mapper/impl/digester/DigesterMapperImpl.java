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
package org.apache.jackrabbit.ocm.mapper.impl.digester;



import java.io.InputStream;

import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.impl.AbstractMapperImpl;

/**
 *
 * Digester implementation for {@link org.apache.jackrabbit.ocm.mapper.Mapper}
 * 
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class DigesterMapperImpl extends AbstractMapperImpl implements Mapper {
    

    /**
     * No-arg constructor.
     */
    public DigesterMapperImpl() {
    }

    /**
     * Constructor
     *
     * @param xmlFile The xml mapping file to read
     *
     */
    public DigesterMapperImpl(String xmlFile) {
        descriptorReader = new DigesterDescriptorReader(xmlFile);
        this.buildMapper();
    }

    /**
     * Constructor
     *
     * @param files a set of xml mapping files to read
     *
     */
    public DigesterMapperImpl(String[] files) {
        descriptorReader = new DigesterDescriptorReader(files);
        this.buildMapper();
    }

    /**
     * Constructor
     *
     * @param stream The xml mapping file to read
     */
    public DigesterMapperImpl(InputStream stream) {
        descriptorReader = new DigesterDescriptorReader(stream);
        this.buildMapper();
    }

    /**
     * Constructor
     *
     * @param streams a set of mapping files to read
     *
     */
    public DigesterMapperImpl(InputStream[] streams) {
        descriptorReader = new DigesterDescriptorReader(streams);
        this.buildMapper();
    }

}
