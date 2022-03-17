package it.unive.pylisa.cfg.expression.comparison;

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
import it.unive.lisa.program.cfg.statement.comparison.GreaterOrEqual;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.pylisa.symbolic.operators.ComparisonOperator;

public class PyGreaterOrEqual extends GreaterOrEqual {

    public PyGreaterOrEqual(CFG cfg, CodeLocation location, Expression left, Expression right) {
        super(cfg, location, left, right);
    }

    @Override
    protected <A extends AbstractState<A, H, V, T>, H extends HeapDomain<H>, V extends ValueDomain<V>, T extends TypeDomain<T>> AnalysisState<A, H, V, T> binarySemantics(
            InterproceduralAnalysis<A, H, V, T> interprocedural, AnalysisState<A, H, V, T> state,
            SymbolicExpression left, SymbolicExpression right, StatementStore<A, H, V, T> expressions)
            throws SemanticException {
        AnalysisState<A, H, V, T> pandasSeriesSemantics = PandasSeriesComparisonSemantics.pandasSeriesBinarySemantics(interprocedural, state, left, right, expressions, this, getLocation(), ComparisonOperator.GEQ);
        if (pandasSeriesSemantics != null)
            return pandasSeriesSemantics;

        return super.binarySemantics(interprocedural, state, left, right, expressions);
    }
    
}