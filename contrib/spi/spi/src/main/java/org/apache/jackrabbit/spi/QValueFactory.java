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
package org.apache.jackrabbit.spi;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.util.Calendar;

/**
 * <code>QValueFactory</code>...
 */
public interface QValueFactory {

    /**
     * @param value
     * @return
     */
    public QValue create(String value, int type);

    /**
     * @param value
     * @return
     */
    public QValue create(Calendar value);

    /**
     * @param value
     * @return
     */
    public QValue create(QName value);

    /**
     * @param value
     * @return
     */
    public QValue create(Path value);


    /**
     * @param value
     * @return
     */
    public QValue create(byte[] value);

    /**
     * @param value
     * @return
     * @throws IOException
     */
    public QValue create(InputStream value) throws IOException;

    /**
     * @param value
     * @return
     * @throws IOException
     */
    public QValue create(File value) throws IOException;
}