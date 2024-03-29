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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Interface that must be implemented by objects that must persist their
 * state.
 *
 * @see LogOps
 *
 * 
 *
 * @since 2.0
 */
public interface StorableObject {

    /**  
     * Store the persistent fields 
     */
    public void store(ObjectOutputStream out) throws IOException;

    /**
     * Restore the persistent fields
     */
    public void restore(ObjectInputStream in) 
	throws IOException, ClassNotFoundException;
}
