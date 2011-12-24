package org.caesarj.runtime.constructors;

/**
 * Parameter pattern which references exactly one concrete parameter by type and
 * optionally also by name
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

}
