package org.caesarj.ast;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.caesarj.ast.ASTNode;
import org.caesarj.ast.Access;
import org.caesarj.ast.List;
import org.caesarj.ast.TypeDecl;

public class TypeListIterator implements Iterator<TypeDecl> {
	private final ASTNode accessList;

	private int index;

	public TypeListIterator(List accessList) {
		this.accessList = accessList;
		this.index = 0;
	}

	public boolean hasNext() {
		return index < accessList.getNumChild();
	}

	public TypeDecl next() {
		return ((Access) accessList.getChild(index++)).type();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
