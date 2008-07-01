package org.caesarj.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;

// Use the -D JVM command-line option to filter test cases, e.g.
// java -DtestFilter=*advice0?0*,*mix* test.AllTests
// java -DtestFilter=java.*,mixin.*,vc.*,vcabstr.misc.* test.AllTests
// java -DtestFilter=mixin.inheritance.mixininheritance002 test.AllTests
// Only * (any string) and ? (any character) are allowed wildcards.

public class AllTests {

	public static Test suite() throws Throwable {
		File dir = new File(TestProperties.instance().getTestDir());
		Collection<File> suites = new Vector<File>();
		scan(suites, dir);
		TestSuite completeTest = new TestSuite();

		for (File suite : suites) {
			XMLSuite d = XMLSuite.parse(suite.getName(), new FileInputStream(
					suite));
			if (d.countTestCases() > 0)
				completeTest.addTest(d);
		}

		return completeTest;
	}

	private static void scan(Collection<File> res, File dir) {
		for (File sub : dir.listFiles()) {
			if (sub.isDirectory()) {
				scan(res, sub);
			}
			else if (sub.isFile() && sub.getName().endsWith(".xml")) {
				res.add(sub);
			}
		}
	}
}
