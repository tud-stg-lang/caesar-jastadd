package org.caesarj.runtime.constructors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Analyzes the parameter pattern (which will be visited) of a "calling"
 * constructor with respect to the results of the analysis of the parameter
 * pattern for the "called" constructor.
 * <p>
 * Each instance of this visitor must be used only once.
 * 
 * @author Marko Martin
 */
public class SimpleCallingConstructorParameterPatternAnalyzer implements
		ParameterPatternVisitor, SimpleAPosterioriConstructorAnalysisResult {

	private final Map<Integer, List<ConcreteParameter>> listIndexToConcreteParameterMap = new HashMap<Integer, List<ConcreteParameter>>();

	/**
	 * the index the next visited list will have
	 */
	private int nextListIndex = 0;

	/**
	 * @see #getConcreteParameters()
	 */
	private final List<ConcreteParameter> concreteParameters = new ArrayList<ConcreteParameter>();

	public SimpleCallingConstructorParameterPatternAnalyzer(
			List<ConcreteParameter> concreteParameters,
			Map<ParameterPattern, List<Integer>> calledPatternToParametersMap) {
		for (Entry<ParameterPattern, List<Integer>> entry : calledPatternToParametersMap
				.entrySet()) {
			if (!(entry.getKey() instanceof ParameterListPattern))
				continue;
			List<ConcreteParameter> parameters = new ArrayList<ConcreteParameter>();
			for (int i : entry.getValue())
				parameters.add(concreteParameters.get(i));
			listIndexToConcreteParameterMap.put(((ParameterListPattern) entry
					.getKey()).getReferencedListIndex(), parameters);
		}
	}

	/**
	 * The default implementation calls
	 * {@link #setNextParameter(ConcreteParameter)}.
	 */
	@Override
	public void visit(ConcreteParameter parameter) {
		setNextParameter(parameter);
	}

	/**
	 * Sets the given parameter as next parameter type of the calling
	 * constructor. {@link #parameterTypeNames} and {@link #parameterNames}
	 * appropriately.
	 * 
	 * @param type
	 */
	protected void setNextParameter(ConcreteParameter parameter) {
		concreteParameters.add(parameter);
	}

	@Override
	public void visit(CompositeParameterPattern compositePattern) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(ParameterPatternList list) {
		for (ParameterPattern pattern : list.getComponents())
			pattern.accept(this);
	}

	@Override
	public void visit(ParameterListPattern pattern) {
		for (ConcreteParameter param : listIndexToConcreteParameterMap
				.get(nextListIndex++))
			setNextParameter(param);
	}

	@Override
	public List<ConcreteParameter> getConcreteParameters() {
		return concreteParameters;
	}

	protected int getNextListIndex() {
		return nextListIndex;
	}

	protected void setNextListIndex(int nextListIndex) {
		this.nextListIndex = nextListIndex;
	}

}