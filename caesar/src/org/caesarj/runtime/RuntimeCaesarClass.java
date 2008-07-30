package org.caesarj.runtime;

public class RuntimeCaesarClass extends RuntimeClass {
	protected Class<?> ifcClass;
	protected Class<?> implClass;
	
	public RuntimeCaesarClass(Class<?> implClass, Class<?> ifcClass) {
		this.implClass = implClass;
		this.ifcClass = ifcClass;
	}
	
	public String getName() {
		return implClass.getName();
	}
		
	public boolean isInstance(Object obj) {
		return ifcClass.isInstance(obj);
	}
	
	public boolean isAssignableFrom(RuntimeType cls) {
		return ifcClass.isAssignableFrom(cls.ifcClass());
	}
	
	public boolean isAssignableFrom(Class<?> cls) {
		return ifcClass.isAssignableFrom(cls);
	}
	
	public Class<?> ifcClass() {
		return ifcClass;
	}
	
	public Class<?> implClass() {
		return implClass;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof RuntimeCaesarClass)) {
			return false;
		}
		RuntimeCaesarClass otherCls = (RuntimeCaesarClass)other;
		return (implClass.equals(otherCls.implClass)); 
	}
	
	public int hashCode() {
		return implClass.hashCode();
	}
	
	public String shortName() {
		String fullName = getName();
		int pos = Math.max(fullName.lastIndexOf('$'), fullName.lastIndexOf('.'));
		return fullName.substring(pos + 1);
	}
		
	public String enclosingClassName() throws RuntimeTypeException {
		String fullName = getName();
		int splitPos = fullName.lastIndexOf('$');
		if (splitPos == -1) {
			throw new RuntimeTypeException("Class " + fullName + " is not a nested class");
		}
		return fullName.substring(0, splitPos);		
	}
}
