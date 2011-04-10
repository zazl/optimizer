package org.dojotoolkit.optimizer.tests;

import org.dojotoolkit.optimizer.amd.v8.AMDJSOptimizerFactory;
import org.osgi.framework.BundleContext;

public class V8AMDRelativeTest extends AMDRelativeTest {
	public V8AMDRelativeTest(BundleContext bundleContext, String[] ids) {
		super(bundleContext, ids);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		factory = new AMDJSOptimizerFactory();
	}
}
