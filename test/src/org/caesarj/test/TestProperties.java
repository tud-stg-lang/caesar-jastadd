/*
 * This source file is part of CaesarJ
 * For the latest info, see http://caesarj.org/
 *
 * Copyright © 2003-2005
 * Darmstadt University of Technology, Software Technology Group
 * Also see acknowledgements in readme.txt
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: TestProperties.java,v 1.5 2005-12-16 16:29:43 klose Exp $
 */

package org.caesarj.test;

import java.io.File;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * ...
 *
 * @author Ivica Aracic
 */
public class TestProperties {
	private static TestProperties singleton = new TestProperties();

	private boolean traceLoader = false;
	private boolean compilerVerbose = true;
	private boolean compilerVerboseBinaries = false;
	private boolean testcaseVerbose = true;
	private boolean printCode = true;

	private String packagePrefix = "testrunner";

	private String binDir = "outdir" + File.separator + "bin";
    private String genSrcDir = "outdir" + File.separator + "src";
    private String testDir = "src" + File.separator + "test" + File.separator + "suites";

    private Pattern testFilter = null;

    public static TestProperties instance() {
        return singleton;
    }

    private Properties props = new Properties();

    private TestProperties() {
        try {
            props.load(
                AllTests.class.
                	getResourceAsStream("test.properties")
        	);

            String pattern = getPropValue("testFilter", "*");

        	// Use the -D JVM command-line option to filter test cases, e.g.
        	// java -DtestFilter=*advice0?0*,*mix* test.AllTests
        	// java -DtestFilter=java.*,mixin.*,vc.*,vcabstr.misc.* test.AllTests
        	// java -DtestFilter=mixin.inheritance.mixininheritance002 test.AllTests
        	// Only * (any string) and ? (any character) are allowed wildcards.

            pattern = Pattern.quote(pattern);
            pattern = pattern.replaceAll("\\*", "\\\\E.*\\\\Q");
            pattern = pattern.replaceAll("\\?", "\\\\E.\\\\Q");
            pattern = pattern.replaceAll(",", "\\\\E|\\\\Q");
			// testFilter = testFilter.replaceAll("\\\\Q\\\\E", "");
            pattern = pattern.replaceAll(Pattern.quote("\\Q\\E"), "");
			testFilter = Pattern.compile(pattern);

            binDir = getPropValue("binDir", binDir);
            testDir = getPropValue("testDir", testDir);
            genSrcDir = getPropValue("genSrcDir", genSrcDir);
            packagePrefix = getPropValue("packagePrefix", packagePrefix);
            traceLoader = getBoolValue("traceLoader", traceLoader);
            compilerVerbose = getBoolValue("compilerVerbose", compilerVerbose);
            compilerVerboseBinaries = getBoolValue("compilerVerboseBinaries", compilerVerboseBinaries);
            testcaseVerbose = getBoolValue("testcaseVerbose", testcaseVerbose);
            printCode = getBoolValue("printCode", printCode);
        }
        catch (Exception e) {
            // do nothing, just continue with default values
        }
    }

    private String getPropValue(String key, String defVal) {
    	try {
	    	String value = System.getProperty(key);
	    	if (value != null)
	    		return value;
	    	else if (props.containsKey(key))
	    		return props.getProperty(key);
    	}
    	catch (Exception e) {
            // do nothing, just continue with default values
        }
    	return defVal;
    }

    private boolean getBoolValue(String key, boolean defVal) {
    	return getPropValue(key, String.valueOf(defVal))
    	          .trim().toLowerCase().equals("true");
    }

    public Pattern getTestFilter() {
        return testFilter;
    }

    public String getBinDir() {
        return binDir;
    }

    public String getTestDir() {
        return testDir;
    }

    public String getGenSrcDir() {
        return genSrcDir;
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public boolean getTraceLoader() {
        return traceLoader;
    }

    public boolean getCompilerVerbose() {
        return compilerVerbose;
    }

    public boolean getCompilerVerboseBinaries() {
        return compilerVerboseBinaries;
    }

    public boolean getTestcaseVerbose() {
        return testcaseVerbose;
    }

    public boolean getPrintCode() {
        return printCode;
    }
}
