package it.unive.pylisa.libraries.rclpy.publisher;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.*;
import it.unive.lisa.program.cfg.statement.global.AccessInstanceGlobal;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.pylisa.cfg.expression.PyAssign;


public class Init extends it.unive.lisa.program.cfg.statement.NaryExpression implements PluggableStatement {
    protected Statement st;
    public Init(CFG cfg, CodeLocation location, Expression[] exprs) {
        super(cfg, location, "__init__", exprs);
    }

    public static Init build(CFG cfg, CodeLocation location, Expression[] exprs) {
        return new Init(cfg, location, exprs);
    }


    @Override
    public String toString() {
        return "__init__";
    }

    @Override
    public <A extends AbstractState<A, H, V, T>, H extends HeapDomain<H>, V extends ValueDomain<V>, T extends TypeDomain<T>> AnalysisState<A, H, V, T> expressionSemantics(InterproceduralAnalysis<A, H, V, T> interprocedural, AnalysisState<A, H, V, T> state, ExpressionSet<SymbolicExpression>[] params, StatementStore<A, H, V, T> expressions) throws SemanticException {
        AnalysisState<A,H,V,T> result = state;
        AccessInstanceGlobal aig;
        PyAssign pa;
        // get list of publishers from self._publisher
        // self is first parameter.
        //AccessChild _publishers = new AccessChild(new HeapReference(PyClassType.lookup(LibrarySpecificationProvider.RCLPY_NODE),  params[0], getLocation())), "node_name")"
        aig = new AccessInstanceGlobal(st.getCFG(), getLocation(), getSubExpressions()[0], "msg_type");
        pa = new PyAssign(getCFG(), getLocation(), aig, getSubExpressions()[1]);
        result =  result.lub(pa.semantics(result, interprocedural, expressions));
        aig = new AccessInstanceGlobal(st.getCFG(), getLocation(), getSubExpressions()[0], "topic_name");
        pa = new PyAssign(getCFG(), getLocation(), aig, getSubExpressions()[2]);
        result =  result.lub(pa.semantics(result, interprocedural, expressions));
        aig = new AccessInstanceGlobal(st.getCFG(), getLocation(), getSubExpressions()[0], "qos_profile");
        pa = new PyAssign(getCFG(), getLocation(), aig, getSubExpressions()[3]);
        result =  result.lub(pa.semantics(result, interprocedural, expressions));
        return result;
    }

    @Override
    public void setOriginatingStatement(Statement st) {
        this.st = st;
    }

}