/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer;

import java.io.IOException;
import java.util.Map;

/**
 * Implementations provide access to the analysis information given a list of module ids
 *
 */
public interface JSOptimizer {
	/**
	 * @param modules String array of the modules used to generate the analysis data
	 * @return JSAnalysisData object containing the analysis information
	 * @throws IOException
	 */
	JSAnalysisData getAnalysisData(String[] modules) throws IOException;
	/**
	 * @param modules String array of the modules used to generate the analysis data
	 * @param exclude JSAnalysisData array of the analysis details that will be used to determine what should be excluded from the dependency results
	 * @return JSAnalysisData object containing the analysis information
	 * @throws IOException
	 */
	JSAnalysisData getAnalysisData(String[] modules, JSAnalysisData[] exclude) throws IOException;
	/**
	 * @param key String value that can be used to identify what this analyisData is for
	 * @return JSAnalysisData object containing the analysis information
	 * @throws UnsupportedOperationException if optimizer does not support retrieval by key.
	 */
	JSAnalysisData getAnalysisData(String key) throws UnsupportedOperationException;
	/**
	 * @return Map<String, Object> object configuration information for the Optimizer
	 * @throws IOException
	 */
	Map<String, Object> getConfig();
}
