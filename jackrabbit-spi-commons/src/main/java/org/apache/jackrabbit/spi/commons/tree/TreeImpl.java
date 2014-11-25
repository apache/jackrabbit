package org.apache.jackrabbit.spi.commons.tree;

import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Tree;

public class TreeImpl implements Tree {

    Name nodeName;
    Name ntName;
    String uuid;
    List<PropertyImpl> childP = new ArrayList<PropertyImpl>();
    List<TreeImpl> childN = new ArrayList<TreeImpl>();
    
    private TreeImpl(Name nodeName, Name ntName, String uuid) {
        this.nodeName = nodeName;
        this.ntName = ntName;
        this.uuid = uuid;
    }
    
    public void addNode(TreeImpl node) {
        childN.add(node);
    }
    
    public void addProperty(PropertyImpl prop) {
        childP.add(prop);
    }
    
    public List<TreeImpl> getAddNodes() {
        return childN;
    }
        
    public Name getNodeTypeName() {
        return ntName;
    }
    
    public String getUniqueIdentifier() {
        return uuid;
    }
    
    public List<PropertyImpl> getAddProperties() {
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
        if (other instanceof TreeImpl) {
            TreeImpl o = (TreeImpl) other;
            return nodeName.equals(o.nodeName) &&
                   ntName.equals(o.ntName) &&
                   uuid.equals(o.uuid) &&
                   childN.equals(o.childN) &&
                   childP.equals(o.childP);   
        }
        return false;
    }
    //---------------------------------------------< static factory method >---
    public static Tree create(Name nodeName, Name ntName, String uuid) {
        Tree an = new TreeImpl(nodeName, ntName, uuid);
        return an;
    }

}
