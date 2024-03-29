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
package com.sun.jini.test.impl.start;

import java.util.logging.Level;

import java.rmi.*;
import java.rmi.activation.*;

import com.sun.jini.start.*;
import com.sun.jini.start.ActivateWrapper.*;
import com.sun.jini.start.ServiceStarter.*;
import com.sun.jini.qa.harness.TestException;

/**
 * This test verifies that the ActivateDesc stored by the Activation system
 * matches the provided values. Note that ActivateWrapperActivateDescTest
 * already tests that the constructor provided values are the same as those
 * found in the ActivateDesc object, so there's no need to create one from
 * scratch here. We just call the getServiceActivateDesc() utility routine
 * and use the provided field values.
 */

public class ActivateWrapperActivateDescTest2 extends AbstractStartBaseTest {

    public void run() throws Exception {
        logger.log(Level.INFO, "" + ":run()");
        ActivateDesc adesc = 
            ActivateWrapperTestUtil.getServiceActivateDesc(
                "com.sun.jini.test.impl.start.Probe", getConfig());
        logger.log(Level.INFO, "Probe ActivateDesc = " + adesc);
    
        logger.log(Level.INFO, "Marshalling ActivateDesc");
        MarshalledObject mo = new MarshalledObject(adesc);
    
        logger.log(Level.INFO, "Obtaining shared group info");
        ActivationGroupID gid = TestUtil.loadSharedCreate(manager.getSharedVMLog());
    
        logger.log(Level.INFO, "Obtaining probe reference");
        ActivationID aid = 
	        ActivateWrapper.register(gid, adesc, false, ActivationGroup.getSystem());
        logger.log(Level.INFO, "Obtaining activation id");
        logger.log(Level.INFO, "ActivationID: " + aid);
    
        logger.log(Level.INFO, "Obtaining ActivationDesc via activation system");
        ActivationDesc desc = 
                ActivationGroup.getSystem().getActivationDesc(aid);
        logger.log(Level.INFO, "ActivationDesc: " + desc);
    
        logger.log(Level.INFO, "Comparing ActivationDesc vs ActivateDesc");
        if (!mo.equals(desc.getData())) {
            throw new TestException( "ActivateWrapper descriptor "
    		+ "does not match version stored with activation.");
    	}
        logger.log(Level.INFO, "ActivationDesc and ActivateDesc information"
	        + " matches");
    
    	return;
    }
}
