package org.caesarj.runtime.constructors;

/**
 * A parameter pattern which contains a list of component patterns. These have
 * to match the concrete superclass constructor in the given order.
 * 
 * @author Marko Martin
 */
public class ParameterPatternList extends CompositeParameterPattern {

	@Override
	public void accept(ParameterPatternVisitor visitor) {
		visitor.visit(this);
	}

}
