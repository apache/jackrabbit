package org.apache.jackrabbit.spi.commons.tree;

import java.util.Arrays;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.Tree;

public class PropertyImpl implements Tree {

    private final Name propName;
    private final int propertyType;
    private final boolean isMultiValued;
    private final QValue[] values;
    
    private PropertyImpl(Name propName, int propertyType, boolean isMultiValued, QValue[] values) {
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
        if (other instanceof PropertyImpl) {
            PropertyImpl o = (PropertyImpl) other;
            return propName.equals(o.propName) &&
                    isMultiValued == o.isMultiValued &&
                    Arrays.equals(values, o.values);
        }
        return false;
    }
    
    //-------------------------------< static factory method >---
    public static PropertyImpl create(Name propName, int propertyType, boolean isMultiValued, QValue[] values) {
        PropertyImpl ap = new PropertyImpl(propName, propertyType, isMultiValued, values);
        return ap;
    }

}
