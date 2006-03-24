/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jmeter.protocol.java.sampler;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.chain.Context;
import org.apache.jmeter.threads.JMeterContext;

/**
 * <code>JMeterContext</code> to commons chain <code>Context</code> adapter
 */
class JMeterContextAdapter implements Context
{
    private JMeterContext ctx;

    /**
     * private constructor
     */
    private JMeterContextAdapter()
    {
        super();
    }

    /**
     * private constructor
     */
    public JMeterContextAdapter(JMeterContext ctx)
    {
        super();
        this.ctx = ctx;
    }

    public void clear()
    {
        throw new UnsupportedOperationException();
    }

    public boolean containsKey(Object key)
    {
        return get(key) != null;
    }

    public boolean containsValue(Object value)
    {
        throw new UnsupportedOperationException();
    }

    public Set entrySet()
    {
        throw new UnsupportedOperationException();
    }

    public Object get(Object key)
    {
        if (key == null || !(key instanceof String))
        {
            throw new IllegalArgumentException("Only String keys are supported");
        }
        return ctx.getVariables().getObject((String) key);
    }

    public boolean isEmpty()
    {
        throw new UnsupportedOperationException();
    }

    public Set keySet()
    {
        throw new UnsupportedOperationException();
    }

    public Object put(Object key, Object value)
    {
        if (key == null || !(key instanceof String))
        {
            throw new IllegalArgumentException("Only String keys are supported");
        }
        Object stale = ctx.getVariables().getObject((String) key);
        ctx.getVariables().putObject((String) key, value);
        return stale;
    }

    public void putAll(Map t)
    {
        ctx.getVariables().putAll(t);
    }

    public Object remove(Object key)
    {
        if (key == null || !(key instanceof String))
        {
            throw new IllegalArgumentException("Only String keys are supported");
        }
        Object stale = ctx.getVariables().getObject((String) key);
        ctx.getVariables().remove((String) key);
        return stale;
    }

    public int size()
    {
        throw new UnsupportedOperationException();
    }

    public Collection values()
    {
        throw new UnsupportedOperationException();
    }
}
