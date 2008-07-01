package org.caesarj.ast;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class NestedIterator<E> implements Iterator<E> {
	private final Iterator<E> first;

	private final Iterator<E> second;

	public NestedIterator(Iterator<E> first, Iterator<E> second) {
		this.first = first;
		this.second = second;
	}

	public boolean hasNext() {
		return first.hasNext() || second.hasNext();
	}

	public E next() {
		if (first.hasNext())
			return first.next();
		if (second.hasNext())
			return second.next();
		throw new NoSuchElementException();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
