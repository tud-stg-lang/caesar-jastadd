package org.caesarj.runtime.constructors;

public interface ParameterPatternVisitor {

	void visit(ConcreteParameter parameter);

	void visit(CompositeParameterPattern compositePattern);
	
	void visit(ParameterPatternList list);
	
	void visit(ParameterListPattern pattern);

}
