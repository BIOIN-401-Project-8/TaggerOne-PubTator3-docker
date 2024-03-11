package ncbi.taggerOne.util.vector;

import java.io.Serializable;

import ncbi.taggerOne.util.Dictionary;

public abstract class Vector<E> implements Serializable {

	private static final long serialVersionUID = 1L;

	protected Dictionary<E> dictionary;

	public Vector(Dictionary<E> dictionary) {
		if (dictionary == null) {
			throw new IllegalArgumentException("dictionary cannot be null");
		}
		if (!dictionary.isFrozen()) {
			throw new IllegalArgumentException("dictionary must be frozen");
		}
		this.dictionary = dictionary;
	}

	public Dictionary<E> getDictionary() {
		return dictionary;
	}

	public int dimensions() {
		return dictionary.size();
	}

	public int cardinality() {
		return dictionary.size();
	}

	public abstract boolean isEmpty();

	protected void checkIndex(int index) {
		if (index < 0 || index >= dictionary.size()) {
			throw new IndexOutOfBoundsException("Index must be at least 0 but less than " + dictionary.size() + ": " + index);
		}
	}

	protected void checkDictionary(Vector<E> vector) {
		if (dictionary.size() != vector.dictionary.size()) {
			throw new IllegalArgumentException("Vectors must have the same dimensions: " + dictionary.size() + " != " + vector.dictionary.size());
		}
	}

	public abstract double get(int index);

	public abstract void set(int index, double value);

	public abstract void increment(int index, double value);

	public abstract void increment(double factor, Vector<E> vector);

	public abstract void increment(Vector<E> vector);

	public abstract double length();

	public abstract void normalize();

	public abstract void pack();

	public abstract double dotProduct(Vector<E> vector);

	public abstract Vector<E> copy();

	public abstract String visualize();

	public abstract VectorIterator getIterator();

	public interface VectorIterator {

		public boolean next();

		public int getIndex();

		public double getValue();

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dictionary.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Vector<?> other = (Vector<?>) obj;
		if (!dictionary.equals(other.dictionary))
			return false;
		return true;
	}

}
