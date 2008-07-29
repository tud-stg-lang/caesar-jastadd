package org.caesarj.runtime;

public abstract class RuntimeClass extends RuntimeType {
	
	public abstract String getName();
	
	public abstract Class<?> implClass();
	
	/*******************************************************************************
	 *   Static functionality 
	 */
	
	private static RuntimeCaesarClass cjObjectClass = null;
	
	public static RuntimeCaesarClass cjObjectClass() {
		if (cjObjectClass == null) {
			cjObjectClass = new RuntimeCjObjectClass();
		}
		return cjObjectClass;
	}
	
	public static String toCaesarInterfaceName(String className) {
		return className + "_ccIfc";
	}
	
	public static RuntimeClass forName(String className) throws RuntimeTypeException {
		try {
			return forClass(Class.forName(className));
		}
		catch (Exception e) {
			throw new RuntimeTypeException(e.getMessage());
		}
	}
	
	public static RuntimeClass forClass(Class<?> javaClass) throws RuntimeTypeException {
		try {
			if (cjObjectClass().implClass().isAssignableFrom(javaClass)) {
				if (javaClass.equals(cjObjectClass().implClass())) {
					return cjObjectClass();
				}
				else {
					String ifcName = toCaesarInterfaceName(javaClass.getName());
					Class<?> ifcClass = Class.forName(ifcName);
					return new RuntimeCaesarClass(javaClass, ifcClass);
				}
			}
			else {
				return new RuntimeJavaClass(javaClass);
			}
		}
		catch (Exception e) {
			throw new RuntimeTypeException(e.getMessage());
		}
	}
	
	public static RuntimeClass forClassUnsafe(Class<?> javaClass) {
		try {
			if (cjObjectClass().implClass().isAssignableFrom(javaClass)) {
				if (javaClass.equals(cjObjectClass().implClass())) {
					return cjObjectClass();
				}
				else {
					String ifcName = toCaesarInterfaceName(javaClass.getName());
					Class<?> ifcClass = Class.forName(ifcName);
					return new RuntimeCaesarClass(javaClass, ifcClass);
				}
			}
			else {
				return new RuntimeJavaClass(javaClass);
			}
		}
		catch (Exception e) {
			throw new RuntimeInconsistencyError(e.getMessage());
		}
	}
}
