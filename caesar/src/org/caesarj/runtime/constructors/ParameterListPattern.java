package org.caesarj.runtime.constructors;

/**
 * Parameter pattern which represents a list of actual arguments.
 * 
 * @author Marko Martin
 */
public class ParameterListPattern implements ParameterPattern {

	private int referencedListIndex;

	public ParameterListPattern(int referencedListIndex) {
		this.referencedListIndex = referencedListIndex;
	}

	public ParameterListPattern() {
	}

	/**
	 * @return the index of the list in the pattern of the calling constructor
	 *         which is referenced by this list pattern applied as argument to
	 *         the called constructor<br>
	 *         If this pattern is used as pattern of the calling constructor,
	 *         the return value is undefined.
	 */
	public int getReferencedListIndex() {
		return referencedListIndex;
	}

	public void setReferencedListIndex(int referencedListIndex) {
		this.referencedListIndex = referencedListIndex;
	}

	@Override
	public void accept(ParameterPatternVisitor visitor) {
		visitor.visit(this);
	}

}
