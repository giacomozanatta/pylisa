package it.unive.pylisa.libraries.pandas;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.pylisa.symbolic.operators.dataframes.ApplyTransformation;

public class ToDatetime extends it.unive.lisa.program.cfg.statement.UnaryExpression implements PluggableStatement {

	private Statement st;

	public ToDatetime(CFG cfg, CodeLocation location, String constructName, Expression series) {
		super(cfg, location, constructName, series);
	}

	public static ToDatetime build(CFG cfg, CodeLocation location, Expression[] exprs) {
		return new ToDatetime(cfg, location, "to_datetime", exprs[0]);
	}

	@Override
	final public void setOriginatingStatement(Statement st) {
		this.st = st;
	}

	@Override
	public <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> unarySemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					AnalysisState<A, H, V, T> state,
					SymbolicExpression expr,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		ApplyTransformation op = new ApplyTransformation(ApplyTransformation.Kind.TO_DATETIME, false);
		return PandasSemantics.transform(state, expr, st, op);
	}
}
