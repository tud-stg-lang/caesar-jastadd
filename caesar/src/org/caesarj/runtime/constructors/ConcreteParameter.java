package org.caesarj.runtime.constructors;

/**
 * Parameter pattern which references exactly one concrete parameter by type and
 * optionally also by name
 * <p>
 * Equality of concrete parameters is defined based only on the type signature.
 * 
 * @author Marko Martin
 */
public class ConcreteParameter implements ParameterPattern {

	private String name;

	private String typeSignature;

	public ConcreteParameter(String typeSignature) {
		setTypeSignature(typeSignature);
	}

	public ConcreteParameter(String name, String typeSignature) {
		this(typeSignature);
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
	public String getTypeSignature() {
		return typeSignature;
	}

	/**
	 * @param typeSignature
	 * @throws IllegalArgumentException
	 *             if typeSignature is null
	 */
	public void setTypeSignature(String typeSignature)
			throws IllegalArgumentException {
		if (typeSignature == null)
			throw new IllegalArgumentException(
					"typeSignature must not be null.");
		this.typeSignature = typeSignature;
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
				+ ((typeSignature == null) ? 0 : typeSignature.hashCode());
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
		if (typeSignature == null) {
			if (other.typeSignature != null)
				return false;
		} else if (!typeSignature.equals(other.typeSignature))
			return false;
		return true;
	}

}
