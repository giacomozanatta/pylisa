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
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

public class PyTernaryOperator extends Expression {

	private final Expression condition;
	private final Expression ifTrue;
	private final Expression ifFalse;

	public PyTernaryOperator(CFG cfg, CodeLocation location, Expression condition, Expression ifTrue,
			Expression ifFalse) {
		super(cfg, location, ifTrue.getStaticType().commonSupertype(ifFalse.getStaticType()));
		this.condition = condition;
		this.ifTrue = ifTrue;
		this.ifFalse = ifFalse;
	}

	public Expression getCondition() {
		return condition;
	}

	public Expression getIfTrue() {
		return ifTrue;
	}

	public Expression getIfFalse() {
		return ifFalse;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((condition == null) ? 0 : condition.hashCode());
		result = prime * result + ((ifFalse == null) ? 0 : ifFalse.hashCode());
		result = prime * result + ((ifTrue == null) ? 0 : ifTrue.hashCode());
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
		PyTernaryOperator other = (PyTernaryOperator) obj;
		if (condition == null) {
			if (other.condition != null)
				return false;
		} else if (!condition.equals(other.condition))
			return false;
		if (ifFalse == null) {
			if (other.ifFalse != null)
				return false;
		} else if (!ifFalse.equals(other.ifFalse))
			return false;
		if (ifTrue == null) {
			if (other.ifTrue != null)
				return false;
		} else if (!ifTrue.equals(other.ifTrue))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return ifTrue + " if " + condition + " else " + ifFalse;
	}

	@Override
	public int setOffset(int offset) {
		this.offset = offset;
		int off = ifTrue.setOffset(this.offset + 1);
		off = condition.setOffset(off + 1);
		off = ifFalse.setOffset(off + 1);
		return off;
	}

	@Override
	public <V> boolean accept(GraphVisitor<CFG, Statement, Edge, V> visitor, V tool) {
		if (!ifTrue.accept(visitor, tool))
			return false;
		if (!condition.accept(visitor, tool))
			return false;
		if (!ifFalse.accept(visitor, tool))
			return false;
		return visitor.visit(tool, getCFG(), this);
	}

	@Override
	public <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> semantics(
					AnalysisState<A, H, V, T> entryState,
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		AnalysisState<A, H, V, T> postCondition = condition.semantics(entryState, interprocedural, expressions);
		for (SymbolicExpression cond : postCondition.getComputedExpressions()) {
			UnaryExpression negated = new UnaryExpression(cond.getStaticType(), cond, LogicalNegation.INSTANCE,
					cond.getCodeLocation());
			// TODO: fix assume
			switch (postCondition.satisfies(cond, this)) {
			case BOTTOM:
				return entryState.bottom();
			case NOT_SATISFIED:
				return ifFalse.semantics(postCondition.assume(cond, condition), interprocedural, expressions);
			case SATISFIED:
				return ifTrue.semantics(postCondition.assume(negated, condition), interprocedural, expressions);
			case UNKNOWN:
				return ifTrue.semantics(postCondition.assume(cond, condition), interprocedural, expressions)
						.lub(ifFalse.semantics(postCondition.assume(negated, condition), interprocedural, expressions));
			}
		}

		return entryState.top();
	}
}
