package org.caesarj.ast;

import java.util.Iterator;

import org.caesarj.ast.ASTNode;

public class ChildIterator implements Iterator<ASTNode> {
	private final ASTNode node;

	private int index;

	public ChildIterator(ASTNode node) {
		this.node = node;
		this.index = 0;
	}

	public boolean hasNext() {
		return index < node.getNumChild();
	}

	public ASTNode next() {
		return node.getChild(index++);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
