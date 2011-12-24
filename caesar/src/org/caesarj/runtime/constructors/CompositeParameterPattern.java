package org.caesarj.runtime.constructors;

import java.util.ArrayList;
import java.util.List;

public abstract class CompositeParameterPattern implements ParameterPattern {

	private List<ParameterPattern> components = new ArrayList<ParameterPattern>();

	/**
	 * @return the components contained in this composite pattern, never null<br>
	 *         By default, this is a modifiable empty list.
	 */
	public List<ParameterPattern> getComponents() {
		return components;
	}

	/**
	 * @param components
	 * @throws IllegalArgumentException
	 *             if components is null
	 */
	public void setComponents(List<ParameterPattern> components)
			throws IllegalArgumentException {
		if (components == null)
			throw new IllegalArgumentException("components must not be null.");
		this.components = components;
	}

	@Override
	public void accept(ParameterPatternVisitor visitor) {
		visitor.visit(this);
	}

}
