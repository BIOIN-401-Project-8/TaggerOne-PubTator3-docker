package ncbi.taggerOne.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import ncbi.util.Profiler;

public class ArraySet<E extends Serializable> implements Set<E>, Serializable {

	private static final long serialVersionUID = 1L;

	private ArrayList<E> elements;

	public ArraySet() {
		elements = new ArrayList<E>();
	}

	public ArraySet(Set<? extends E> c) {
		elements = new ArrayList<E>(c);
	}

	@Override
	public boolean add(E e) {
		Profiler.start("ArraySet.add()");
		if (elements.contains(e)) {
			Profiler.stop("ArraySet.add()");
			return false;
		}
		elements.add(e);
		Profiler.stop("ArraySet.add()");
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		if (elements.size() == 0 && c instanceof Set) {
			return elements.addAll(c);
		} else {
			boolean changed = false;
			for (E e : c) {
				changed = add(e) || changed;
			}
			return changed;
		}
	}

	@Override
	public void clear() {
		elements.clear();
	}

	@Override
	public boolean contains(Object e) {
		return elements.contains(e);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return elements.containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return elements.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return elements.iterator();
	}

	@Override
	public boolean remove(Object e) {
		return elements.remove(e);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return elements.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return elements.retainAll(c);
	}

	@Override
	public int size() {
		return elements.size();
	}

	@Override
	public Object[] toArray() {
		return elements.toArray();
	}

	@Override
	public <T> T[] toArray(T[] e) {
		return null;
	}

	public int indexOf(Object e) {
		return elements.indexOf(e);
	}

	public void trimToSize() {
		elements.trimToSize();
	}
}
