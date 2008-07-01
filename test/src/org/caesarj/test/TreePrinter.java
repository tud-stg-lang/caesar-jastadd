package org.caesarj.test;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.caesarj.ast.CompilationUnit;
import org.caesarj.ast.Program;

class TreePrinter {
  public static void main(String args[]) {
    if(!compile(args))
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
    program.addKeyOption("-version");
    program.addKeyOption("-help");
    program.addKeyOption("-g");

    program.addOptions(args);
    Collection files = program.files();

    if(program.hasOption("-version")) {
      printVersion();
      return false;
    }
    if(program.hasOption("-help")) {
      printUsage();
      return false;
    }
    
	if (files.isEmpty()) {
		if (program.hasValueForOption("-sourcepath")) {
			String spath = program.getValueForOption("-sourcepath");
			if (spath.charAt(spath.length()-1) != File.separatorChar)
				spath += File.separator;
			File spathFile = new File(spath);
			if (spathFile.isDirectory() ) {
				System.out.println("Source directory : "+spath);
				String[] filelist = spathFile.list();
				for (String filename: filelist) {
					String fullname = spath+filename;
					File f = new File(fullname);
					if (f.isFile()) {
						System.out.println("adding file : "+filename);
						files.add(fullname);
					}
				}
			}
		}
		else {
			printUsage();
			return false;
		}
	}

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

    try {
      for(Iterator iter = program.compilationUnitIterator(); iter.hasNext(); ) {
        CompilationUnit unit = (CompilationUnit)iter.next();
        if(unit.fromSource()) {
          Collection errors = new LinkedList();
          unit.errorCheck(errors);
          if(!errors.isEmpty()) {
            System.out.println("Errors:");
            for(Iterator iter2 = errors.iterator(); iter2.hasNext(); ) {
              String s = (String)iter2.next();
              System.out.println(s);
            }
            return false;
          }
        }
      }
    } catch (Error e) {
      System.err.println(e.getMessage());
      return false;
    }

    if(Program.verbose())
      System.out.println("Pretty printing the AST");
    program.dumpTree();
    return true;
  }

  protected static void printUsage() {
    printVersion();
    System.out.println(
      "\nJavaPrettyPrinter\n\n" +
      "Usage: java JavaPrettyPrinter <options> <source files>\n" +
      "  -verbose                  Output messages about what the compiler is doing\n" +
      "  -classpath <path>         Specify where to find user class files\n" +
      "  -sourcepath <path>        Specify where to find input source files\n" + 
      "  -bootclasspath <path>     Override location of bootstrap class files\n" + 
      "  -extdirs <dirs>           Override location of installed extensions\n" +
      "  -d <directory>            Specify where to place generated class files\n" +
      "  -help                     Print a synopsis of standard options\n" +
      "  -version                  Print version information\n"
    );
  }

  protected static void printVersion() {
    System.out.println("Java1.4Frontend (http://jastadd.cs.lth.se) Version R20060729");
  }
}
