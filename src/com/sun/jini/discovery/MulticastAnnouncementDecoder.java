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

package com.sun.jini.discovery;

import java.io.IOException;
import java.nio.ByteBuffer;
import net.jini.core.constraint.InvocationConstraints;

/**
 * Interface implemented by classes which decode multicast announcement data
 * according to discovery protocol formats.
 *
 * @since 2.0
 */
public interface MulticastAnnouncementDecoder extends DiscoveryFormatProvider {

    /**
     * Decodes the multicast announcement data contained in the given buffer in
     * a manner that satisfies the specified absolute constraints, returning a
     * {@link MulticastAnnouncement} instance that contains the decoded data.
     * <code>null</code> constraints are considered equivalent to empty
     * constraints.  All the specified constraints are checked before this
     * method returns.
     *
     * @param buf a buffer containing the packet data to decode.  The
     * multicast announcement data must begin at position zero of
     * <code>buf</code>.
     * @param constraints the constraints to apply when decoding the data, or
     * <code>null</code>
     * @return the decoded multicast announcement data
     * @throws IOException if an error occurs in interpreting the data
     * @throws UnsupportedConstraintException if unable to satisfy the
     * specified constraints
     * @throws SecurityException if the given constraints cannot be satisfied
     * due to insufficient caller permissions
     * @throws NullPointerException if <code>buf</code> is <code>null</code>
     */
    MulticastAnnouncement decodeMulticastAnnouncement(
					    ByteBuffer buf,
					    InvocationConstraints constraints)
	throws IOException;
}
