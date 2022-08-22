package it.unive.pylisa.symbolic.operators.compare;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.common.BoolType;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

public class PyComparisonGt implements ComparisonOperator, BinaryOperator {

	public static final PyComparisonGt INSTANCE = new PyComparisonGt();

	private PyComparisonGt() {
	}

	@Override
	public String toString() {
		return ">";
	}

	@Override
	public ComparisonOperator opposite() {
		return PyComparisonLe.INSTANCE;
	}

	@Override
	public ExternalSet<Type> typeInference(ExternalSet<Type> left, ExternalSet<Type> right) {
		return Caches.types().mkSingletonSet(BoolType.INSTANCE);
	}
}
