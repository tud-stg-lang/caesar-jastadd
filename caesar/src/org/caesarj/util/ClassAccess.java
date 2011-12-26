package org.caesarj.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassAccess {

	private static final Map<String, Class<?>> BUILT_IN_MAP = new ConcurrentHashMap<String, Class<?>>();

	private static final Map<Class<?>, Class<?>> PRIMITIVES_ASSIGNABLE_TO = new ConcurrentHashMap<Class<?>, Class<?>>();

	private static final Map<Class<?>, Class<?>> PRIMITIVES_UNBOXED_TYPES = new ConcurrentHashMap<Class<?>, Class<?>>();

	static {
		for (Class<?> c : new Class[] { void.class, boolean.class, byte.class,
				char.class, short.class, int.class, float.class, double.class,
				long.class })
			BUILT_IN_MAP.put(c.getName(), c);
		PRIMITIVES_ASSIGNABLE_TO.put(float.class, double.class);
		PRIMITIVES_ASSIGNABLE_TO.put(long.class, float.class);
		PRIMITIVES_ASSIGNABLE_TO.put(int.class, long.class);
		PRIMITIVES_ASSIGNABLE_TO.put(short.class, int.class);
		PRIMITIVES_ASSIGNABLE_TO.put(char.class, short.class);
		PRIMITIVES_ASSIGNABLE_TO.put(byte.class, short.class);
		PRIMITIVES_UNBOXED_TYPES.put(Boolean.class, boolean.class);
		PRIMITIVES_UNBOXED_TYPES.put(Byte.class, byte.class);
		PRIMITIVES_UNBOXED_TYPES.put(Character.class, char.class);
		PRIMITIVES_UNBOXED_TYPES.put(Short.class, short.class);
		PRIMITIVES_UNBOXED_TYPES.put(Integer.class, int.class);
		PRIMITIVES_UNBOXED_TYPES.put(Long.class, long.class);
		PRIMITIVES_UNBOXED_TYPES.put(Float.class, float.class);
		PRIMITIVES_UNBOXED_TYPES.put(Double.class, double.class);
	}

	public static Class<?> forName(String name, boolean initialize,
			ClassLoader loader) throws ClassNotFoundException {
		Class<?> c = BUILT_IN_MAP.get(name);
		if (c == null)
			return Class.forName(name, initialize, loader);
		return c;
	}

	/**
	 * @param targetTypeName
	 * @param sourceTypeName
	 * @return true if an instance of the class with name sourceTypeName can be
	 *         assigned to a variable of type targetTypeName<br>
	 *         This method respects primitive types and box types.
	 */
	public static boolean isAssignableFrom(String targetTypeName,
			String sourceTypeName, ClassLoader classLoader) {
		if (targetTypeName.equals(sourceTypeName))
			return true;
		final Class<?> targetClass;
		final Class<?> sourceClass;
		try {
			targetClass = forName(targetTypeName, false, classLoader);
			sourceClass = forName(sourceTypeName, false, classLoader);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		return isAssignableFrom(targetClass, sourceClass);
	}

	/**
	 * @param targetClass
	 * @param sourceClass
	 * @return true if an instance of sourceClass can be assigned to a variable
	 *         of type targetClass<br>
	 *         This method respects primitive types and box types.
	 */
	public static boolean isAssignableFrom(Class<?> targetClass,
			Class<?> sourceClass) {
		Class<?> unboxedType = PRIMITIVES_UNBOXED_TYPES.get(targetClass);
		if (unboxedType != null)
			targetClass = unboxedType;
		unboxedType = PRIMITIVES_UNBOXED_TYPES.get(sourceClass);
		if (unboxedType != null)
			sourceClass = unboxedType;
		Class<?> sourceClassAssignableTo = sourceClass;
		while (true) {
			sourceClassAssignableTo = PRIMITIVES_ASSIGNABLE_TO
					.get(sourceClassAssignableTo);
			if (sourceClassAssignableTo == null)
				break;
			if (sourceClassAssignableTo.equals(targetClass))
				return true;
		}
		return targetClass.isAssignableFrom(sourceClass);
	}

}
