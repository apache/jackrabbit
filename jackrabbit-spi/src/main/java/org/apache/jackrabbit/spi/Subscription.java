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
package org.apache.jackrabbit.spi;

/**
 * <code>Subscription</code> defines a marker interface for an event
 * subscription. An implementation will likely keep information in this object
 * about the last consumed events and other implementation specific data. A
 * client will usually first create an event filter and then a subscription
 * based on the filter. Events can then be retrieved by calling {@link
 * RepositoryService#getEvents(Subscription, long)}. If a subscription is no
 * longer needed a client should call {@link RepositoryService#dispose(Subscription)}.
 */
public interface Subscription {
}
