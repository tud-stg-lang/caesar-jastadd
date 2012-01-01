package org.caesarj.runtime.constructors;

import org.caesarj.util.ClassAccess;
import org.objectweb.asm.Type;

/**
 * Parameter pattern which references exactly one concrete parameter by type and
 * optionally also by name
 * <p>
 * Equality of concrete parameters is defined based only on the type names.
 * 
 * @author Marko Martin
 */
public class ConcreteParameter implements ParameterPattern {

	private String name;

	private String typeName;

	public ConcreteParameter(String typeName) {
		setTypeName(typeName);
	}

	public ConcreteParameter(String name, String typeName) {
		this(typeName);
		this.name = name;
	}

	public ConcreteParameter() {
	}

	/**
	 * @return the name of the referenced parameter, or null if no name is used
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name != null && name.isEmpty())
			this.name = null;
		else
			this.name = name;
	}

	/**
	 * @return the type of the referenced parameter
	 */
	public String getTypeName() {
		return typeName;
	}

	/**
	 * @param typeSignature
	 * @throws IllegalArgumentException
	 *             if typeSignature is null
	 */
	public void setTypeName(String typeSignature)
			throws IllegalArgumentException {
		if (typeSignature == null)
			throw new IllegalArgumentException(
					"typeSignature must not be null.");
		this.typeName = typeSignature;
	}

	@Override
	public void accept(ParameterPatternVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((typeName == null) ? 0 : typeName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConcreteParameter other = (ConcreteParameter) obj;
		if (typeName == null) {
			if (other.typeName != null)
				return false;
		} else if (!typeName.equals(other.typeName))
			return false;
		return true;
	}

	/**
	 * @param classLoader
	 * @return the type of the parameter, or null if it cannot be obtained
	 */
	public Type getType(ClassLoader classLoader) {
		try {
			return Type.getType(ClassAccess.forName(getTypeName(), false,
					classLoader));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

}
