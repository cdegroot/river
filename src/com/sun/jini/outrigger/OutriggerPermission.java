/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sun.jini.outrigger;

import net.jini.security.AccessPermission;
import net.jini.jeri.BasicInvocationDispatcher;
import net.jini.jeri.BasicJeriExporter;

/**
 * Permission that can be used to express the access control policy for an
 * instance of an Outrigger server exported with a {@link
 * BasicJeriExporter}.  This class can be specified to {@link
 * BasicInvocationDispatcher}, which will then perform permission checks for
 * incoming remote calls using <code>OutriggerPermission</code> instances.
 *
 * 
 * @since 2.0
 */
public class OutriggerPermission extends AccessPermission {
    private static final long serialVersionUID = 1;

    /**
     * Create a new <code>OutriggerPermission</code> instance.
     * See {@link AccessPermission} for details on
     * the name parameter.
     * @param name the target name
     * @throws NullPointerException if the target name is <code>null</code>
     * @throws IllegalArgumentException if the target name does not match
     * the syntax specified in the comments at the beginning of
     * {@link AccessPermission}.
     */
    public OutriggerPermission(String name) {
	super(name);
    }
}
