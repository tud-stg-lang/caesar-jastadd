package org.caesarj.runtime.constructors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;

/**
 * Analyzes the parameter pattern (which will be visited) of a "calling"
 * constructor with respect to the results of the analysis of the parameter
 * pattern for the "called" constructor.
 * <p>
 * Each instance of this visitor must be used only once.
 * <p>
 * This class extends the
 * {@link SimpleCallingConstructorParameterPatternAnalyzer} with additional
 * analyses concerning the resulting code for loading arguments.
 * 
 * @author Marko Martin
 */
public class CallingConstructorParameterPatternAnalyzer extends
		SimpleCallingConstructorParameterPatternAnalyzer implements
		APosterioriConstructorAnalysisResult {

	private final ClassLoader classLoader;

	private ParameterPattern lastPattern = null;

	/**
	 * the next variable index to be assigned
	 */
	private int nextLocalNum;

	/**
	 * the index which is used for referencing the next variable in the loaded
	 * code<br>
	 * This will probably differ from {@link #nextLocalNum} in the process
	 * because list parameters are ignored in the loaded code, but not in the
	 * counter {@link #nextLocalNum}.
	 */
	private int nextLocalNumBeforeConversion;

	/**
	 * @see #getPatternToLocalNumMap()
	 */
	private final Map<ParameterPattern, Integer> patternToLocalNumMap = new HashMap<ParameterPattern, Integer>();

	/**
	 * @see #getPatternToPatternLinks()
	 */
	private final Map<ParameterPattern, ParameterPattern> patternToPatternLinks = new HashMap<ParameterPattern, ParameterPattern>();

	/**
	 * @see #getIndexToListPatternMap()
	 */
	private final Map<Integer, ParameterListPattern> indexToListPatternMap = new HashMap<Integer, ParameterListPattern>();

	/**
	 * @see #getParameterTypes()
	 */
	private final List<Type> parameterTypes = new ArrayList<Type>();

	/**
	 * @see #getVariableIndexMap()
	 */
	private final Map<Integer, Integer> variableIndexMap = new HashMap<Integer, Integer>();

	/**
	 * @param concreteParameters
	 *            concrete parameters of the called constructor
	 * @param calledPatternToParametersMap
	 *            a map of patterns to indexes of concrete parameters
	 * @param firstVariableIndex
	 *            the index to be assumed for the first concrete parameter
	 *            within the visited pattern
	 * @param classLoader
	 */
	public CallingConstructorParameterPatternAnalyzer(
			List<ConcreteParameter> concreteParameters,
			Map<ParameterPattern, List<Integer>> calledPatternToParametersMap,
			int firstVariableIndex, ClassLoader classLoader) {
		super(concreteParameters, calledPatternToParametersMap);
		nextLocalNum = firstVariableIndex;
		/*
		 * $cj$outer is NOT a parameter in the original constructor. Therefore,
		 * the first index is 1 lower:
		 */
		nextLocalNumBeforeConversion = firstVariableIndex - 1;
		this.classLoader = classLoader;
	}

	@Override
	public void visit(ConcreteParameter parameter) {
		linkPattern(parameter);
		patternToLocalNumMap.put(parameter, nextLocalNum);
		variableIndexMap.put(nextLocalNumBeforeConversion, nextLocalNum);
		nextLocalNumBeforeConversion += parameter.getType(classLoader)
				.getSize();
		super.visit(parameter);
	}

	private void linkPattern(ParameterPattern pattern) {
		if (lastPattern != null)
			patternToPatternLinks.put(lastPattern, pattern);
		lastPattern = pattern;
	}

	@Override
	protected void setNextParameter(ConcreteParameter parameter) {
		super.setNextParameter(parameter);
		setNextParameterType(parameter.getType(classLoader));
	}

	/**
	 * Sets the given type as next parameter type of the calling constructor.
	 * Adapts {@link #nextLocalNum} and {@link #parameterTypes} appropriately.
	 * 
	 * @param type
	 */
	private void setNextParameterType(final Type type) {
		nextLocalNum += getAssumedVariableSize(type);
		parameterTypes.add(type);
	}

	/**
	 * @param type
	 * @return {@code type.getSize()}, or 1 if type is null
	 */
	private static int getAssumedVariableSize(final Type type) {
		return type == null ? 1 : type.getSize();
	}

	@Override
	public void visit(ParameterListPattern pattern) {
		linkPattern(pattern);
		patternToLocalNumMap.put(pattern, nextLocalNum);
		indexToListPatternMap.put(getNextListIndex(), pattern);
		super.visit(pattern);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.caesarj.runtime.constructors.APosterioriConstructorAnalysisResult
	 * #getPatternToLocalNumMap()
	 */
	/**
	 * The first visited pattern will be assigned the index provided in the
	 * constructor.
	 */
	@Override
	public Map<ParameterPattern, Integer> getPatternToLocalNumMap() {
		return patternToLocalNumMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.caesarj.runtime.constructors.APosterioriConstructorAnalysisResult
	 * #getPatternToPatternLinks()
	 */
	@Override
	public Map<ParameterPattern, ParameterPattern> getPatternToPatternLinks() {
		return patternToPatternLinks;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.caesarj.runtime.constructors.APosterioriConstructorAnalysisResult
	 * #getIndexToListPatternMap()
	 */
	@Override
	public Map<Integer, ParameterListPattern> getIndexToListPatternMap() {
		return indexToListPatternMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.caesarj.runtime.constructors.APosterioriConstructorAnalysisResult
	 * #getNextVariableIndex()
	 */
	@Override
	public int getNextVariableIndex() {
		return nextLocalNum;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.caesarj.runtime.constructors.APosterioriConstructorAnalysisResult
	 * #getNewVariableIndexShift()
	 */
	@Override
	public int getNewVariableIndexShift() {
		return nextLocalNum - nextLocalNumBeforeConversion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.caesarj.runtime.constructors.APosterioriConstructorAnalysisResult
	 * #getParameterTypes()
	 */
	@Override
	public List<Type> getParameterTypes() {
		return parameterTypes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.caesarj.runtime.constructors.APosterioriConstructorAnalysisResult
	 * #getVariableIndexMap()
	 */
	@Override
	public Map<Integer, Integer> getVariableIndexMap() {
		return variableIndexMap;
	}

}
