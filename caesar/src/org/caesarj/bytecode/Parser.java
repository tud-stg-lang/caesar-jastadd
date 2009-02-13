package org.caesarj.bytecode;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.caesarj.ast.ASTNode;
import org.caesarj.ast.Access;
import org.caesarj.ast.BodyDecl;
import org.caesarj.ast.CjContractClassDecl;
import org.caesarj.ast.CjPrivInhClassDecl;
import org.caesarj.ast.ClassDecl;
import org.caesarj.ast.CompilationUnit;
import org.caesarj.ast.Dot;
import org.caesarj.ast.InterfaceDecl;
import org.caesarj.ast.List;
import org.caesarj.ast.MemberClassDecl;
import org.caesarj.ast.MemberInterfaceDecl;
import org.caesarj.ast.Modifier;
import org.caesarj.ast.Modifiers;
import org.caesarj.ast.Opt;
import org.caesarj.ast.ParseName;
import org.caesarj.ast.Program;
import org.caesarj.ast.TypeDecl;
import org.objectweb.asm.ClassReader;

//import sun.management.MethodInfo;
//import sun.reflect.FieldInfo;


public class Parser {
	public static final boolean VERBOSE = false;

	private DataInputStream is;
	public CONSTANT_Class_Info classInfo;
	public CONSTANT_Class_Info outerClassInfo;
	public String name;

	public CONSTANT_Info[] constantPool = null;

	/** "Trickle-up": This field is used for passing the outer <code>TypeDecl</code>, to which a 
	 *  <code>MemberClassDecl</code> (corresponding to the inner class) was added, along the cascade 
	 *  of <code title="org.caesarj.bytecode.Attributes">Attributes</code> and 
	 *  <code title="org.caesarj.bytecode.Parser">Parser</code> instances. */
	private TypeDecl outerTypeDecl;

	public Parser(byte[] buffer, int size, String name) {
		//this.is = new DataInputStream(new DummyInputStream(buffer, size));
		this.is = new DataInputStream(new ByteArrayInputStream(buffer, 0, size));
		this.name = name;
	}
	public Parser(InputStream in, String name) {
		//this.is = new DataInputStream(new DummyInputStream(buffer, size));
		this.is = new DataInputStream(new DummyInputStream(in));
		this.name = name;
	}

	public Parser(String name) throws FileNotFoundException {
		if (!name.endsWith(".class") && !name.endsWith(".cjclass") ) {
			//name = name.replaceAll("\\.", "/") + ".class";
			name = name.replace('.', '/') + ".class";
		}
		this.name = name;
		this.is = new DataInputStream(new FileInputStream(name));
	}

	private static class DummyInputStream extends InputStream {
		byte[] bytes;
		int pos;
		int size;
		public DummyInputStream(byte[] buffer, int size) {
			bytes = buffer;
			this.size = size;
		}
		public DummyInputStream(InputStream is) {
			bytes = new byte[1024];
			int index = 0;
			size = 1024;
			try {
				int status;
				do {
					status = is.read(bytes, index, size - index);
					if(status != -1) {
						index += status;
						if(index == size) {
							byte[] newBytes = new byte[size*2];
							System.arraycopy(bytes, 0, newBytes, 0, size);
							bytes = newBytes;
							size *= 2;
						}
					}
				} while (status != -1);
			} catch (IOException e) {
				System.err.println("Something went wrong trying to read " + is);
				//System.exit(1);
			}
			size = index;
			pos = 0;
		}

		public int available() {
			return size - pos;
		}

		public void close() {
		}

		public void mark(int readlimit) {
		}

		public boolean markSupported() {
			return false;
		}

		public int read(byte[] b) {
			int actualLength = Math.min(b.length, size-pos);
			System.arraycopy(bytes, pos, b, 0, actualLength);
			pos += actualLength;
			return actualLength;
		}

		public int read(byte[] b, int offset, int length) {
			int actualLength = Math.min(length, size-pos);
			System.arraycopy(bytes, pos, b, offset, actualLength);
			pos += actualLength;
			return actualLength;
		}

		public void reset() {
		}

		public long skip(long n) {
			if(size == pos)
				return -1;
			long skipSize = Math.min(n, size-pos);
			pos += skipSize;
			return skipSize;
		}

		public int read() throws IOException {
			if(pos < size) {
				int i = bytes[pos++];
				if(i < 0)
					i = 256 + i;
				return i;
			}
			return -1;
		}
	}

	public int next() {
		try {
			return is.read();
		} catch (IOException e) {
			System.exit(1);
		}
		return -1;
	}

	public int u1() {
		try {
			return is.readUnsignedByte();
		} catch (IOException e) {
			System.exit(1);
		}
		return -1;
	}

	public int u2() {
		try {
			return is.readUnsignedShort();
		} catch (IOException e) {
			System.exit(1);
		}
		return -1;
	}

	public int u4() {
		try {
			return is.readInt();
		} catch (IOException e) {
			System.exit(1);
		}
		return -1;
	}

	public int readInt() {
		try {
			return is.readInt();
		} catch (IOException e) {
			System.exit(1);
		}
		return -1;
	}

	public float readFloat() {
		try {
			return is.readFloat();
		} catch (IOException e) {
			System.exit(1);
		}
		return -1;
	}

	public long readLong() {
		try {
			return is.readLong();
		} catch (IOException e) {
			System.exit(1);
		}
		return -1;
	}

	public double readDouble() {
		try {
			return is.readDouble();
		} catch (IOException e) {
			System.exit(1);
		}
		return -1;
	}

	public String readUTF() {
		try {
			return is.readUTF();
		} catch (IOException e) {
			System.exit(1);
		}
		return "";
	}

	public void skip(int length) {
		try {
			is.skip(length);
		} catch (IOException e) {
			System.exit(1);
		}
	}

	public void error(String s) {
		throw new RuntimeException(s);
	}

	public void print(String s) {
		System.out.print(s);
	}

	public void println(String s) {
		print(s + "\n");
	}

	public void println() {
		print("\n");
	}

	public CompilationUnit parse(TypeDecl outerTypeDeclParam, CONSTANT_Class_Info outerClassInfo, Program classPath) 
	throws FileNotFoundException, IOException {
		if(Parser.VERBOSE) 
			println("Parsing byte codes in " + name);

		this.outerClassInfo = outerClassInfo;

		// Read blocks of Java class file format:
		parseMagic();
		parseMinor();
		parseMajor();
		parseConstantPool();
		TypeDecl typeDecl = parseTypeDecl();
		parseFields(typeDecl);
		parseMethods(typeDecl);
		Attributes attrs = new Attributes(this, typeDecl, outerTypeDeclParam, classPath);

		// If the type is a CaesarJ class in a ClassDecl hull, make it a CjVirtualClassDecl and add inherited members:
		List superclassesList = attrs.superclassesList();
		if(attrs.isCjClass() || superclassesList != null) {
			typeDecl = transformBodyAndSuperclasses((ClassDecl)typeDecl, superclassesList);
		}
		CompilationUnit cu = new CompilationUnit();
		cu.setPackageDecl(classInfo.packageDecl());

		// check if not already added as a inner class of another class
		if (typeDecl.getParent() == null) {
			cu.addTypeDecl(typeDecl);
		}

		is.close();
		is = null;

		return cu;
	}

	/** Transforms a TypeDecl into a CjContractClassDecl */
	//TODO: Make sure to always use the most concrete CjClassDecl derivate
	CjContractClassDecl transformBodyAndSuperclasses(ClassDecl typeDecl, List superclassesList) {
		List body = typeDecl.getBodyDeclListNoTransform();
		List newbody = new List();
		for(int i = 0; i < body.getNumChild(); ++i) {
			BodyDecl decl = (BodyDecl)body.getChildNoTransform(i);
			if(decl instanceof MemberInterfaceDecl) {
				// Ignore implicit interface
				if(((MemberInterfaceDecl)decl).getInterfaceDeclNoTransform().getID().equals("ccIfc"))
					continue;
			}
			newbody.add(decl);
		}

		if (superclassesList == null)
			superclassesList = new List(); //ignore typeDecl.getSuperClassAccess()

		// Make a CjContractClassDecl, including its superclasses and its inherited members:
		CjPrivInhClassDecl transformedTypeDecl =
				new CjPrivInhClassDecl(
						typeDecl.getModifiersNoTransform(),
						typeDecl.getID(),
						superclassesList,
						typeDecl.getImplementsListNoTransform(),
						newbody,
						new List(),
						new List());
		transformedTypeDecl.setDynamicTypeList(
				typeDecl.getDynamicTypeListNoTransform());

		// For virtual classes replace them in the parent
		ASTNode parent = typeDecl.getParent();
		if (parent != null) {
			if (parent instanceof MemberClassDecl) {
				((MemberClassDecl)parent).setClassDecl(transformedTypeDecl);			
			}
		}
		
		return transformedTypeDecl;
	}

	public void parseMagic() {
		if (next() != 0xca || next() != 0xfe || next() != 0xba || next() != 0xbe)
			error("magic error");
	}

	public void parseMinor() {
		int low = u1();
		int high = u1();
		if(Parser.VERBOSE) 
			println("Minor: " + high + "." + low);
	}

	public void parseMajor() {
		int low = u1();
		int high = u1();
		if(Parser.VERBOSE) 
			println("Major: " + high + "." + low);
	}

	public static boolean isInnerClass = false;

	public TypeDecl parseTypeDecl() {
		isInnerClass = false;

		int flags = u2();
		Modifiers modifiers = modifiers(flags & 0xfddf);
		if ((flags & 0x0200) == 0) {

			ClassDecl decl = new ClassDecl();
			decl.setModifiers(modifiers);
			decl.setID(parseThisClass());
			Access superClass = parseSuperClass();
			decl.setSuperClassAccessOpt(superClass == null ? new Opt()
			: new Opt(superClass));
			decl.setImplementsList(parseInterfaces(new List()));


			if ((flags & 0x0008) == 0 && outerClassInfo != null)
				isInnerClass = true;
			return decl;
		} else {
			InterfaceDecl decl = new InterfaceDecl();
			decl.setModifiers(modifiers);
			decl.setID(parseThisClass());
			Access superClass = parseSuperClass();
			decl.setSuperInterfaceIdList(parseInterfaces(superClass == null ? new List()
			: new List().add(superClass)));
			return decl;
		}
	}


	public String parseThisClass() {
		int index = u2();
		CONSTANT_Class_Info info = (CONSTANT_Class_Info) constantPool[index];
		classInfo = info;
		return info.simpleName();
	}

	public Access parseSuperClass() {
		int index = u2();
		if (index == 0)
			return null;
		CONSTANT_Class_Info info = (CONSTANT_Class_Info) constantPool[index];
		return info.access();
	}

	public List parseInterfaces(List list) {
		int count = u2();
		for (int i = 0; i < count; i++) {
			CONSTANT_Class_Info info = (CONSTANT_Class_Info) constantPool[u2()];
			replaceInterfaces(info.name(), list);
		}
		return list;
	}
	
	protected void replaceInterfaces(String ifcTypeName, List list) {
		if (ifcTypeName.endsWith("_ccIfc")) {
			try {
				final String ifcFilePath = ifcTypeName.replace('.', '/');
				ClassReader cr = new ClassReader(ClassLoader
						.getSystemResourceAsStream(ifcFilePath + ".class"));
				for (String ifc : cr.getInterfaces())
					if (!ifc.equals("org/caesarj/runtime/CjObjectIfc"))
						replaceInterfaces(ifc.replace('/', '.'), list);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else
			list.add(access(ifcTypeName));
	}
	
	protected Access access(String ifc) {
		String name = ifc.replace('/', '.').replace('$', '.');
		int index = -1;
		int pos = 0;
		Access result = null;
		do {
			pos = name.indexOf('.', index + 1);
			if (pos == -1)
				pos = name.length();
			String s = name.substring(index + 1, pos);
			if (index == -1) {
				result = new ParseName(s);
			} else {
				result = new Dot(result, new ParseName(s));
			}
			index = pos;
		} while (pos != name.length());
		return result;
	}


	public Access fromClassName(String s) {
		//s = s.replaceAll("\\$", "/");
		s = s.replace('$', '/');

		int index = -1;
		int pos = 0;
		Access result = null;
		do {
			pos = s.indexOf('/', index+1);
			if(pos == -1)
				pos = s.length();
			String name = s.substring(index+1, pos);
			if(index == -1) {
				result = new ParseName(name);
			}
			else {
				result = new Dot(result, new ParseName(name));
			}
			index = pos;
		} while(pos != s.length());
		return result;

	}

	public static Modifiers modifiers(int flags) {
		Modifiers m = new Modifiers();
		if ((flags & 0x0001) != 0)
			m.addModifier(new Modifier("public"));
		if ((flags & 0x0002) != 0)
			m.addModifier(new Modifier("private"));
		if ((flags & 0x0004) != 0)
			m.addModifier(new Modifier("protected"));
		if ((flags & 0x0008) != 0)
			m.addModifier(new Modifier("static"));
		if ((flags & 0x0010) != 0)
			m.addModifier(new Modifier("final"));
		if ((flags & 0x0020) != 0)
			m.addModifier(new Modifier("synchronized"));
		if ((flags & 0x0040) != 0)
			m.addModifier(new Modifier("volatile"));
		if ((flags & 0x0080) != 0)
			m.addModifier(new Modifier("transient"));
		if ((flags & 0x0100) != 0)
			m.addModifier(new Modifier("native"));
		if ((flags & 0x0400) != 0)
			m.addModifier(new Modifier("abstract"));
		if ((flags & 0x0800) != 0)
			m.addModifier(new Modifier("strictfp"));
		return m;
	}

	public void parseFields(TypeDecl typeDecl) {
		int count = u2();
		if(Parser.VERBOSE) 
			println("Fields (" + count + "):");
		for (int i = 0; i < count; i++) {
			if(Parser.VERBOSE) 
				print(" Field nbr " + i + " ");
			FieldInfo fieldInfo = new FieldInfo(this);
			if(!fieldInfo.isSynthetic())
				typeDecl.addBodyDecl(fieldInfo.bodyDecl());
		}
	}


	public void parseMethods(TypeDecl typeDecl) {
		int count = u2();
		if(Parser.VERBOSE) 
			println("Methods (" + count + "):");
		for (int i = 0; i < count; i++) {
			if(Parser.VERBOSE) 
				print("  Method nbr " + i + " ");
			MethodInfo info = new MethodInfo(this);
			if(!info.isSynthetic() && !info.name.equals("<clinit>")) {
				typeDecl.addBodyDecl(info.bodyDecl());
			}
		}
	}


	private void checkLengthAndNull(int index) {
		if(index >= constantPool.length) {
			throw new Error("Trying to access element " + index  + " in constant pool of length " + constantPool.length);
		}
		if(constantPool[index] == null)
			throw new Error("Unexpected null element in constant pool at index " + index);
	}
	public boolean validConstantPoolIndex(int index) {
		return index < constantPool.length && constantPool[index] != null;
	}
	public CONSTANT_Info getCONSTANT_Info(int index) {
		checkLengthAndNull(index);
		return constantPool[index];
	}
	public CONSTANT_Utf8_Info getCONSTANT_Utf8_Info(int index) {
		checkLengthAndNull(index);
		CONSTANT_Info info = constantPool[index];
		if(!(info instanceof CONSTANT_Utf8_Info))
			throw new Error("Expected CONSTANT_Utf8_info at " + index + " in constant pool but found " + info.getClass().getName());
		return (CONSTANT_Utf8_Info)info;
	}
	public CONSTANT_Class_Info getCONSTANT_Class_Info(int index) {
		checkLengthAndNull(index);
		CONSTANT_Info info = constantPool[index];
		if(!(info instanceof CONSTANT_Class_Info))
			throw new Error("Expected CONSTANT_Class_info at " + index + " in constant pool but found " + info.getClass().getName());
		return (CONSTANT_Class_Info)info;
	}

	public void parseConstantPool() {
		int count = u2();
		if(Parser.VERBOSE) 
			println("constant_pool_count: " + count);
		constantPool = new CONSTANT_Info[count + 1];
		for (int i = 1; i < count; i++) {
			parseEntry(i);
			if (constantPool[i] instanceof CONSTANT_Long_Info
					|| constantPool[i] instanceof CONSTANT_Double_Info)
				i++;
		}

		//println("ConstantPool: ");
		//for(int i = 1; i < count; i++) {
		//  println(i + ", " + constantPool[i]);
		//}

	}

	private static final int CONSTANT_Class = 7;
	private static final int CONSTANT_FieldRef = 9;
	private static final int CONSTANT_MethodRef = 10;
	private static final int CONSTANT_InterfaceMethodRef = 11;
	private static final int CONSTANT_String = 8;
	private static final int CONSTANT_Integer = 3;
	private static final int CONSTANT_Float = 4;
	private static final int CONSTANT_Long = 5;
	private static final int CONSTANT_Double = 6;
	private static final int CONSTANT_NameAndType = 12;
	private static final int CONSTANT_Utf8 = 1;

	public void parseEntry(int i) {
		int tag = u1();
		switch (tag) {
		case CONSTANT_Class:
			constantPool[i] = new CONSTANT_Class_Info(this);
			break;
		case CONSTANT_FieldRef:
			constantPool[i] = new CONSTANT_Fieldref_Info(this);
			break;
		case CONSTANT_MethodRef:
			constantPool[i] = new CONSTANT_Methodref_Info(this);
			break;
		case CONSTANT_InterfaceMethodRef:
			constantPool[i] = new CONSTANT_InterfaceMethodref_Info(this);
			break;
		case CONSTANT_String:
			constantPool[i] = new CONSTANT_String_Info(this);
			break;
		case CONSTANT_Integer:
			constantPool[i] = new CONSTANT_Integer_Info(this);
			break;
		case CONSTANT_Float:
			constantPool[i] = new CONSTANT_Float_Info(this);
			break;
		case CONSTANT_Long:
			constantPool[i] = new CONSTANT_Long_Info(this);
			break;
		case CONSTANT_Double:
			constantPool[i] = new CONSTANT_Double_Info(this);
			break;
		case CONSTANT_NameAndType:
			constantPool[i] = new CONSTANT_NameAndType_Info(this);
			break;
		case CONSTANT_Utf8:
			constantPool[i] = new CONSTANT_Utf8_Info(this);
			break;
		default:
			println("Unknown entry: " + tag);
		}
	}

	public TypeDecl getOuterTypeDecl() {
		return this.outerTypeDecl;
	}

}
