testClass=StressTestWithShutdown
testCategories=javaspace,javaspace_impl,javaspace_impl_matching,stress
com.sun.jini.qa.harness.timeout=14400
#com.sun.jini.qa.harness.verifier=com.sun.jini.qa.harness.SkipLinuxTestVerifier
include0=../outrigger.properties

com.sun.jini.test.share.restart_wait=10000
com.sun.jini.test.impl.outrigger.matching.StressTest.num_entries=10000
com.sun.jini.test.impl.outrigger.matching.StressTest.num_readers=500
com.sun.jini.test.impl.outrigger.matching.StressTest.num_writers=500
com.sun.jini.test.impl.outrigger.matching.tryShutdown=1

