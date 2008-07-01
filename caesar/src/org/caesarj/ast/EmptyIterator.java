package org.caesarj.ast;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class EmptyIterator implements Iterator {

	public boolean hasNext() {
		return false;
	}

	public Object next() {
		throw new NoSuchElementException();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	static private final EmptyIterator instance = new EmptyIterator();

	static public EmptyIterator instance() {
		return instance;
	}
}
