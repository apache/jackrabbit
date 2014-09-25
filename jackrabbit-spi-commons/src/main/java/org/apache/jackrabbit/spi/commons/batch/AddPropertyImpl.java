package org.apache.jackrabbit.spi.commons.batch;

import java.util.Arrays;

import org.apache.jackrabbit.spi.AddItem;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValue;

public class AddPropertyImpl implements AddItem {

    private final Name propName;
    private final int propertyType;
    private final boolean isMultiValued;
    private final QValue[] values;
    
    private AddPropertyImpl(Name propName, int propertyType, boolean isMultiValued, QValue[] values) {
        this.propName = propName;
        this.propertyType = propertyType;
        this.isMultiValued = isMultiValued;
        this.values = values;
    }
      
    public int getType() {
        return propertyType;
    }
    
    
    public QValue[] getValues() {
        return values;
    }
    
    public boolean isMultiValued() {
        return isMultiValued;
    }
    
    //------------------------------------< AddItem >---
    @Override
    public Name getName() {
        return propName;
    }
    
    //-----------------------------------< Object >---
    @Override
    public boolean equals(Object other) {
        if (null == other) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (other instanceof AddPropertyImpl) {
            AddPropertyImpl o = (AddPropertyImpl) other;
            return propName.equals(o.propName) &&
                    isMultiValued == o.isMultiValued &&
                    Arrays.equals(values, o.values);                 
        }
        return false;
    }
    
    //-------------------------------< static factory method >---
    public static AddPropertyImpl create(Name propName, int propertyType, boolean isMultiValued, QValue[] values) {
        AddPropertyImpl ap = new AddPropertyImpl(propName, propertyType, isMultiValued, values);
        return ap;
    }
}
