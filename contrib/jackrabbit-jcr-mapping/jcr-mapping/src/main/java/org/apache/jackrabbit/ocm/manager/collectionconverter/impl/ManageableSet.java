package org.apache.jackrabbit.ocm.manager.collectionconverter.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection;


/**
 * This class/interface 
 */
public class ManageableSet extends HashSet implements ManageableCollection {
    public ManageableSet() {
    }
    
    public ManageableSet(Collection collection) {
        super(collection);
    }
    
    /**
     * @see org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection#addObject(java.lang.Object)
     */
    public void addObject(Object object) {
        add(object);
    }

    /**
     * @see org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection#getIterator()
     */
    public Iterator getIterator() {
        return iterator();
    }

    /**
     * @see org.apache.jackrabbit.ocm.manager.collectionconverter.ManageableCollection#getSize()
     */
    public int getSize() {
        return size();
    }

}
