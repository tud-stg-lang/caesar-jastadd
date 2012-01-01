package org.caesarj.util;

import java.util.ListIterator;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;

public class InstructionTransformer {

	private final Map<Integer, Integer> variableIndexMap;

	private final int newVariableIndexShift;

	/**
	 * 
	 * @param variableIndexMap
	 *            specifies how old indexes map to new ones
	 * @param newVariableIndexShift
	 *            If an index is not found as key in variableIndexMap, the
	 *            respective instructions is assumed to address a new variable
	 *            and, thus, the given shift value is added to this index.
	 */
	public InstructionTransformer(Map<Integer, Integer> variableIndexMap,
			int newVariableIndexShift) {
		this.variableIndexMap = variableIndexMap;
		this.newVariableIndexShift = newVariableIndexShift;
	}

	/**
	 * Transforms the given instructions for an adapted variable index list and
	 * a shift for variables as defined in the constructor. Accesses to variable
	 * index 0 are never changed because this always refers to the "this"
	 * pointer.
	 * 
	 * @param instructions
	 */
	public void transformVariableIndexes(InsnList instructions) {
		@SuppressWarnings("unchecked")
		ListIterator<AbstractInsnNode> iterator = instructions.iterator();
		while (iterator.hasNext()) {
			AbstractInsnNode insnNode = iterator.next();
			if (insnNode instanceof VarInsnNode) {
				VarInsnNode varInsnNode = (VarInsnNode) insnNode;
				varInsnNode.var = transformVariableIndex(varInsnNode.var);
			} else if (insnNode instanceof IincInsnNode) {
				IincInsnNode iincInsnNode = (IincInsnNode) insnNode;
				iincInsnNode.var = transformVariableIndex(iincInsnNode.var);
			} else if (insnNode instanceof FrameNode) {
				FrameNode frame = (FrameNode) insnNode;
				switch (frame.type) {
				case Opcodes.F_NEW:
					// TODO
				}
			}
		}
	}

	/**
	 * @param var
	 * @return the new index of the given old variable index
	 */
	public int transformVariableIndex(int var) {
		if (var == 0)
			return 0; // don't transform "this"
		Integer newIndex = variableIndexMap.get(var);
		if (newIndex == null)
			/*
			 * This is a new variable.
			 */
			return var + newVariableIndexShift;
		return newIndex;
	}

}
