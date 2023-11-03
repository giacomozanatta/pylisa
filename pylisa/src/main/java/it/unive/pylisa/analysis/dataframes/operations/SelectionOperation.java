package it.unive.pylisa.analysis.dataframes.operations;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.pylisa.analysis.dataframes.operations.selection.Selection;

public abstract class SelectionOperation<S extends Selection<S>> extends DataframeOperation {

	private final S selection;

	public SelectionOperation(
			CodeLocation where,
			S selection) {
		super(where);
		this.selection = selection;
	}

	public S getSelection() {
		return selection;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected boolean lessOrEqualSameOperation(
			DataframeOperation other)
			throws SemanticException {
		SelectionOperation<?> o = (SelectionOperation<?>) other;
		if (selection.getClass() != o.selection.getClass())
			return false;
		return selection.lessOrEqual((S) o.selection);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected DataframeOperation lubSameOperation(
			DataframeOperation other)
			throws SemanticException {
		SelectionOperation<?> o = (SelectionOperation<?>) other;
		if (selection.getClass() != o.selection.getClass())
			return top();
		return mk(loc(other), selection.lub((S) o.selection));
	}

	protected abstract SelectionOperation<S> mk(
			CodeLocation where,
			S selection);

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((selection == null) ? 0 : selection.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SelectionOperation<?> other = (SelectionOperation<?>) obj;
		if (selection == null) {
			if (other.selection != null)
				return false;
		} else if (!selection.equals(other.selection))
			return false;
		return true;
	}

	@Override
	protected int compareToSameClassAndLocation(
			DataframeOperation o) {
		SelectionOperation<?> other = (SelectionOperation<?>) o;
		return selection.compareTo(other.selection);
	}
}
