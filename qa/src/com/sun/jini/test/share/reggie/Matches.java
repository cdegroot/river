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
package com.sun.jini.test.share.reggie;

import net.jini.core.lookup.*;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * A Matches contains the fields of a ServiceMatches packaged up for
 * transmission between client-side proxies and the registrar server.
 * Instances are never visible to clients, they are private to the
 * communication between the proxies and the server.
 * <p>
 * This class only has a bare minimum of methods, to minimize
 * the amount of code downloaded into clients.
 *
 * 
 *
 */
class Matches implements java.io.Serializable {

    /**
     * ServiceMatches.items as an ArrayList of Item
     *
     * @serial
     */
    public ArrayList items;
    /**
     * ServiceMatches.totalMatches
     *
     * @serial
     */
    public int totalMatches;

    private static final long serialVersionUID = 4446148021075494999L;

    /** Simple constructor. */
    public Matches(ArrayList items, int totalMatches) {
	this.items = items;
	this.totalMatches = totalMatches;
    }

    /** Converts a Matches to a ServiceMatches. */
    ServiceMatches get() throws RemoteException {
	return new ServiceMatches(Item.toServiceItem(items), totalMatches);
    }
}
