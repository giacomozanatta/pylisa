package it.unive.pylisa.symbolic.operators.compare;

import java.util.Collections;
import java.util.Set;

import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

public class PyComparisonLt implements ComparisonOperator, BinaryOperator {

	public static final PyComparisonLt INSTANCE = new PyComparisonLt();

	private PyComparisonLt() {
	}

	@Override
	public String toString() {
		return "<";
	}

	@Override
	public ComparisonOperator opposite() {
		return PyComparisonGe.INSTANCE;
	}

	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> right) {
		return Collections.singleton(BoolType.INSTANCE);
	}
}