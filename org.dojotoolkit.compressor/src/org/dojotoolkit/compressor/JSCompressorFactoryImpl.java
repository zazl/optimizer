/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.compressor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.server.util.resource.ResourceLoader;

public class JSCompressorFactoryImpl implements JSCompressorFactory {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.compressor");
	private static final String DEFAULT_JS_COMPRESSOR_CLASS = "org.dojotoolkit.compressor.shrinksafe.ShrinksafeJSCompressor"; 
	private Constructor<JSCompressor> jsCompressorConstructor = null;
	
	public JSCompressorFactoryImpl() {
		try {
			Class<JSCompressor> jsCompressorClass = null;
			URL propsURL = getClass().getClassLoader().getResource("/org_dojotoolkit_compressor.properties");
			if (propsURL != null) {
				logger.logp(Level.FINE, getClass().getName(), "JSCompressorFactoryImpl", "org_dojotoolkit_compressor.properties is available");
				InputStream is = null;
				try {
					is = propsURL.openStream();
					Properties props = new Properties();
					props.load(is);
					String jsCompressorClassName = props.getProperty("jsCompressorClassName");
					if (jsCompressorClassName != null) {
						logger.logp(Level.FINE, getClass().getName(), "JSCompressorFactoryImpl", "jsCompressorClassName is to ["+jsCompressorClassName+"]");
						jsCompressorClass = (Class<JSCompressor>) getClass().getClassLoader().loadClass(jsCompressorClassName);
					}
				} catch (IOException e) {
					logger.logp(Level.SEVERE, getClass().getName(), "JSOptimizerFactoryImpl", "Implementation of JSCompressor defined in org_dojotoolkit_compressor.properties is unavailable", e);
					throw new IllegalStateException("Implementation of JSCompressor defined in org_dojotoolkit_compressor.properties is unavailable");		
				} finally {
					if (is != null) { 
						try { 
							is.close(); 
						} catch (IOException e) { 
						}
					}
				}
			}
			if (jsCompressorClass == null) {
				logger.logp(Level.INFO, getClass().getName(), "JSCompressorFactoryImpl", "jsCompressorClassName is default of  ["+DEFAULT_JS_COMPRESSOR_CLASS+"]");
				jsCompressorClass = (Class<JSCompressor>) getClass().getClassLoader().loadClass(DEFAULT_JS_COMPRESSOR_CLASS);
			}
			jsCompressorConstructor = jsCompressorClass.getConstructor(new Class[] {ResourceLoader.class});
		} catch (ClassNotFoundException e) {
			logger.logp(Level.SEVERE, getClass().getName(), "JSOptimizerFactoryImpl", "Implementation of JSCompressor defined in org_dojotoolkit_compressor.properties is unavailable", e);
			throw new IllegalStateException("Implementation of JSCompressor defined in org_dojotoolkit_compressor.properties is unavailable");		
		} catch (NoSuchMethodException e) {
			logger.logp(Level.SEVERE, getClass().getName(), "JSOptimizerFactoryImpl", "JSCompressor implementation does not have a valid constructor", e);
			throw new IllegalStateException("JSCompressor implementation does not have a valid constructor");		
		}
	}
	
	public JSCompressor createJSCompressor(ResourceLoader resourceLoader) {
		JSCompressor jsCompressor = null;
		
		try {
			jsCompressor = jsCompressorConstructor.newInstance(new Object[] {resourceLoader});
		} catch (Exception e) {
			logger.logp(Level.SEVERE, getClass().getName(), "createJSCompressor", "Exception thrown while creating and instance of "+jsCompressorConstructor.toString(), e);
		}
		 
		return jsCompressor; 
	}

}
