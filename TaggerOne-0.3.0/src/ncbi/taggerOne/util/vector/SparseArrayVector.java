package ncbi.taggerOne.util.vector;

import java.io.Serializable;

import ncbi.taggerOne.util.Dictionary;
import ncbi.util.Profiler;

public class SparseArrayVector<E extends Serializable> extends Vector<E> {

	private static final long serialVersionUID = 1L;

	private static final int BUFFER = 3;

	public static final VectorFactory factory = new VectorFactory() {

		private static final long serialVersionUID = 1L;

		@Override
		public <E extends Serializable> Vector<E> create(Dictionary<E> dictionary) {
			return new SparseArrayVector<E>(dictionary);
		}
	};

	int[] indices;
	double[] values;
	int size;

	/*
	 * A Vector implemented as a pair of arrays, sorted by index. The arrays are sorted and lookup uses binary search, making the asymptotic runtime for inserting values O(n) and retrieving values O(log(n)). However this implementation is
	 * much faster than SparseVector for a small number of values and memory usage is also much lower.
	 */
	public SparseArrayVector(Dictionary<E> dictionary) {
		super(dictionary);
		indices = new int[BUFFER];
		values = new double[BUFFER];
		size = 0;
	}

	private SparseArrayVector(Dictionary<E> dictionary, int[] indices, double[] values, int size) {
		super(dictionary);
		this.indices = indices;
		this.values = values;
		this.size = size;
	}

	public void pack() {
		Profiler.start("SparseArrayVector.pack()");
		// Remove zero values
		for (int i = 0; i < size; i++) {
			if (values[i] == 0.0) {
				for (int j = i + 1; j < size; j++) {
					indices[j - 1] = indices[j];
					values[j - 1] = values[j];
				}
				size--;
			}
		}
		int[] newIndices = new int[size];
		System.arraycopy(indices, 0, newIndices, 0, size);
		indices = newIndices;
		double[] newValues = new double[size];
		System.arraycopy(values, 0, newValues, 0, size);
		values = newValues;
		Profiler.stop("SparseArrayVector.pack()");
	}

	/*
	 * This implementation would be even faster if we used linear search for very small spans and interpolation search for very large ones
	 */
	private int findPosition(int index) {
		Profiler.start("SparseArrayVector.findPosition()");
		int low = 0;
		int high = size - 1;
		while (high >= low) {
			int middle = low + (high - low) / 2;
			int indexAtMiddle = indices[middle];
			if (indexAtMiddle == index) {
				Profiler.stop("SparseArrayVector.findPosition()");
				return middle;
			}
			if (indexAtMiddle < index) {
				low = middle + 1;
			}
			if (indexAtMiddle > index) {
				high = middle - 1;
			}
		}
		Profiler.stop("SparseArrayVector.findPosition()");
		return Integer.MIN_VALUE;
	}

	@Override
	public int cardinality() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public double get(int index) {
		Profiler.start("SparseArrayVector.get()");
		checkIndex(index);
		// Note 0.0 is the no entry value
		int position = findPosition(index);
		if (position < 0) {
			Profiler.stop("SparseArrayVector.get()");
			return 0.0;
		}
		double value = values[position];
		Profiler.stop("SparseArrayVector.get()");
		return value;
	}

	private void internalAdd(int index, double value) {
		// Make sure there is space
		if (size >= indices.length) {
			int[] newIndices = new int[indices.length + BUFFER];
			System.arraycopy(indices, 0, newIndices, 0, size);
			indices = newIndices;
			double[] newValues = new double[values.length + BUFFER];
			System.arraycopy(values, 0, newValues, 0, size);
			values = newValues;
		}
		// Add index & value
		indices[size] = index;
		values[size] = value;
		// Percolate both up
		int position = size;
		while (position > 0 && indices[position] < indices[position - 1]) {
			int index2 = indices[position - 1];
			indices[position - 1] = indices[position];
			indices[position] = index2;
			double value2 = values[position - 1];
			values[position - 1] = values[position];
			values[position] = value2;
			position--;
		}
		size++;
	}

	@Override
	public void set(int index, double value) {
		Profiler.start("SparseArrayVector.set()");
		checkIndex(index);
		int position = findPosition(index);
		if (position < 0) {
			internalAdd(index, value);
		} else {
			values[position] = value;
		}
		Profiler.stop("SparseArrayVector.set()");
	}

	private void internalIncrement(int index, double value) {
		Profiler.start("SparseArrayVector.internalIncrement()");
		int position = findPosition(index);
		if (position < 0) {
			internalAdd(index, value);
		} else {
			values[position] += value;
		}
		Profiler.stop("SparseArrayVector.internalIncrement()");
	}

	@Override
	public void increment(int index, double value) {
		Profiler.start("SparseArrayVector.increment()1");
		checkIndex(index);
		internalIncrement(index, value);
		Profiler.stop("SparseArrayVector.increment()1");
	}

	@Override
	public void increment(double factor, Vector<E> vector) {
		Profiler.start("SparseArrayVector.increment()2");
		checkDictionary(vector);
		VectorIterator iterator = vector.getIterator();
		while (iterator.next()) {
			int index = iterator.getIndex();
			double value = iterator.getValue();
			internalIncrement(index, factor * value);
		}
		Profiler.stop("SparseArrayVector.increment()2");
	}

	@Override
	public void increment(Vector<E> vector) {
		Profiler.start("SparseArrayVector.increment()3");
		checkDictionary(vector);
		VectorIterator iterator = vector.getIterator();
		while (iterator.next()) {
			int index = iterator.getIndex();
			double value = iterator.getValue();
			internalIncrement(index, value);
		}
		Profiler.stop("SparseArrayVector.increment()3");
	}

	@Override
	public double length() {
		Profiler.start("SparseArrayVector.length()");
		double length = 0.0;
		for (int i = 0; i < size; i++) {
			length += values[i] * values[i];
		}
		length = Math.sqrt(length);
		Profiler.stop("SparseArrayVector.length()");
		return length;
	}

	@Override
	public void normalize() {
		double length = length();
		for (int i = 0; i < size; i++) {
			values[i] = values[i] / length;
		}
	}

	@Override
	public double dotProduct(Vector<E> vector) {
		Profiler.start("SparseArrayVector.dotProduct()");
		checkDictionary(vector);
		double sum = 0.0;
		for (int i = 0; i < size; i++) {
			sum += values[i] * vector.get(indices[i]);
		}
		Profiler.stop("SparseArrayVector.dotProduct()");
		return sum;
	}

	@Override
	public Vector<E> copy() {
		int[] newIndices = new int[indices.length];
		System.arraycopy(indices, 0, newIndices, 0, size);
		double[] newValues = new double[values.length];
		System.arraycopy(values, 0, newValues, 0, size);
		return new SparseArrayVector<E>(dictionary, newIndices, newValues, size);
	}

	@Override
	public String visualize() {
		StringBuilder str = new StringBuilder("[");
		for (int i = 0; i < size; i++) {
			double value = values[i];
			if (value != 0.0) {
				if (i > 0) {
					str.append(", ");
				}
				int index = indices[i];
				str.append(index);
				str.append(":");
				str.append(dictionary.getElement(index));
				str.append("=");
				str.append(value);
			}
		}
		str.append("]");
		return str.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		for (int i = 0; i < size; i++) {
			double value = values[i];
			if (value != 0.0) {
				result = prime * result + indices[i];
				result = prime * result + Double.hashCode(values[i]);
			}
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SparseArrayVector<?> other = (SparseArrayVector<?>) obj;
		int position = 0;
		int otherPosition = 0;
		while (position < size && otherPosition < other.size) {
			// Ignore zero values
			while (position < size && values[position] == 0.0) {
				position++;
			}
			while (otherPosition < other.size && other.values[otherPosition] == 0.0) {
				otherPosition++;
			}
			if (position < size && otherPosition < other.size) {
				if (indices[position] != other.indices[otherPosition] || values[position] != other.values[otherPosition]) {
					return false;
				}
			}
			position++;
			otherPosition++;
		}
		return position >= size && otherPosition >= other.size;
	}

	@Override
	public VectorIterator getIterator() {
		return new SparseArrayVectorIterator(indices, values, size);
	}

	private class SparseArrayVectorIterator implements VectorIterator {
		private int[] indices;
		private double[] values;
		private int size;
		private int currentPosition;

		public SparseArrayVectorIterator(int[] indices, double[] values, int size) {
			this.indices = indices;
			this.values = values;
			this.size = size;
			currentPosition = -1;
		}

		@Override
		public boolean next() {
			currentPosition++;
			while (currentPosition < size && values[currentPosition] == 0.0) {
				currentPosition++;
			}
			return currentPosition < size;
		}

		@Override
		public int getIndex() {
			return indices[currentPosition];
		}

		@Override
		public double getValue() {
			return values[currentPosition];
		}
	}
}
