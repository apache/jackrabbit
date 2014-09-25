package org.apache.jackrabbit.spi.commons.batch;

import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.spi.AddItem;
import org.apache.jackrabbit.spi.Name;

public class AddNodeImpl implements AddItem {

    Name nodeName;
    Name ntName;
    String uuid;
    List<AddPropertyImpl> childP = new ArrayList<AddPropertyImpl>();
    List<AddNodeImpl> childN = new ArrayList<AddNodeImpl>();
    
    private AddNodeImpl(Name nodeName, Name ntName, String uuid) {
        this.nodeName = nodeName;
        this.ntName = ntName;
        this.uuid = uuid;
    }
    
    public void addNode(AddNodeImpl node) {
        childN.add(node);
    }
    
    public void addProperty(AddPropertyImpl prop) {
        childP.add(prop);
    }
    
    public List<AddNodeImpl> getAddNodes() {
        return childN;
    }
        
    public Name getNodeTypeName() {
        return ntName;
    }
    
    public String getUniqueIdentifier() {
        return uuid;
    }
    
    public List<AddPropertyImpl> getAddProperties() {
        return childP;
    }
    //---------------------------------------------< AddItem >--
    @Override
    public Name getName() {
        return nodeName;
    }
    //---------------------------------------------< Object >--
    @Override
    public boolean equals(Object other) {
        if (null == other) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (other instanceof AddNodeImpl) {
            AddNodeImpl o = (AddNodeImpl) other;
            return nodeName.equals(o.nodeName) &&
                   ntName.equals(o.ntName) &&
                   uuid.equals(o.uuid) &&
                   childN.equals(o.childN) &&
                   childP.equals(o.childP);   
        }
        return false;
    }
    //---------------------------------------------< static factory method >---
    public static AddNodeImpl create(Name nodeName, Name ntName, String uuid) {
        AddNodeImpl an = new AddNodeImpl(nodeName, ntName, uuid);
        return an;
    }
}
