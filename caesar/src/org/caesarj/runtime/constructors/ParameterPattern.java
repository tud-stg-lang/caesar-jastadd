package org.caesarj.runtime.constructors;

/**
 * A pattern which is applied within a call to a superclass constructor
 * <p>
 * TODO further information
 * 
 * @author Marko Martin
 */
public interface ParameterPattern {

	void accept(ParameterPatternVisitor visitor);

}
