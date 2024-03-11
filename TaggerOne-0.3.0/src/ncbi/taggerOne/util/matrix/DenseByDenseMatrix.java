package ncbi.taggerOne.util.matrix;

import java.io.Serializable;

import ncbi.taggerOne.util.Dictionary;
import ncbi.taggerOne.util.vector.DenseVector;
import ncbi.taggerOne.util.vector.Vector;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public class DenseByDenseMatrix<R, C> extends Matrix<R, C> {

	private static final long serialVersionUID = 1L;

	protected DenseVector<C>[] values;

	public static final MatrixFactory factory = new MatrixFactory() {
		@Override
		public <R extends Serializable, C extends Serializable> Matrix<R, C> create(Dictionary<R> rowDictionary, Dictionary<C> columnDictionary) {
			return new DenseByDenseMatrix<R, C>(rowDictionary, columnDictionary);
		}
	};

	@SuppressWarnings("unchecked")
	public DenseByDenseMatrix(Dictionary<R> rowDictionary, Dictionary<C> columnDictionary) {
		super(rowDictionary, columnDictionary);
		values = new DenseVector[numRows];
		for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
			values[rowIndex] = new DenseVector<C>(columnDictionary);
		}
	}

	@Override
	public double get(int rowIndex, int columnIndex) {
		checkIndices(rowIndex, columnIndex);
		return values[rowIndex].get(columnIndex);
	}

	public Vector<C> getRowVector(int rowIndex) {
		return values[rowIndex];
	}

	public void incrementRow(int rowIndex, Vector<C> rowVector) {
		values[rowIndex].increment(rowVector);
	}

	public void incrementRow(int rowIndex, double factor, Vector<C> rowVector) {
		values[rowIndex].increment(factor, rowVector);
	}

	@Override
	public void set(int rowIndex, int columnIndex, double value) {
		checkIndices(rowIndex, columnIndex);
		values[rowIndex].set(columnIndex, value);
	}

	@Override
	public void increment(Matrix<R, C> matrix) {
		checkDimensions(matrix);
		if (matrix instanceof SparseMatrix) {
			SparseMatrix<R, C> sparseMatrix = (SparseMatrix<R, C>) matrix;
			ObjectIterator<it.unimi.dsi.fastutil.longs.Long2DoubleMap.Entry> iterator = sparseMatrix.values.long2DoubleEntrySet().fastIterator();
			while (iterator.hasNext()) {
				it.unimi.dsi.fastutil.longs.Long2DoubleMap.Entry entry = iterator.next();
				long jointIndex = entry.getLongKey();
				int rowIndex = SparseMatrix.getRow(jointIndex);
				int columnIndex = SparseMatrix.getColumn(jointIndex);
				values[rowIndex].increment(columnIndex, entry.getDoubleValue());
			}
		} else if (matrix instanceof DenseByDenseMatrix) {
			DenseByDenseMatrix<R, C> denseMatrix = (DenseByDenseMatrix<R, C>) matrix;
			for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
				values[rowIndex].increment(denseMatrix.values[rowIndex]);
			}
		} else {
			for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
				for (int columnIndex = 0; columnIndex < numColumns; columnIndex++) {
					double matrixValue = matrix.get(rowIndex, columnIndex);
					if (matrixValue != 0.0) {
						values[rowIndex].increment(columnIndex, matrixValue);
					}
				}
			}
		}
	}

	@Override
	public void increment(double value, Matrix<R, C> matrix) {
		checkDimensions(matrix);
		if (matrix instanceof SparseMatrix) {
			SparseMatrix<R, C> sparseMatrix = (SparseMatrix<R, C>) matrix;
			ObjectIterator<it.unimi.dsi.fastutil.longs.Long2DoubleMap.Entry> iterator = sparseMatrix.values.long2DoubleEntrySet().fastIterator();
			while (iterator.hasNext()) {
				it.unimi.dsi.fastutil.longs.Long2DoubleMap.Entry entry = iterator.next();
				long jointIndex = entry.getLongKey();
				int rowIndex = SparseMatrix.getRow(jointIndex);
				int columnIndex = SparseMatrix.getColumn(jointIndex);
				values[rowIndex].increment(columnIndex, value * entry.getDoubleValue());
			}
		} else if (matrix instanceof DenseByDenseMatrix) {
			DenseByDenseMatrix<R, C> denseMatrix = (DenseByDenseMatrix<R, C>) matrix;
			for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
				values[rowIndex].increment(value, denseMatrix.values[rowIndex]);
			}
		} else {
			for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
				for (int columnIndex = 0; columnIndex < numColumns; columnIndex++) {
					double matrixValue = matrix.get(rowIndex, columnIndex);
					if (matrixValue != 0.0) {
						values[rowIndex].increment(columnIndex, value * matrixValue);
					}
				}
			}
		}
	}
}
