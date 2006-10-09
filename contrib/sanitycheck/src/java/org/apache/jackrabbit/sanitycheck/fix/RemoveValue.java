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
package org.apache.jackrabbit.sanitycheck.fix;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.sanitycheck.SanityCheckException;
import org.apache.jackrabbit.sanitycheck.inconsistency.ValueInconsistency;

/**
 * Fixes a <code>ValueInconsistency</code> only in 
 * multivalued properties by removing the value.
 * It can't handle single value properties. If the property is
 * single valued it delegates the call to the next command
 * in the chain.
 */
public class RemoveValue implements Command
{
    
    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        FixContext fCtx = (FixContext) ctx ;
        ValueInconsistency inc = (ValueInconsistency) fCtx.getInconsistency() ;
        PropertyState prop = inc.getProperty() ;

        // Check it's multivalues
        if (prop.getValues().length==1) {
            return false ;
        }
        
        // Remove the value from the collection
        InternalValue[] values =  prop.getValues() ;
        Collection newValues = new ArrayList() ;
        for (int i = 0; i < values.length; i++)
        {
            InternalValue value = values[i];
            if (i!=inc.getIndex()) {
                newValues.add(values[i]);
            }
        }
        
        // Modify and store the property
        prop.setValues((InternalValue[]) newValues.toArray(new InternalValue[values.length-1]));
        ChangeLog changeLog = new ChangeLog();
        changeLog.modified(prop);
        try
        {
            inc.getPersistenceManager().store(changeLog);
        } catch (ItemStateException e)
        {
            throw new SanityCheckException(
                    "Unable to store repaired property state", e);
        }
        
        return true;
    }
    
}
