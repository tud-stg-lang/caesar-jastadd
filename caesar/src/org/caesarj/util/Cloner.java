package org.caesarj.util;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

public class Cloner {

	public static InsnList clone(InsnList insnList) {
		InsnList instructionList = new InsnList();
		@SuppressWarnings("unchecked")
		ListIterator<AbstractInsnNode> iterator = insnList.iterator();
		Map<LabelNode, LabelNode> labelMap = new HashMap<LabelNode, LabelNode>();
		while (iterator.hasNext()) {
			AbstractInsnNode node = iterator.next();
			if (node instanceof LabelNode)
				labelMap.put((LabelNode) node, new LabelNode());
			AbstractInsnNode clone = node.clone(labelMap);
			if (clone != null)
				instructionList.add(clone);
		}
		return instructionList;
	}

}
