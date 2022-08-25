package it.unive.pylisa.analysis.dataframes.transformation.operations;

import it.unive.lisa.program.cfg.CodeLocation;

public class Iteration extends DataframeOperation {

	public Iteration(CodeLocation where) {
		super(where);
	}

	@Override
	protected boolean lessOrEqualSameOperation(DataframeOperation other) {
		return true;
	}

	@Override
	protected DataframeOperation lubSameOperation(DataframeOperation other) {
		return new Iteration(loc(other));
	}

	@Override
	public String toString() {
		return "iterate";
	}
}