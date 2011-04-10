package org.dojotoolkit.optimizer.tests;

import org.dojotoolkit.optimizer.amd.v8.AMDJSOptimizerFactory;
import org.osgi.framework.BundleContext;

public class V8AMDCircularDependencyTest extends AMDCircularDependencyTest {
	public V8AMDCircularDependencyTest(BundleContext bundleContext, String[] ids) {
		super(bundleContext, ids);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		factory = new AMDJSOptimizerFactory();
	}
}
