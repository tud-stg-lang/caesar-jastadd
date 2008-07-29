package org.caesarj.runtime;

public class RuntimeFamilyType extends RuntimeType {
	protected CjObject family;
	protected String className;
	protected RuntimeCaesarClass virtClass;
	
	public RuntimeFamilyType(CjObject family, String className) throws RuntimeTypeException {
		this.family = family;
		this.className = className;
		RuntimeClass cls = RuntimeClass.forName(fullClassName());
		if (cls instanceof RuntimeCaesarClass) {
			this.virtClass = (RuntimeCaesarClass)cls;
		}
		else {
			throw new RuntimeTypeException(cls.getName() + " is not a virtual class");
		}
	}
	
	public RuntimeFamilyType(CjObject family, RuntimeCaesarClass virtClass) throws RuntimeTypeException {
		this.family = family;
		this.virtClass = virtClass;
		String familyClass = family.getClass().getName();
		if (!familyClass.equals(virtClass.enclosingClassName())) {
			throw new RuntimeTypeException(virtClass.getName() + " is not a virtual class of " + familyClass);					
		}
		this.className = virtClass.shortName();		
	}
	
	public RuntimeCaesarClass virtualClass() {
		return virtClass;
	}
	
	public Class<?> ifcClass() {
		return virtClass.ifcClass();
	}
	
	public CjObject family() {
		return family;
	}
	
	public String shortClassName() {
		return className;
	}
	
	public String fullClassName() {
		return family.getClass().getName() + "$" + className;
	}
	
	public boolean isAssignableFrom(RuntimeType type) {
		if (!(type instanceof RuntimeFamilyType)) {
			return false;
		}
		RuntimeFamilyType famType = (RuntimeFamilyType)type;
		return (family == famType.family && virtClass.isAssignableFrom(famType.virtClass));		
	}
	
	public boolean isInstance(Object o) {
		if (!virtClass.isInstance(o)) {
			return false;
		}
		else {
			return ((CjObject)o).cjFamily() == family;
		}
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof RuntimeFamilyType)) {
			return false;
		}
		RuntimeFamilyType famType = (RuntimeFamilyType)other;
		return (family == famType.family && className.equals(famType.className)); 
	}
	
	public int hashCode() {
		return family.hashCode() + className.hashCode();
	}
}
