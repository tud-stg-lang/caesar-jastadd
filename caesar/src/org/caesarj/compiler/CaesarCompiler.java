package org.caesarj.compiler;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.caesarj.ast.BytecodeParser;
import org.caesarj.ast.CompilationUnit;
import org.caesarj.ast.JavaParser;
import org.caesarj.ast.Options;
import org.caesarj.ast.Problem;
import org.caesarj.ast.Program;
import org.caesarj.util.ProgressTracker;
import org.caesarj.util.VerboseProgress;

public class CaesarCompiler {
	static protected Program program = null;
	static protected Collection<String> errors = null;
	static protected ProgressTracker progressTracker = null;
	
	public static void initialize() {
		program = new Program();
		Options options = program.options();
		errors = new LinkedList<String>();
		progressTracker = new ProgressTracker();
		options.initOptions();
		options.addKeyValueOption("-classpath");
		options.addKeyValueOption("-sourcepath");
		options.addKeyValueOption("-bootclasspath");
		options.addKeyValueOption("-extdirs");
		options.addKeyValueOption("-d");
		options.addKeyOption("-logcompiler");
		options.addKeyOption("-verbose");
		options.addKeyOption("-version");
		options.addKeyOption("-help");
		options.addKeyOption("-g");
		options.addKeyOption("-checkonly");
		
		program.initBytecodeReader(new BytecodeParser());
		program.initJavaParser(new JavaParser() {
          public CompilationUnit parse(java.io.InputStream is, String fileName) throws java.io.IOException, beaver.Parser.Exception {
            return new parser.JavaParser().parse(is, fileName);
          }
        });		
	}
	
	public static void cleanUp() {
		program = null;
		errors = null;
		progressTracker = null;
	}
	
	public static void addOptions(String args[]) {
		program.options().addOptions(args);
	}
	
	public static void setOption(String name) {
		program.options().setOption(name);
	}
	
	public static void setValueForOption(String value, String option) {
		program.options().setValueForOption(value, option);
	}
	
	/**
	 * Add a new source file
	 */
	public static void addSourceFile(String filename) {
		program.addSourceFile(filename);
	}
	
	/**
	 * Get compiler errors
	 */
	public static Collection<String> getErrors() {
		return errors;
	}
	
	/**
	 * Get progress tracker 
	 */
	public static ProgressTracker getProgressTracker() {
		return progressTracker;
	}
	
	/**
	 * Change the progress tracker 
	 * (should be called before initialization of compiler) 
	 */
	public static void setProgressTracker(ProgressTracker tracker) {
		progressTracker = tracker;
	}
	
	/**
	 * Pretty printing of parsed AST
	 */
	public static String getParsedCode() {
		return program.toString();			
		/* 
		catch (JavaParser.SourceError e) {
			errors.add(e.getMessage());
			return e.getMessage();
		}
		*/	
	}
	
	/**
	 * Collects source files according to the compiler options
	 * (An alternative to manually adding source files)
	 */
	
	public static boolean collectSourceFiles() {
		
		progressTracker.startPhase("collectSrc", "Collecting source files...", 0.05);
		
		Collection files = program.options().files();
		
		if (files.isEmpty()) {
			if (program.options().hasValueForOption("-sourcepath")) {
				String spath = program.options().getValueForOption("-sourcepath");
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
			if ((!file.exists()) && (program.options().hasValueForOption("-sourcepath"))) {
				String spath = program.options().getValueForOption("-sourcepath");
				if (spath.charAt(spath.length()-1) != File.separatorChar)
					spath += File.separator;
				File newFile = new File(spath+name);
				if (newFile.isFile())
					name = newFile.getAbsolutePath();
				System.out.println("check 1 file : "+name);
			}
			program.addSourceFile(name);
		}
		
		progressTracker.endPhase("collectSrc");
		
		return true;
	}
	
	public static boolean typeCheck() {
		
		progressTracker.startPhase("typeCheck",	"Checking for errors...", 0.5);
		
		try {
			program.insertExternalizedVC();
			
			double step = 1.0 / countSourceCUs();
			for(Iterator iter = program.compilationUnitIterator(); iter.hasNext(); ) {
				CompilationUnit cu = (CompilationUnit)iter.next();
				if (cu.fromSource()) {
					progressTracker.advanceProgress("Checking for errors: " + cu.relativeName(), step);
					cu.collectErrors();	
					for (Object e: cu.getErrors()) {
						if (e instanceof Problem) {
							errors.add(((Problem)e).toString());
						}
						else {
							errors.add((String)e);
						}						
					}
				}
			}			
			
			if (!errors.isEmpty()) {
				return false;
			}
		}
		/* TODO: find out how to parser errors are handled
		catch (JavaParser.SourceError e) {
			errors.add(e.getMessage());
			return false;
		}
		*/ 
		finally {
			progressTracker.endPhase("typeCheck");
		}
		
		return true;
	}
	
	public static void generateBytecode() {
		// estimated to take 45% of compilation time
		progressTracker.startPhase("genBytecode", "Generating bytecode....", 0.45);
		
		double step = 1.0 / countSourceCUs();
        for(Iterator iter = program.compilationUnitIterator(); iter.hasNext(); ) {
        	CompilationUnit cu = (CompilationUnit)iter.next();
        	if (cu.fromSource()) {
	        	progressTracker.advanceProgress("Generating bytecode: " + cu.relativeName(), step);
	        	cu.transformation();
	      		cu.generateClassfile();
        	}
    	}
        
        progressTracker.endPhase("genBytecode");
	}
	
	private static int countSourceCUs() {
		int count = 0;
		for(Iterator iter = program.compilationUnitIterator(); iter.hasNext(); ) {
        	CompilationUnit cu = (CompilationUnit)iter.next();
        	if (cu.fromSource()) {
        		count++;        		
        	}
		}
		return count;
	}

	// Ignore directories named "test"
	protected static boolean isValidDirectory(File spathFile) {
		return spathFile.isDirectory() && !spathFile.getName().equals("test");
	}

	protected static void scanDirectory (Program program, Collection files, String spath,
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
						+ "  -version                  Print version information\n"
						+ "  -checkonly                Perform only type checking\n");
	}

	protected static void printVersion() {
		System.out.println("Java1.4Frontend + Backend + Java5 extensions (http://jastadd.cs.lth.se) Version R20060915");
		System.out.println("CaesarJ: Multiple Inheritance extension Version R20080204");
	}
	
	
	public static void main(String args[]) {
		initialize();	
		addOptions(args);
		if (!compile()) {
			System.exit(1);
		}
	}

	/**
	 * Implements compilation process
	 * 
	 * call initialize() and set compiler options before compilation
	 * call cleanUp() after compilation
	 * 
	 * @return success
	 */
	public static boolean compile() {
		
		if (program.options().hasOption("-version")) {
			printVersion();
			return false;
		}
		
		if (program.options().hasOption("-help")) {
			printUsage();
			return false;
		}
		
		if (program.options().verbose()) {
			progressTracker.addProgressListener(new VerboseProgress());
		}
		
		if (!collectSourceFiles()) {
			return false;
		}
		
		if (!typeCheck()) {
			System.out.println("Errors:");
			for (Iterator err = errors.iterator(); err.hasNext();) {
				System.out.println((String) err.next());
			}
			return false;
		}
		
		if (!program.options().hasOption("-checkonly")) {
			generateBytecode();
		}
		return true;
	}	
}
