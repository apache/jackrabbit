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

package org.apache.portals.graffito.jcr.exception;


/**
 * Occurs when the jcr mapping converters try to assign or read an incorrect atomic field type.
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class IncorrectAtomicTypeException extends JcrMappingException {

    /** Use serialVersionUID for interoperability. */
    private final static long serialVersionUID = 8819724602193665601L;

    public IncorrectAtomicTypeException(String message, Throwable nested) {
        super(message, nested);
    }

    public IncorrectAtomicTypeException(String message) {
        super(message);
    }

    public IncorrectAtomicTypeException(Throwable nested) {
        super(nested);
    }

}
