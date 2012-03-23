package org.caesarj.runtime.constructors;

import java.util.List;

import org.caesarj.runtime.CjObjectIfc;

/**
 * Interface for a result of a constructor analysis which contains results
 * dependent on the concrete called constructor.
 * <p>
 * Some of the methods refer to {@link ParameterPattern}s. If they are parts of
 * the calling constructor, the abbreviation PPC is used in the method
 * description; if they are parts of the called constructor, the abbreviation
 * PPc. is used.
 * 
 * @author Marko Martin
 */
public interface SimpleAPosterioriConstructorAnalysisResult {

	/**
	 * @return the actual parameters of the calling constructor which match the
	 *         main PPC (parameter pattern of the calling constructor) according
	 *         to the considered called constructor<br>
	 *         This list does NOT contain the type {@link CjObjectIfc} as first
	 *         type.
	 */
	List<ConcreteParameter> getConcreteParameters();

}