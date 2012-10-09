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
package net.jini.lookup.ui.factory;

import javax.swing.JComponent;

/**
 * UI factory for a Swing <CODE>JComponent</CODE>.
 *
 * <P>If the UI generated by the method declared in this interface implements
 * <CODE>javax.accessibility.Accessible</CODE> and supports the Java Accessibility
 * API, an <CODE>AccessibleUI</CODE> attribute
 * should be placed in the <CODE>UIDescriptor</CODE>'s <CODE>attributes</CODE> set.
 *
 * 
 */
public interface JComponentFactory extends java.io.Serializable {

    /**
     * Convenience constant to use in the <CODE>toolkit</CODE>
     * field of <CODE>UIDescriptor</CODE>s that contain a
     * <CODE>JComponentFactory</CODE>.
     * The value of this constant is <CODE>"javax.swing"</CODE>.
     */
    String TOOLKIT = "javax.swing";

    /**
     * Convenience constant to use in the <CODE>UIFactoryTypes</CODE>
     * set in the <CODE>attributes</CODE> set of <CODE>UIDescriptor</CODE>s
     * that contain a <CODE>JComponentFactory</CODE>.
     * The value of this constant is <CODE>"net.jini.lookup.ui.factory.JComponentFactory"</CODE>.
     */
    String TYPE_NAME = "net.jini.lookup.ui.factory.JComponentFactory";

    /**
     * Returns a <CODE>JComponent</CODE>.
     *
     * @param roleObject an object defined by the semantics of the UI role interface
     * implemented by the returned UI object. (UI role is indicated in the
     * <code>role</code> field of <code>UIDescriptor</code>s.)
     *
     * @return a <code>JComponent</code> UI
     */
    JComponent getJComponent(Object roleObject);
}

