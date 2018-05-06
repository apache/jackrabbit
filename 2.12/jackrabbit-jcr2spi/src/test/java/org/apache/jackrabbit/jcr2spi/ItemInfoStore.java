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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.commons.iterator.Iterators;

import javax.jcr.ItemNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * In memory store for {@link ItemInfo}s.
 */
public class ItemInfoStore {
    private final Map<ItemId, ItemInfo>infos = new HashMap<ItemId, ItemInfo>();
    private final Map<ItemId, Collection<ItemInfo>> batches = new HashMap<ItemId, Collection<ItemInfo>>();
    private final Map<ItemId, Collection<ChildInfo>> childInfos = new HashMap<ItemId, Collection<ChildInfo>>();

    /**
     * Retrieve an item by its <code>id</code>.
     * @param id
     * @return
     * @throws ItemNotFoundException  if no such item exists
     */
    public ItemInfo getItemInfo(ItemId id) throws ItemNotFoundException {
        ItemInfo itemInfo = infos.get(id);

        return itemInfo == null
            ? ItemInfoStore.<ItemInfo>notFound(id)
            : itemInfo;
    }

    /**
     * Retrieve an iterator over all items
     * @return
     */
    public Iterator<ItemInfo> getItemInfos() {
        return infos.values().iterator();
    }

    /**
     * Retrieve a node by its <code>id</code>.
     *
     * @param id
     * @return
     * @throws ItemNotFoundException  if no such node exists
     */
    public NodeInfo getNodeInfo(NodeId id) throws ItemNotFoundException {
        ItemInfo itemInfo = getItemInfo(id);

        return itemInfo.denotesNode()
            ? (NodeInfo) itemInfo
            : ItemInfoStore.<NodeInfo>notFound(id);
    }

    /**
     * Retrieve a property by its <code>id</code>.
     *
     * @param id
     * @return
     * @throws ItemNotFoundException  if no such property exists
     */
    public PropertyInfo getPropertyInfo(PropertyId id) throws ItemNotFoundException {
        ItemInfo itemInfo = getItemInfo(id);

        return itemInfo.denotesNode()
            ? ItemInfoStore.<PropertyInfo>notFound(id)
            : (PropertyInfo) itemInfo;
    }

    /**
     * Retrieve all items of a batch
     * @see RepositoryService#getItemInfos(org.apache.jackrabbit.spi.SessionInfo, NodeId)
     *
     * @param id
     * @return
     */
    public Iterator<? extends ItemInfo> getBatch(ItemId id) {
        Iterable<ItemInfo> batch = batches.get(id);

        return batch == null
            ? Iterators.<ItemInfo>empty()
            : batch.iterator();
    }

    /**
     * Retrieve the {@link ChildInfo}s of a node
     *
     * @param id
     * @return
     * @throws ItemNotFoundException  if no such node exists
     */
    public Iterator<ChildInfo> getChildInfos(NodeId id) throws ItemNotFoundException {
        Iterable<ChildInfo> childs = childInfos.get(id);

        return childs == null
            ? ItemInfoStore.<Iterator<ChildInfo>>notFound(id)
            : childs.iterator();
    }

    /**
     * Add an {@link ItemInfo}
     *
     * @param info
     */
    public void addItemInfo(ItemInfo info) {
        infos.put(info.getId(), info);
    }

    /**
     * Add a {@link ItemInfo} to a batch
     *
     * @param id
     * @param info
     */
    public void updateBatch(ItemId id, ItemInfo info) {
        if (!batches.containsKey(id)) {
            batches.put(id, new ArrayList<ItemInfo>());
        }

        batches.get(id).add(info);
    }

    /**
     * Add a {@link ChildInfo} to a node
     * @param id
     * @param info
     */
    public void updateChilds(ItemId id, ChildInfo info) {
        if (!childInfos.containsKey(id)) {
            childInfos.put(id, new ArrayList<ChildInfo>());
        }

        childInfos.get(id).add(info);
    }

    /**
     * Set the {@link ChildInfo}s of a node
     *
     * @param id
     * @param infos
     */
    public void setChildInfos(NodeId id, Iterator<ChildInfo> infos) {
        childInfos.put(id, toList(infos));
    }

    // -----------------------------------------------------< private >---

    private static <T> T notFound(ItemId itemId) throws ItemNotFoundException {
        throw new ItemNotFoundException(itemId.toString());
    }

    private static <T> List<T> toList(Iterator<T> infos) {
        List<T> list = new ArrayList<T>();

        while (infos.hasNext()) {
            list.add(infos.next());
        }
        return list;
    }

}
