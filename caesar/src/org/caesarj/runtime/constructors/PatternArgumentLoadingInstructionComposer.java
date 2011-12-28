package org.caesarj.runtime.constructors;

import java.util.List;

import org.caesarj.util.Cloner;
import org.objectweb.asm.tree.InsnList;

public class PatternArgumentLoadingInstructionComposer implements
		ParameterPatternVisitor {

	private List<InsnList> concreteParameterLoadingInstructions;

	private int countConcreteParameters;

	private InsnList instructionList;

	/**
	 * @param concreteParameterLoadingInstructions
	 *            instructions for loading contained concrete parameters in the
	 *            order in which the parameters appear in the visited pattern
	 */
	public PatternArgumentLoadingInstructionComposer(
			List<InsnList> concreteParameterLoadingInstructions) {
		this.concreteParameterLoadingInstructions = concreteParameterLoadingInstructions;
	}

	@Override
	public void visit(ConcreteParameter parameter) {
		instructionList = Cloner.clone(concreteParameterLoadingInstructions
				.get(0));
		countConcreteParameters = 1;
	}

	@Override
	public void visit(CompositeParameterPattern compositePattern) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(ParameterPatternList list) {
		countConcreteParameters = 0;
		instructionList = new InsnList();
		for (ParameterPattern pattern : list.getComponents()) {
			PatternArgumentLoadingInstructionComposer composer = new PatternArgumentLoadingInstructionComposer(
					concreteParameterLoadingInstructions.subList(
							countConcreteParameters,
							concreteParameterLoadingInstructions.size()));
			pattern.accept(composer);
			instructionList.add(composer.getInstructionList());
			countConcreteParameters += composer.getCountConcreteParameters();
		}

	}

	/**
	 * @return the number of concrete parameters loaded within the visited
	 *         pattern
	 */
	public int getCountConcreteParameters() {
		return countConcreteParameters;
	}

	/**
	 * @return the instruction list which pushes arguments for the whole visited
	 *         pattern onto the stack
	 */
	public InsnList getInstructionList() {
		return instructionList;
	}

}
