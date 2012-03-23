package org.caesarj.runtime.constructors;

import java.util.List;
import java.util.Map;

import org.caesarj.runtime.CjObjectIfc;
import org.objectweb.asm.Type;

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
public interface APosterioriConstructorAnalysisResult extends
		SimpleAPosterioriConstructorAnalysisResult {

	/**
	 * @return the actual parameter types of the calling constructor which match
	 *         the main PPC (parameter pattern of the calling constructor)
	 *         according to the considered called constructor<br>
	 *         This list does NOT contain the type {@link CjObjectIfc} as first
	 *         type.
	 * @see #getParameterNames()
	 */
	List<Type> getParameterTypes();

	/**
	 * @return a map which maps PPCs (sub-patterns of the calling constructor
	 *         parameter pattern) to their (first) variable indexes
	 */
	Map<ParameterPattern, Integer> getPatternToLocalNumMap();

	/**
	 * @return a map which maps PPCs (parameter patterns of the calling
	 *         constructor) to their successor PPCs
	 */
	Map<ParameterPattern, ParameterPattern> getPatternToPatternLinks();

	/**
	 * @return a map which maps list indexes such as used in the PPcs (patterns
	 *         of the called constructor) to the list PPCs (patterns of the
	 *         calling constructor)
	 */
	Map<Integer, ParameterListPattern> getIndexToListPatternMap();

	/**
	 * @return the next free index for variables after considering all argument
	 *         variables according to the pattern of the calling constructor
	 */
	int getNextVariableIndex();

	/**
	 * @return the difference to be added to the index of new variables (i.e.,
	 *         the number of variable slots which have dynamically been added)
	 */
	int getNewVariableIndexShift();

	/**
	 * @return a map which maps old variable indexes to new ones
	 */
	Map<Integer, Integer> getVariableIndexMap();

}