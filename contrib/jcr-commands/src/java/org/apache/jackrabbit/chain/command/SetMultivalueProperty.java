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
package org.apache.jackrabbit.chain.command;

import javax.jcr.Node;
import javax.jcr.PropertyType;

import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;

/**
 * Set a multivalue property to the current working node. <br>
 * The default regular expression is ",". <br>
 * The Command attributes are set from the specified literal values, or from the
 * context attributes stored under the given keys.
 */
public class SetMultivalueProperty extends AbstractSetProperty
{

    // ---------------------------- < literals >

    /** regular expression */
    private String regExp;

    // ---------------------------- < keys >

    /** regular expression key */
    private String regExpKey;

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        String regExp = CtxHelper.getAttr(this.regExp, this.regExpKey,
            ",", ctx);

        String value = CtxHelper.getAttr(this.value, this.valueKey,
            ctx);

        String name = CtxHelper.getAttr(this.propertyName, this.propertyNameKey, ctx);

        String propertyType = CtxHelper.getAttr(this.propertyType,
            this.propertyTypeKey, PropertyType.TYPENAME_STRING, ctx);

        String[] values = value.split(regExp);
        Node node = CtxHelper.getCurrentNode(ctx);
        node
            .setProperty(name, values, PropertyType.valueFromName(propertyType));

        return false;
    }

    /**
     * @return Returns the regExp.
     */
    public String getRegExp()
    {
        return regExp;
    }

    /**
     * @param regExp
     *            The regExp to set.
     */
    public void setRegExp(String regExp)
    {
        this.regExp = regExp;
    }

    /**
     * @return Returns the regExpKey.
     */
    public String getRegExpKey()
    {
        return regExpKey;
    }

    /**
     * @param regExpKey
     *            Set the context attribute key for the regExp attribute.
     */
    public void setRegExpKey(String regExpKey)
    {
        this.regExpKey = regExpKey;
    }
}
