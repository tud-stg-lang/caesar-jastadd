package org.caesarj.runtime;

public class RuntimeJavaClass extends RuntimeClass {
	
	protected Class<?> javaClass;
	
	public RuntimeJavaClass(Class<?> javaClass) {
		this.javaClass = javaClass;
	}
	
	public Class<?> ifcClass() {
		return javaClass;
	}
	
	public Class<?> implClass() {
		return javaClass;
	}
	
	public String getName() {
		return javaClass.getName();
	}
	
	public boolean isInstance(Object obj) {
		return javaClass.isInstance(obj);
	}
	
	public boolean isAssignableFrom(RuntimeType cls) {
		return javaClass.isAssignableFrom(cls.ifcClass());
	}
	
	public boolean isAssignableFrom(Class<?> cls) {
		return javaClass.isAssignableFrom(cls);
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof RuntimeJavaClass)) {
			return false;
		}
		RuntimeJavaClass otherCls = (RuntimeJavaClass)other;
		return (javaClass.equals(otherCls.javaClass)); 
	}
	
	public int hashCode() {
		return javaClass.hashCode();
	}
}
