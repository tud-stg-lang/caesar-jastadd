package org.caesarj.test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestSuite;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLSuite extends TestSuite {
	
	private static String attr(Node n, String name) throws Exception {
		n = n.getAttributes().getNamedItem(name);
		if (n == null)
			throw new Exception(name + " attribute missing");
		return n.getTextContent();
	}

	private static Node elt(Node n, String name) throws Exception {
		NodeList children = n.getChildNodes();
		for (int i = 0; i < children.getLength(); ++i) {
			Node item = children.item(i);
			if (item.getNodeName().equals(name))
				return item;
		}
		throw new Exception(name + " element missing");
	}
	
	private static TestProperties properties() {
		return TestProperties.instance();
	}

	public static XMLSuite parse(String filename, InputStream xml)
			throws Exception {
		XMLSuite res = new XMLSuite();

		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().parse(xml);

		Node root = doc.getFirstChild();

		if (root == null || !root.getNodeName().equals("testsuite"))
			throw new Exception("<testsuite> expected, found"
					+ root.getNodeName());

		String name = attr(root, "name");
		String packageName = attr(root, "package");
		res.setName(name + " (" + filename + ")");

		String commonCodeBase = "";
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); ++i) {
			Node item = children.item(i);

			if (item.getNodeType() != Node.ELEMENT_NODE)
				continue;

			if (item.getNodeName().equals("common-code-base")) {
				commonCodeBase = item.getTextContent();
				continue;
			}

			String id = attr(item, "id");
			String description = attr(item, "description");
			String codeBlock = elt(item, "code").getTextContent();
			String packagePrefix = properties().getPackagePrefix()
			                          + "." + packageName + "." + id;

			if (properties().getTestFilter() != null) {
				if (!properties().getTestFilter().matcher(packageName + "." + id).matches())
					continue;
			}
			
			CompilerTest newTest = null;
			if (item.getNodeName().equals("compile-check-error")) {
				String errorCode = attr(item, "error");
				newTest = new CompileAndFail(packagePrefix, errorCode);
			} else if (item.getNodeName().equals("compile")) {
				newTest = new CompileOnly(packagePrefix);
			} else if (item.getNodeName().equals("compile-run")) {
				newTest = new CompileAndRun(packagePrefix);
			}

			if (newTest == null) {
				throw new Exception(
						"<compile-check-error>, <compile>, or <compile-run> expected, found "
								+ item.getNodeName());
			}
			
			Set binaryNodes = getBinaryNodes(item, "binary");
			newTest.setBinaries(binaryNodes);
			
			if (newTest instanceof CompileAndRun) {
				String testMethodBlock = elt(item, "test").getTextContent();
				((CompileAndRun) newTest).addMain(testMethodBlock);
			}

			newTest.setName(id + " - " + description);
			newTest.addSource("Code.java", codeBlock);
			if (commonCodeBase.length() > 0) {
				newTest.addSource("Common.java", commonCodeBase);
				if (!binaryNodes.isEmpty()) {
					Set binariesWithCommonCodeBase = new HashSet(binaryNodes);
					binariesWithCommonCodeBase.add(commonCodeBase);
					newTest.setBinaries(binariesWithCommonCodeBase);
				}
			}
			res.addTest(newTest);
		}

		return res;
	}

	private static Set getBinaryNodes(Node parent,String nodeName) {
		HashSet binaryCodeSet = new HashSet();
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); ++i) {
			Node item = children.item(i);
			if (item.getNodeName().equals(nodeName)) {
				String code = item.getTextContent();
				binaryCodeSet.add(code);
			}
		}
		return binaryCodeSet;
	}
	
}
