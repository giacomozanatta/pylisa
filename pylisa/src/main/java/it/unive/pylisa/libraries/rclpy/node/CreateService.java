package it.unive.pylisa.libraries.rclpy.node;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.global.AccessInstanceGlobal;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.pylisa.cfg.expression.PyNewObj;
import it.unive.pylisa.cfg.type.PyClassType;
import it.unive.pylisa.libraries.LibrarySpecificationProvider;
import it.unive.ros.lisa.symbolic.operators.ros.ROSTopicNameExpansion;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CreateService extends NaryExpression implements PluggableStatement {

	protected Statement st;

	protected CreateService(
			CFG cfg,
			CodeLocation location,
			String constructName,
			Expression... parameters) {
		super(cfg, location, constructName, parameters);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	public static CreateService build(
			CFG cfg,
			CodeLocation location,
			Expression[] exprs) {
		return new CreateService(cfg, location, "create_service", exprs);
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		AnalysisState<A> result = state.bottom();

		params[2] = SemanticsHelpers.nameExpansion(this, getSubExpressions()[0], params[2], interprocedural, state,
				expressions);

		PyClassType serviceClassType = PyClassType.lookup(LibrarySpecificationProvider.RCLPY_SERVICE);

		PyNewObj serviceObj = new PyNewObj(this.getCFG(), (SourceCodeLocation) getLocation(), "__init__",
				serviceClassType, Arrays.copyOfRange(getSubExpressions(), 1, getSubExpressions().length));
		AnalysisState<A> newServiceAS = serviceObj.forwardSemanticsAux(interprocedural,
				state, Arrays.copyOfRange(params, 1, params.length), expressions);

		return result.lub(newServiceAS);
	}

	@Override
	public void setOriginatingStatement(
			Statement statement) {
		this.st = statement;
	}
}