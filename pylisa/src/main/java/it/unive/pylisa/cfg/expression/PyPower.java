package it.unive.pylisa.cfg.expression;

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
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.pylisa.symbolic.operators.Power;

public class PyPower extends BinaryExpression {

	public PyPower(CFG cfg, CodeLocation loc, Expression left, Expression right) {
		super(cfg, loc, "**", left, right);
	}

	@Override
	public <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> binarySemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					AnalysisState<A, H, V, T> state,
					SymbolicExpression left,
					SymbolicExpression right,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		TypeSystem types = getProgram().getTypes();
		if (left.getRuntimeTypes(types).stream().anyMatch(Type::isNumericType) && right.getRuntimeTypes(types).stream().anyMatch(Type::isNumericType)) {
			return state.smallStepSemantics(
					new it.unive.lisa.symbolic.value.BinaryExpression(
							getStaticType(),
							left,
							right,
							Power.INSTANCE,
							getLocation()),
					this);
		}

		return state.bottom();
	}
}
