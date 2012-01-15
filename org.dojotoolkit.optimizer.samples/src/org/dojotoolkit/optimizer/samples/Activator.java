/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.samples;

import org.dojotoolkit.optimizer.servlet.osgi.ZazlServicesTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	private ZazlServicesTracker zazlServicesTracker = null;
	
	public Activator() {
		zazlServicesTracker = new ZazlServicesTracker("org.dojotoolkit.optimizer.samples.httpcontext");
	}
	
	public void start(BundleContext context) throws Exception {
		zazlServicesTracker.start(context);
	}

	public void stop(BundleContext context) throws Exception {
		zazlServicesTracker.stop();
	}
}
