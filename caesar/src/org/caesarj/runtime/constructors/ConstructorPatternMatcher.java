package org.caesarj.runtime.constructors;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.caesarj.util.ClassAccess;

/**
 * Matches {@link ParameterPattern}s against actual formal parameters given as a
 * list of classes in the constructor. Results can be obtained with
 * {@link #getParameterToPatternList()} and {@link #getPatternToParametersMap()}
 * .
 * 
 * @author Marko Martin
 */
public class ConstructorPatternMatcher implements ParameterPatternVisitor {

	private final List<ConcreteParameter> parameters;

	/**
	 * @see #getParameterToPatternList()
	 */
	private List<ParameterPattern> parameterToPatternList;

	/**
	 * @see #getPatternToParametersMap()
	 */
	private Map<ParameterPattern, Set<Integer>> patternToParametersMap;

	private final ClassLoader classLoader;

	/**
	 * @param parameters
	 *            the formal parameter list against which to match the visited
	 *            pattern
	 * @param classLoader
	 *            the loader to be used to load classes in order to access their
	 *            type hierarchy
	 * @throws IllegalArgumentException
	 *             if parameters or classLoader is null
	 */
	public ConstructorPatternMatcher(List<ConcreteParameter> parameters,
			ClassLoader classLoader) throws IllegalArgumentException {
		if (parameters == null || classLoader == null)
			throw new IllegalArgumentException(
					"parameters and classLoader must not be null.");
		this.parameters = parameters;
		this.classLoader = classLoader;
	}

	/**
	 * This is equivalent to {@code ConstructorPatternMatcher(parameters,
	 * ClassLoader.getSystemClassLoader())}.
	 * 
	 * @param parameters
	 *            the formal parameter list against which to match the visited
	 *            pattern
	 * @throws IllegalArgumentException
	 *             if parameters is null
	 */
	public ConstructorPatternMatcher(List<ConcreteParameter> parameters)
			throws IllegalArgumentException {
		this(parameters, ClassLoader.getSystemClassLoader());
	}

	@Override
	public void visit(ConcreteParameter parameter) {
		if (parameters.size() != 1) {
			fail();
			return;
		}
		if (!ClassAccess.isAssignableFrom(parameters.get(0).getTypeName(),
				parameter.getTypeName(), classLoader)) {
			fail();
			return;
		}
		parameterToPatternList = Arrays.asList((ParameterPattern) parameter);
		patternToParametersMap = Collections.singletonMap(
				(ParameterPattern) parameter, Collections.singleton(0));
	}

	@Override
	public void visit(CompositeParameterPattern compositePattern) {
	}

	/**
	 * Tries to match the given list to the {@link #parameters}. Thereby,
	 * earlier patterns are preferably matched to larger ranges of parameters.
	 * E.g., IF list contains two patterns, parameters contains four items, the
	 * first pattern could be either matched to all or to the first two
	 * parameters, and the second pattern could be either matched to the last
	 * two or to no parameter, THEN this method will assign the first pattern to
	 * all four parameters and the second one to no parameter.
	 */
	@Override
	public void visit(ParameterPatternList list) {
		int countListPatterns = list.getComponents().size();
		if (countListPatterns == 0) {
			handleCaseOfEmptyPatternList();
			return;
		}
		ParameterPattern[] parameterToPatternList = new ParameterPattern[parameters
				.size()];
		@SuppressWarnings("unchecked")
		Map<ParameterPattern, Set<Integer>>[] patternToParameterMaps = new Map[countListPatterns];
		int curListPattern = 0;
		/*
		 * firstMappedParameters[i] is the index of the first parameter (list
		 * "parameters") mapped to the list component i+1.
		 */
		int[] firstMappedParameters = new int[countListPatterns - 1];
		int toIndex = parameters.size();
		patternLoop: while (curListPattern < countListPatterns) {
			/*
			 * INVARIANT: toIndex is the next value to be tried for
			 * firstMappedParameters[curListPattern], or, if curListPattern >
			 * firstMappedParameters.length, then toIndex == parameters.size().
			 * 
			 * INVARIANT: firstMappedParameters[i] <= firstMappedParameters[j]
			 * for 0 <= i <= j < curListPattern
			 */
			int fromIndex = curListPattern > 0 ? firstMappedParameters[curListPattern - 1]
					: 0;
			if (fromIndex > toIndex) {
				if (curListPattern <= 0) {
					fail();
					return;
				}
				curListPattern--;
				toIndex = firstMappedParameters[curListPattern] - 1;
				continue;
			}
			while (true) {
				ConstructorPatternMatcher matcher = new ConstructorPatternMatcher(
						parameters.subList(fromIndex, toIndex), classLoader);
				list.getComponents().get(curListPattern).accept(matcher);
				if (matcher.hasFailed()) {
					if (fromIndex >= toIndex
							|| curListPattern == countListPatterns - 1) {
						toIndex = -1;
						continue patternLoop;
					}
					toIndex--;
				} else {
					for (ParameterPattern pattern : matcher.parameterToPatternList)
						parameterToPatternList[fromIndex++] = pattern;
					assert fromIndex == toIndex;
					patternToParameterMaps[curListPattern] = matcher.patternToParametersMap;
					if (curListPattern >= countListPatterns - 1)
						break patternLoop;
					firstMappedParameters[curListPattern] = toIndex;
					curListPattern++;
					toIndex = parameters.size();
					continue patternLoop;
				}
			}
		}

		// Set the results:
		patternToParametersMap = new HashMap<ParameterPattern, Set<Integer>>();
		patternToParametersMap.putAll(patternToParameterMaps[0]);
		for (int i = 1; i < countListPatterns; i++)
			for (Entry<ParameterPattern, Set<Integer>> entry : patternToParameterMaps[i]
					.entrySet()) {
				Set<Integer> transformedSet = new HashSet<Integer>();
				for (int parameterIndex : entry.getValue())
					transformedSet.add(parameterIndex
							+ firstMappedParameters[i - 1]);
				patternToParametersMap.put(entry.getKey(), transformedSet);
			}
		this.parameterToPatternList = Arrays.asList(parameterToPatternList);
	}

	private void handleCaseOfEmptyPatternList() {
		if (parameters.size() > 0)
			fail();
		else {
			parameterToPatternList = Collections.emptyList();
			patternToParametersMap = Collections.emptyMap();
		}
		return;
	}

	/**
	 * @return true after visiting a pattern if this pattern does not match the
	 *         list of parameters specified in the constructor
	 */
	public boolean hasFailed() {
		return parameterToPatternList == null;
	}

	/**
	 * Sets the result fields {@link #parameterToPatternList} and
	 * {@link #patternToParametersMap} to null in case that the parameter list
	 * does not match the visited pattern.
	 */
	private void fail() {
		parameterToPatternList = null;
		patternToParametersMap = null;
	}

	/**
	 * @return The i-th element will contain the pattern which matches the
	 *         parameter at position i in the list {@link #parameters}.<br>
	 *         This is null if there is no match.
	 */
	public List<ParameterPattern> getParameterToPatternList() {
		return parameterToPatternList;
	}

	/**
	 * @return a map which maps parameter patterns to the set of parameters
	 *         (indexes in the list {@link #parameters}) which have been
	 *         assigned by this matcher<br>
	 *         This is null if there is no match.
	 */
	public Map<ParameterPattern, Set<Integer>> getPatternToParametersMap() {
		return patternToParametersMap;
	}

}