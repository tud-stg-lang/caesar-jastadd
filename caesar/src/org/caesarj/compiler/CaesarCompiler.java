package org.caesarj.compiler;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.caesarj.ast.CompilationUnit;
import org.caesarj.ast.Program;

import parser.JavaParser;

public class CaesarCompiler {

	public static void main(String args[]) {
		if (!compile(args))
			System.exit(1);
	}

	public static boolean compile(String args[]) {
		Program program = new Program();
		program.initOptions();
		program.addKeyValueOption("-classpath");
		program.addKeyValueOption("-sourcepath");
		program.addKeyValueOption("-bootclasspath");
		program.addKeyValueOption("-extdirs");
		program.addKeyValueOption("-d");
		program.addKeyOption("-verbose");
		program.addKeyOption("-logcompiler");
		program.addKeyOption("-version");
		program.addKeyOption("-help");
		program.addKeyOption("-g");
		program.addKeyOption("-checkonly");

		program.addOptions(args);
		
		if (program.hasOption("-version")) {
			printVersion();
			return false;
		}
		if (program.hasOption("-help")) {
			printUsage();
			return false;
		}
		
		if (!collectSourceFiles(program)) {
			return false;
		}
		
		try {
			program.insertExternalizedVC();
			
			if (Program.verbose()) {
				System.out.println("Parsed source code:");
				System.out.println(program.toString());
			}

			if (containsErrors(program)) return false;

			if (!program.hasOption("-checkonly")) {
				System.out.println("Generating class files ...");
				program.java2Transformation();
				program.generateClassfile();
			}

			System.out.println("Done.");
		} catch (JavaParser.SourceError e) {
			System.err.println(e.getMessage());
			return false;
		} /* catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			return false;
		} */
		return true;
	}
	
	public static boolean collectSourceFiles(Program program) {
		Collection files = program.files();
		
		if (files.isEmpty()) {
			if (program.hasValueForOption("-sourcepath")) {
				String spath = program.getValueForOption("-sourcepath");
				if (spath.charAt(spath.length()-1) != File.separatorChar)
					spath += File.separator;
				File spathFile = new File(spath);
				if (isValidDirectory(spathFile)) {
					if (program.logCompiler()) {
						System.out.println("Source directory : "+spath);
					}
					try {
						scanDirectory(program, files, spath, spathFile);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			else {
				printUsage();
				return false;
			}
		}
		
		program.initCjSourceFiles();

		for (Iterator iter = files.iterator(); iter.hasNext();) {
			String name = (String) iter.next();
			File file = new File(name);
			if ((!file.exists()) && (program.hasValueForOption("-sourcepath"))) {
				String spath = program.getValueForOption("-sourcepath");
				if (spath.charAt(spath.length()-1) != File.separatorChar)
					spath += File.separator;
				File newFile = new File(spath+name);
				if (newFile.isFile())
					name = newFile.getAbsolutePath();
				System.out.println("check 1 file : "+name);
			}
			program.addSourceFile(name);
		}
		
		return true;
	}

	// Ignore directories named "test"
	private static boolean isValidDirectory(File spathFile) {
		return spathFile.isDirectory() && !spathFile.getName().equals("test");
	}

	private static void scanDirectory (Program program, Collection files, String spath,
			File spathFile) throws IOException {
		String[] filelist = spathFile.list();
		for (String filename: filelist) {
			String fullname = spath+filename;
			File f = new File(fullname);
			if (f.isFile() && fullname.endsWith(".java")) {
				if (program.logCompiler()) {
					System.out.println("                   + "+filename);
				}
				files.add(fullname);
			} else if (isValidDirectory(f)) {
				if (program.logCompiler()) {
					System.out.println("adding directory : "+f.getPath());
				}
				scanDirectory(program, files, f.getPath()+File.separator, f);
			}
		}
	}

	private static boolean containsErrors(Program program) {
		System.out.println("Checking for errors ...");
		Collection errors = new LinkedList();
		program.errorCheck(errors);
		if (!errors.isEmpty()) {
			System.out.println("Errors:");
			for (Iterator err = errors.iterator(); err.hasNext();) {
				System.out.println((String) err.next());
			}
			return true;
		}
		return false;
	}

	protected static void printUsage() {
		printVersion();
		System.out
				.println("\nCaesarJ Compiler\n\n"
						+ "Usage: java CaesarCompiler <options> <source files>\n"
						+ "  -verbose                  Output messages about what the compiler is doing\n"
						+ "  -classpath <path>         Specify where to find user class files\n"
						+ "  -sourcepath <path>        Specify where to find input source files\n"
						+ "  -bootclasspath <path>     Override location of bootstrap class files\n"
						+ "  -extdirs <dirs>           Override location of installed extensions\n"
						+ "  -d <directory>            Specify where to place generated class files\n"
						+ "  -help                     Print a synopsis of standard options\n"
						+ "  -version                  Print version information\n");
	}

	protected static void printVersion() {
		System.out.println("Java1.4Frontend + Backend + Java5 extensions (http://jastadd.cs.lth.se) Version R20060915");
		System.out.println("CaesarJ: Multiple Inheritance extension Version R20080204");
	}

}
