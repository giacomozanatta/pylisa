package it.unive.pylisa.symbolic.operators.compare;

import java.util.Collections;
import java.util.Set;

import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.common.BoolType;

public class PyComparisonGe implements ComparisonOperator, BinaryOperator {

	public static final PyComparisonGe INSTANCE = new PyComparisonGe();

	private PyComparisonGe() {
	}

	@Override
	public String toString() {
		return ">=";
	}

	@Override
	public ComparisonOperator opposite() {
		return PyComparisonLt.INSTANCE;
	}

	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> right) {
		return Collections.singleton(BoolType.INSTANCE);
	}
}
