package org.caesarj.runtime.constructors;

import java.util.Map;

import org.caesarj.ast.TypeDecl;
import org.caesarj.util.ClassAccess;

/**
 * Interface for strategy which checks if a certain type is subtype of a given
 * supertype.
 * 
 * @author Marko Martin
 */
public interface SubtypeChecker {

	public static class ClassAccessSubtypeChecker implements SubtypeChecker {

		private final ClassLoader classLoader;

		public ClassAccessSubtypeChecker(ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		@Override
		public boolean isSubtype(String supertype, String subtype) {
			return ClassAccess
					.isAssignableFrom(supertype, subtype, classLoader);
		}

	}

	public static class TypeDeclSuptypeChecker implements
			org.caesarj.runtime.constructors.SubtypeChecker {

		private final Map<String, TypeDecl> typeNameToDeclMap;

		private final StringBuilder errors = new StringBuilder();

		public TypeDeclSuptypeChecker(Map<String, TypeDecl> typeNameToDeclMap) {
			this.typeNameToDeclMap = typeNameToDeclMap;
		}

		@Override
		public boolean isSubtype(final String supertype, final String subtype) {
			final TypeDecl supertypeDecl = typeNameToDeclMap.get(supertype);
			final TypeDecl subtypeDecl = typeNameToDeclMap.get(subtype);
			if (supertypeDecl != null && subtypeDecl != null)
				return subtypeDecl.subtype(supertypeDecl);
			if (errors.length() > 0)
				errors.append(' ');
			errors.append("Cannot check whether \"").append(subtype)
					.append("\" is subtype of \"").append(supertype)
					.append("\".");
			return false;
		}

		/**
		 * @return error messages if error occurred during subtype checking,
		 *         null else
		 */
		public String getErrors() {
			if (errors.length() == 0)
				return null;
			return errors.toString();
		}
	}

	/**
	 * @param supertype
	 *            the class name of the potential supertype
	 * @param subtype
	 *            the class name of the potential subtype
	 * @return true if subtype is a subtype of supertype or if both are equal
	 */
	boolean isSubtype(String supertype, String subtype);

}
