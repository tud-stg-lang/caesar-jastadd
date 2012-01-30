package org.caesarj.runtime.constructors;

import java.util.List;
import java.util.Map;

import org.caesarj.util.Cloner;
import org.caesarj.util.InstructionTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;

public class PatternArgumentLoadingInstructionComposer implements
		ParameterPatternVisitor {

	private final List<ConcreteParameter> calledConstructorParameters;

	private final List<InsnList> concreteParameterLoadingInstructions;

	private final APosterioriConstructorAnalysisResult callingPatternAnalysisResult;

	private final Map<ParameterPattern, List<Integer>> calledPatternToParameterMap;

	private int countConcreteParameters;

	private InsnList instructionList;

	private final ClassLoader classLoader;

	/**
	 * @param calledConstructorParameters
	 *            the concrete parameters of the called constructor
	 * @param concreteParameterLoadingInstructions
	 *            instructions for loading contained concrete parameters in the
	 *            order in which the parameters appear in the visited pattern
	 * @param aPosterioriAnalysisResult
	 *            the results of analyzing the parameter pattern of the calling
	 *            constructor with respect to the concrete called constructor
	 * @param callPatternToParametersMap
	 *            a map from patterns of the called constructor to concrete
	 *            parameters
	 */
	public PatternArgumentLoadingInstructionComposer(
			List<ConcreteParameter> calledConstructorParameters,
			List<InsnList> concreteParameterLoadingInstructions,
			APosterioriConstructorAnalysisResult aPosterioriAnalysisResult,
			Map<ParameterPattern, List<Integer>> callPatternToParametersMap,
			ClassLoader classLoader) {
		this.calledConstructorParameters = calledConstructorParameters;
		this.concreteParameterLoadingInstructions = concreteParameterLoadingInstructions;
		this.callingPatternAnalysisResult = aPosterioriAnalysisResult;
		this.calledPatternToParameterMap = callPatternToParametersMap;
		this.classLoader = classLoader;
	}

	@Override
	public void visit(ConcreteParameter parameter) {
		instructionList = Cloner.clone(concreteParameterLoadingInstructions
				.get(0));
		new InstructionTransformer(
				callingPatternAnalysisResult.getVariableIndexMap(),
				callingPatternAnalysisResult.getNewVariableIndexShift())
				.transformVariableIndexes(instructionList);
		/*
		 * TODO possibly value conversion necessary (int -> float etc.)<br>
		 * Then: Don't forget adapting
		 * org.caesarj.util.ClassAccess.PRIMITIVES_ASSIGNABLE_TO and
		 * PRIMITIVES_UNBOXED_TYPES appropriately (int can be assigned to float
		 * etc.).
		 */
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
					calledConstructorParameters,
					concreteParameterLoadingInstructions.subList(
							countConcreteParameters,
							concreteParameterLoadingInstructions.size()),
					callingPatternAnalysisResult, calledPatternToParameterMap,
					classLoader);
			pattern.accept(composer);
			instructionList.add(composer.getInstructionList());
			countConcreteParameters += composer.getCountConcreteParameters();
		}
	}

	@Override
	public void visit(ParameterListPattern pattern) {
		final ParameterListPattern listPattern = callingPatternAnalysisResult
				.getIndexToListPatternMap().get(
						pattern.getReferencedListIndex());
		if (listPattern == null)
			throw new Error(String.format(
					"No list pattern is assigned to index %d.",
					pattern.getReferencedListIndex()));
		Integer localNum = callingPatternAnalysisResult
				.getPatternToLocalNumMap().get(listPattern);
		if (localNum == null)
			throw new Error(String.format(
					"No variable index is assigned to the list with index %d.",
					pattern.getReferencedListIndex()));
		List<Integer> calledParameters = calledPatternToParameterMap
				.get(pattern);
		if (calledParameters == null)
			throw new Error(
					String.format(
							"No concrete parameters are mapped to the list with index %d.",
							pattern.getReferencedListIndex()));

		instructionList = new InsnList();
		for (int paramIndex : calledParameters) {
			ConcreteParameter parameter = calledConstructorParameters
					.get(paramIndex);
			Type type = parameter.getType(classLoader);
			instructionList.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD),
					localNum));
			localNum += type.getSize();
		}

		countConcreteParameters = 0;
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
