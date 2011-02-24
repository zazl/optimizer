/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;

public class JSOptimizerFactoryImpl implements JSOptimizerFactory {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer");
	private static final String DEFAULT_JS_OPTIMIZER_CLASS = "org.dojotoolkit.optimizer.rhino.RhinoJSOptimizer"; 
	private Constructor<JSOptimizer> jsOptimizerConstructor = null;
	
	public JSOptimizerFactoryImpl() {
		try {
			Class<JSOptimizer> jsOptimizerClass = null;
			URL propsURL = getClass().getClassLoader().getResource("/org_dojotoolkit_optimizer.properties");
			if (propsURL != null) {
				logger.logp(Level.FINE, getClass().getName(), "JSOptimizerFactoryImpl", "org_dojotoolkit_optimizer.properties is available");
				InputStream is = null;
				try {
					is = propsURL.openStream();
					Properties props = new Properties();
					props.load(is);
					String jsOptimizerClassName = props.getProperty("jsOptimizerClassName");
					if (jsOptimizerClassName != null) {
						logger.logp(Level.FINE, getClass().getName(), "JSOptimizerFactoryImpl", "jsOptimizerClassName is to ["+jsOptimizerClassName+"]");
						jsOptimizerClass = (Class<JSOptimizer>) getClass().getClassLoader().loadClass(jsOptimizerClassName);
					}
				} catch (IOException e) {
					logger.logp(Level.SEVERE, getClass().getName(), "JSOptimizerFactoryImpl", "Implementation of JSOptimizer defined in org_dojotoolkit_optimizer.properties is unavailable", e);
					throw new IllegalStateException("Implementation of JSOptimizer defined in org_dojotoolkit_optimizer.properties is unavailable");		
				} finally {
					if (is != null) { 
						try { 
							is.close(); 
						} catch (IOException e) { 
						}
					}
				}
			}
			if (jsOptimizerClass == null) {
				logger.logp(Level.INFO, getClass().getName(), "JSOptimizerFactoryImpl", "jsOptimizerClassName is default of  ["+DEFAULT_JS_OPTIMIZER_CLASS+"]");
				jsOptimizerClass = (Class<JSOptimizer>) getClass().getClassLoader().loadClass(DEFAULT_JS_OPTIMIZER_CLASS);
			}
			jsOptimizerConstructor = jsOptimizerClass.getConstructor(new Class[] {ResourceLoader.class, RhinoClassLoader.class, boolean.class});
		} catch (ClassNotFoundException e) {
			logger.logp(Level.SEVERE, getClass().getName(), "JSOptimizerFactoryImpl", "Implementation of JSOptimizer defined in org_dojotoolkit_optimizer.properties is unavailable", e);
			throw new IllegalStateException("Implementation of JSOptimizer defined in org_dojotoolkit_optimizer.properties is unavailable");		
		} catch (NoSuchMethodException e) {
			logger.logp(Level.SEVERE, getClass().getName(), "JSOptimizerFactoryImpl", "JSOptimizer implementation does not have a valid constructor", e);
			throw new IllegalStateException("JSOptimizer implementation does not have a valid constructor");		
		}
		
	}
	
	public JSOptimizer createJSOptimizer(ResourceLoader resourceLoader, RhinoClassLoader rhinoClassLoader, boolean javaChecksum) {
		JSOptimizer jsOptimizer = null;
		try {
			jsOptimizer = jsOptimizerConstructor.newInstance(new Object[] {resourceLoader, rhinoClassLoader, javaChecksum});
		} catch (Exception e) {
			logger.logp(Level.SEVERE, getClass().getName(), "createJSOptimizer", "Exception thrown while creating and instance of "+jsOptimizerConstructor.toString(), e);
		} 
		return jsOptimizer;
	}

}
