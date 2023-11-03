package it.unive.pylisa.analysis.dataframes.operations.selection;

import it.unive.lisa.analysis.SemanticException;
import it.unive.pylisa.analysis.dataframes.Names;

public class ColumnIteration extends ColumnSelection<ColumnIteration> {

	@Override
	public ColumnIteration top() {
		return this;
	}

	@Override
	public ColumnIteration bottom() {
		return this;
	}

	@Override
	public ColumnIteration lubAux(
			ColumnIteration other)
			throws SemanticException {
		return this;
	}

	@Override
	public ColumnIteration wideningAux(
			ColumnIteration other)
			throws SemanticException {
		return this;
	}

	@Override
	public boolean lessOrEqualAux(
			ColumnIteration other)
			throws SemanticException {
		return true;
	}

	@Override
	public int hashCode() {
		return getClass().getName().hashCode();
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "iterated_column";
	}

	@Override
	protected int compareToSameClass(
			Selection<?> o) {
		return 0;
	}

	@Override
	public Names extractColumnNames() {
		return Names.BOTTOM;
	}
}
