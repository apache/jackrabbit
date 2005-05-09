/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * The <code>StatefullValueAdapter</code> class 
 * 
 * @version $Revision$, $Date$
 * @author fmeschbe
 * @since 
 */
final class StatefullValueAdapter implements StatefullValue {

    private final Value delegatee;
    
    StatefullValueAdapter(Value delegatee) {
        this.delegatee = delegatee;
    }

    /** {@inheritDoc} */
    public InputStream getStream() throws ValueFormatException, RepositoryException {
        return delegatee.getStream();
    }

    /** {@inheritDoc} */
    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        return delegatee.getBoolean();
    }

    /** {@inheritDoc} */
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        return delegatee.getDate();
    }

    /** {@inheritDoc} */
    public double getDouble() throws ValueFormatException, RepositoryException {
        return delegatee.getDouble();
    }

    /** {@inheritDoc} */
    public long getLong() throws ValueFormatException, RepositoryException {
        return delegatee.getLong();
    }

    /** {@inheritDoc} */
    public String getString() throws ValueFormatException, RepositoryException {
        return delegatee.getString();
    }

    /** {@inheritDoc} */
    public int getType() {
        return delegatee.getType();
    }
}
