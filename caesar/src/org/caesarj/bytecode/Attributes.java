package org.caesarj.bytecode;

import java.io.FileNotFoundException;

import org.caesarj.ast.CjVirtualClassDecl;
import org.caesarj.ast.ClassDecl;
import org.caesarj.ast.InterfaceDecl;
import org.caesarj.ast.List;
import org.caesarj.ast.MemberClassDecl;
import org.caesarj.ast.MemberInterfaceDecl;
import org.caesarj.ast.MemberTypeDecl;
import org.caesarj.ast.Program;
import org.caesarj.ast.TypeDecl;


class Attributes {
	private Parser p;
	private List exceptionList;
	private boolean isSynthetic;
	private CONSTANT_Info constantValue;
	private List superclassesList;
	private boolean isCjClass;
	
	/** "Trickle-up": This field is used for passing the outer <code>TypeDecl</code>, to which a 
	 *  <code>MemberClassDecl</code> (corresponding to the inner class) was added, along the cascade 
	 *  of <code title="org.caesarj.bytecode.Attributes">Attributes</code> and 
	 *  <code title="org.caesarj.bytecode.Parser">Parser</code> instances. */
	private TypeDecl outerTypeDecl = null;

	public Attributes(Parser parser) {
		this(parser, null, null, null);
	}
	
	public Attributes(Parser parser, TypeDecl typeDecl, TypeDecl outerTypeDeclParam, Program classPath) {
		p = parser;
		exceptionList = new List();
		isSynthetic = false;
		isCjClass = false;

		int attributes_count = p.u2();
		printIfVerbose("    " + attributes_count + " attributes:");
		for (int j = 0; j < attributes_count; j++) {
			int attribute_name_index = p.u2();
			int attribute_length = p.u4();
			String attribute_name = p.getCONSTANT_Utf8_Info(attribute_name_index).string();
			if(Parser.VERBOSE)
				p.println("    Attribute: " + attribute_name + ", length: " + attribute_length);
			
			// Parse the appropriate attribute and then directly skip to the next iteration of the loop:
			if (attribute_name.equals("de.tud.caesarj.Superclasses")) {
				superclasses();
				continue;
			} else if (attribute_name.equals("de.tud.caesarj.IsCjClass")) {
				isCjClass = true;
				continue;
			} else if (attribute_name.equals("InnerClasses")) {
				innerClasses(typeDecl, outerTypeDeclParam, classPath);
				continue;
			} else if (attribute_name.equals("Exceptions")) {
				exceptions();
				continue;
			} else if(attribute_name.equals("ConstantValue") && attribute_length == 2) {
				constantValues();
				continue;
			} else if (attribute_name.equals("Synthetic")) {
				isSynthetic = true;
				continue;
			} else {
				this.p.skip(attribute_length);
				continue;
			}
			// Do NOT add anything here without appropriate refactoring (remove all continue statements above).
		}
	}

	private void superclasses() {
		int count = p.u2();
		printIfVerbose("      " + count + " superclasses:");
		superclassesList = new List();
		for (int i = 0; i < count; i++) {
			CONSTANT_Class_Info superclass = p.getCONSTANT_Class_Info(p.u2());
			printIfVerbose("        class " + superclass.name());
			superclassesList.add(superclass.access());
		}
	}
	public List superclassesList() {
		return superclassesList;
	}

	
	public void innerClasses(TypeDecl typeDecl, TypeDecl outerTypeDeclParam, Program classPath) {
		int number_of_classes = this.p.u2();
		printIfVerbose("    Number of classes: " + number_of_classes);
		for (int i = 0; i < number_of_classes; i++) {
			printIfVerbose("      " + i + "(" + number_of_classes + ")" +  ":");
			int inner_class_info_index = this.p.u2();
			int outer_class_info_index = this.p.u2();
			int inner_name_index = this.p.u2();
			int inner_class_access_flags = this.p.u2();
			String inner_name = "";
			
			if(!(inner_class_info_index > 0 && outer_class_info_index > 0 && inner_name_index > 0)) {
				continue; // something is missing -> skip the rest of the loop body
			}

			CONSTANT_Class_Info inner_class_info = this.p.getCONSTANT_Class_Info(inner_class_info_index);
			CONSTANT_Class_Info outer_class_info = this.p.getCONSTANT_Class_Info(outer_class_info_index);
			if(inner_class_info == null || outer_class_info == null) {
				System.out.println("Null");
			}
			String inner_class_name = inner_class_info.name();
			String outer_class_name = outer_class_info.name();

			printIfVerbose("      inner: " + inner_class_name + ", outer: " + outer_class_name);

			if (inner_name_index != 0) {
				inner_name = this.p.getCONSTANT_Utf8_Info(inner_name_index).string();
			} else {
				inner_name = inner_class_info.simpleName();
			}

			if (inner_class_info.name().equals(p.classInfo.name())) {
				printIfVerbose("      Class " + inner_class_name + " is inner");
				typeDecl.setID(inner_name);
				typeDecl.setModifiers(Parser.modifiers(inner_class_access_flags & 0x041f));
				if (this.p.outerClassInfo != null && this.p.outerClassInfo.name().equals(outer_class_info.name())) {
					MemberTypeDecl m = null;
					if (typeDecl instanceof ClassDecl) {
						m = new MemberClassDecl((ClassDecl)typeDecl);
						outerTypeDeclParam.addBodyDecl(m);
					} else if (typeDecl instanceof InterfaceDecl) {
						m = new MemberInterfaceDecl((InterfaceDecl)typeDecl);
						outerTypeDeclParam.addBodyDecl(m);
					}
				}
			}
			else if (outer_class_info.name().equals(this.p.classInfo.name())) {
				printIfVerbose("      Class " + this.p.classInfo.name() + " has inner class: " + inner_class_name);
				printIfVerbose("Begin processing: " + inner_class_name);
				try {
					java.io.InputStream is = classPath.getInputStream(inner_class_name);
					if(is != null) {
						Parser p = new Parser(is, inner_class_name /*this.p.name*/);
						// Ignore the created compilation unit.
						// It is intended that the parser will include the inner class 
						// into the given outer class. 
						p.parse(typeDecl, outer_class_info, classPath);
						is.close();
					}
					else {
						System.out.println("Error: ClassFile " + inner_class_name + " not found");
					}
				} catch (FileNotFoundException e) {
					System.out.println("Error: " + inner_class_name + " not found");
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
				printIfVerbose("End processing: " + inner_class_name);
			}

		}
		printIfVerbose("    end");
	}
	
	private void exceptions() {
		int number_of_exceptions = this.p.u2();
		printIfVerbose("      " + number_of_exceptions + " exceptions:");
		for (int i = 0; i < number_of_exceptions; i++) {
			CONSTANT_Class_Info exception = this.p.getCONSTANT_Class_Info(this.p.u2());
			printIfVerbose("        exception " + exception.name());
			exceptionList.add(exception.access());
		}
	}
	
	private void constantValues() {
		int constantvalue_index = this.p.u2();
		constantValue = this.p.getCONSTANT_Info(constantvalue_index);
	}
	
	public List exceptionList() {
		return exceptionList;
	}
	
	public CONSTANT_Info constantValue() {
		return constantValue;
	}
	
	public boolean isSynthetic() {
		return isSynthetic;
	}

	public boolean isCjClass() {
		return isCjClass;
	}
	
	TypeDecl getOuterTypeDecl() {
		return outerTypeDecl;
	}
	

	private void printIfVerbose(String message) {
	      if(Parser.VERBOSE)
	          p.println(message);
	}

}
