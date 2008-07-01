package org.caesarj.ast;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SingleIterator<E> implements Iterator<E> {
	private E object;

	public SingleIterator(E object) {
		this.object = object;
	}

	public boolean hasNext() {
		return object != null;
	}

	public E next() {
		if (object == null)
			throw new NoSuchElementException();
		E ret = object;
		object = null;
		return ret;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
