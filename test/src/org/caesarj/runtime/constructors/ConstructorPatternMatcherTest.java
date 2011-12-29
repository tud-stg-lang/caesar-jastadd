package org.caesarj.runtime.constructors;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/**
 * Tests the {@link ConstructorPatternMatcher}.
 * 
 * @author Marko Martin
 */
public class ConstructorPatternMatcherTest extends TestCase {

	private static final List<ConcreteParameter> DUMMY_PARAMETERS_1 = Arrays
			.asList(new ConcreteParameter(String.class.getName()),
					new ConcreteParameter(ParameterPatternVisitor.class
							.getName()));

	public void test_fail_wrongNumberOfParameters() {
		ConstructorPatternMatcher matcher = new ConstructorPatternMatcher(
				DUMMY_PARAMETERS_1);
		new ConcreteParameter(String.class.getName()).accept(matcher);

		assertTrue(matcher.hasFailed());
		assertNull(matcher.getParameterToPatternList());
		assertNull(matcher.getPatternToParametersMap());
	}

	public void test_fail_wrongTypeOfParameters() {
		ConstructorPatternMatcher matcher = new ConstructorPatternMatcher(
				DUMMY_PARAMETERS_1);
		ParameterPatternList list = new ParameterPatternList();
		list.getComponents().add(new ConcreteParameter(String.class.getName()));
		list.getComponents().add(new ConcreteParameter(Object.class.getName()));
		list.accept(matcher);

		assertTrue(matcher.hasFailed());
		assertNull(matcher.getParameterToPatternList());
		assertNull(matcher.getPatternToParametersMap());
	}

	public void test_success_parameters1() {
		ConstructorPatternMatcher matcher = new ConstructorPatternMatcher(
				DUMMY_PARAMETERS_1);
		ParameterPatternList list = new ParameterPatternList();
		ConcreteParameter stringParameter = new ConcreteParameter(
				String.class.getName());
		list.getComponents().add(stringParameter);
		ConcreteParameter visitorParameter = new ConcreteParameter(
				ConstructorPatternMatcher.class.getName());
		list.getComponents().add(visitorParameter);
		list.accept(matcher);

		assertFalse(matcher.hasFailed());
		List<ParameterPattern> resultList = matcher.getParameterToPatternList();
		assertEquals(2, resultList.size());
		assertEquals(stringParameter, resultList.get(0));
		assertEquals(visitorParameter, resultList.get(1));
		Map<ParameterPattern, Set<Integer>> resultMap = matcher
				.getPatternToParametersMap();
		assertEquals(2, resultMap.size());
		assertEquals(Collections.singleton(0), resultMap.get(stringParameter));
		assertEquals(Collections.singleton(1), resultMap.get(visitorParameter));
	}

	public void test_success_emptyParameters() {
		ConstructorPatternMatcher matcher = new ConstructorPatternMatcher(
				Collections.<ConcreteParameter> emptyList());
		new ParameterPatternList().accept(matcher);

		assertFalse(matcher.hasFailed());
		assertTrue(matcher.getParameterToPatternList().isEmpty());
		assertTrue(matcher.getPatternToParametersMap().isEmpty());
	}

}
