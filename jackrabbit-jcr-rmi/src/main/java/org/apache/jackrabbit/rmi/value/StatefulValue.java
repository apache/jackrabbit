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
package org.apache.jackrabbit.rmi.value;

import java.io.Serializable;

import javax.jcr.Value;

/**
 * The <code>StatefullValue</code> interface defines the API used for the state
 * classes used by the {@link org.apache.jackrabbit.rmi.value.SerialValue} class.
 * <p>
 * This is a marker interface with two purposes; it separates the value
 * state classes from the more general value classes, and it forces the
 * state classes to be serializable. This interface is used only internally
 * by the State pattern implementation of the
 * {@link org.apache.jackrabbit.rmi.value.SerialValue} class.
 * <p>
 * This interface is not intended to be implemented by clients. Rather any of
 * the concrete implementations of this class should be used or overwritten as
 * appropriate.
 *
 * @see org.apache.jackrabbit.rmi.value.SerialValue
 */
public interface StatefulValue extends Value, Serializable {
}
