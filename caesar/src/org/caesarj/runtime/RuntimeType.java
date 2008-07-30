package org.caesarj.runtime;

public abstract class RuntimeType {
	
	abstract public Class<?> ifcClass();
	
	abstract public boolean isInstance(Object obj);
	
	abstract public boolean isAssignableFrom(Class<?> cls);
	
	abstract public boolean isAssignableFrom(RuntimeType type);
}
