package org.dojotoolkit.optimizer.tests;

import org.dojotoolkit.optimizer.amd.v8.AMDJSOptimizerFactory;
import org.osgi.framework.BundleContext;

public class V8AMDOptimizerTest extends AMDOptimizerTest {
	public V8AMDOptimizerTest(BundleContext bundleContext, String[] ids) {
		super(bundleContext, ids);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		factory = new AMDJSOptimizerFactory();
	}
}
