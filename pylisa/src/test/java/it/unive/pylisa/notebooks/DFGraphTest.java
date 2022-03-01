package it.unive.pylisa.notebooks;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JacksonInject.Value;

import org.junit.Test;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.common.Int32;
import it.unive.lisa.type.common.StringType;
import it.unive.pylisa.analysis.dataframes.DFOrConstant;
import it.unive.pylisa.analysis.dataframes.transformation.DataframeGraphDomain;
import it.unive.pylisa.analysis.dataframes.transformation.graph.DataframeGraph;
import it.unive.pylisa.analysis.dataframes.transformation.graph.SimpleEdge;
import it.unive.pylisa.analysis.dataframes.transformation.operations.Concat;
import it.unive.pylisa.analysis.dataframes.transformation.operations.DataframeOperation;
import it.unive.pylisa.analysis.dataframes.transformation.operations.DropColumns;
import it.unive.pylisa.analysis.dataframes.transformation.operations.FilterNullRows;
import it.unive.pylisa.analysis.dataframes.transformation.operations.ReadFromFile;
import it.unive.pylisa.analysis.dataframes.transformation.operations.RowAccess;
import it.unive.pylisa.analysis.dataframes.transformation.operations.RowProjection;
import it.unive.pylisa.cfg.type.PyListType;
import it.unive.pylisa.libraries.pandas.types.PandasDataframeType;
import it.unive.pylisa.symbolic.operators.AccessRows;
import it.unive.pylisa.symbolic.operators.ConcatCols;
import it.unive.pylisa.symbolic.operators.ConcatRows;
import it.unive.pylisa.symbolic.operators.Drop;
import it.unive.pylisa.symbolic.operators.FilterNull;
import it.unive.pylisa.symbolic.operators.ProjectRows;
import it.unive.pylisa.symbolic.operators.ReadDataframe;

public class DFGraphTest {

	private final ProgramPoint fake;

	private final Variable df1;// , df2;

	private final ValueEnvironment<DFOrConstant> base;

	private final String fname;

	public DFGraphTest() throws SemanticException {
		ValueEnvironment<DFOrConstant> singleton = new ValueEnvironment<>(new DFOrConstant());

		fake = new ProgramPoint() {

			@Override
			public CodeLocation getLocation() {
				return SyntheticLocation.INSTANCE;
			}

			@Override
			public CFG getCFG() {
				return null;
			}
		};

		df1 = new Variable(PandasDataframeType.INSTANCE, "df1", SyntheticLocation.INSTANCE);
//		df2 = new Variable(PandasDataframeType.INSTANCE, "df2", SyntheticLocation.INSTANCE);

		fname = "foo.csv";

		DataframeGraphDomain elem = new DataframeGraphDomain(new ReadFromFile(fake.getLocation(), fname));
		DFOrConstant state = new DFOrConstant(elem);
		ValueEnvironment<DFOrConstant> env = singleton.putState(df1, state);
//		TODO build a more elaborate element here
//		elem = new DataframeGraphDomain(fname, df2Set, df2PSet, df2Map, df2Interval);
//		state = new DFOrConstant(elem);
//		env = env.putState(df2, state);
		base = env;
	}

	@Test
	public void testReadCsv() throws SemanticException {
		Constant filename = new Constant(StringType.INSTANCE, fname, SyntheticLocation.INSTANCE);
		UnaryExpression unary = new UnaryExpression(PandasDataframeType.INSTANCE, filename, ReadDataframe.INSTANCE,
				SyntheticLocation.INSTANCE);
		ValueEnvironment<DFOrConstant> sss = base.smallStepSemantics(unary, fake);
		DataframeGraphDomain stack = sss.getValueOnStack().df();
		DataframeGraphDomain expected = new DataframeGraphDomain(new ReadFromFile(fake.getLocation(), fname));

		assertEquals(expected, stack);
	}

	@Test
	public void testFilterNullRows() throws SemanticException {
		UnaryExpression unary = new UnaryExpression(PandasDataframeType.INSTANCE, df1, FilterNull.INSTANCE,
				SyntheticLocation.INSTANCE);
		ValueEnvironment<DFOrConstant> sss = base.smallStepSemantics(unary, fake);
		DataframeGraphDomain stack = sss.getValueOnStack().df();
		DataframeGraphDomain expected = new DataframeGraphDomain(base.getState(df1).df(),
				new FilterNullRows(fake.getLocation()));

		assertEquals(expected, stack);
	}

	@Test
	public void testAccessRows() throws SemanticException {
		TernaryExpression ternary = new TernaryExpression(PandasDataframeType.INSTANCE,
				df1,
				new Constant(Int32.INSTANCE, 0, SyntheticLocation.INSTANCE),
				new Constant(Int32.INSTANCE, 100, SyntheticLocation.INSTANCE),
				AccessRows.INSTANCE,
				SyntheticLocation.INSTANCE);
		ValueEnvironment<DFOrConstant> sss = base.smallStepSemantics(ternary, fake);
		DataframeGraphDomain stack = sss.getValueOnStack().df();
		DataframeGraphDomain expected = new DataframeGraphDomain(base.getState(df1).df(),
				new RowAccess(fake.getLocation(), new Interval(0, 100)));

		assertEquals(expected, stack);
	}

	@Test
	public void testDropColumns() throws SemanticException {

		ExpressionSet<Constant>[] cols = (ExpressionSet<Constant>[]) new ExpressionSet[2];
		cols[0] = new ExpressionSet<>(new Constant(StringType.INSTANCE, "col1", SyntheticLocation.INSTANCE));
		cols[1] = new ExpressionSet<>(new Constant(StringType.INSTANCE, "col2", SyntheticLocation.INSTANCE));

		BinaryExpression bin = new BinaryExpression(PandasDataframeType.INSTANCE, 
				df1, 
				new Constant(PyListType.INSTANCE, 
					cols, 
					SyntheticLocation.INSTANCE), 
				Drop.INSTANCE, SyntheticLocation.INSTANCE);

		ValueEnvironment<DFOrConstant> sss = base.smallStepSemantics(bin, fake);
		DataframeGraphDomain stack = sss.getValueOnStack().df();

		Set<String> colsShouldHaveAccessed = new HashSet<>();
		colsShouldHaveAccessed.add("col1");
		colsShouldHaveAccessed.add("col2");

		DataframeGraphDomain expected = new DataframeGraphDomain(base.getState(df1).df(), new DropColumns(colsShouldHaveAccessed));

		assertEquals(expected, stack);
	}

	@Test
	public void testConcatCols() throws SemanticException {
		Variable df2 = new Variable(PandasDataframeType.INSTANCE, "df2", SyntheticLocation.INSTANCE);
		String fname2 = "foo1.csv";

		DataframeGraphDomain df2GraphDomain = new DataframeGraphDomain(new ReadFromFile(fname2));
		df2GraphDomain = new DataframeGraphDomain(df2GraphDomain, new DropColumns(new HashSet<>()));

		ValueEnvironment<DFOrConstant> valEnv = base.putState(df2, new DFOrConstant(df2GraphDomain));
		BinaryExpression bin = new BinaryExpression(PandasDataframeType.INSTANCE, df1, df2, ConcatCols.INSTANCE, SyntheticLocation.INSTANCE);
		
		ValueEnvironment<DFOrConstant> sss = valEnv.smallStepSemantics(bin, fake);

		DataframeGraph concatGraph = new DataframeGraph();
		DataframeOperation rff1 = new ReadFromFile(fname);
		DataframeOperation rff2 = new ReadFromFile(fname2);
		DataframeOperation drop = new DropColumns(new HashSet<>());
		DataframeOperation concat = new Concat(Concat.Axis.CONCAT_COLS);

		concatGraph.addNode(rff1);
		concatGraph.addNode(rff2);
		concatGraph.addNode(drop);
		concatGraph.addNode(concat);

		concatGraph.addEdge(new SimpleEdge(rff1, concat, 0));
		concatGraph.addEdge(new SimpleEdge(rff2, drop));
		concatGraph.addEdge(new SimpleEdge(drop, concat, 1));

		DataframeGraphDomain expected = new DataframeGraphDomain(concatGraph);
		DataframeGraphDomain stack = sss.getValueOnStack().df();

		assertEquals(expected, stack);
	}

	@Test
	public void testConcatRows() throws SemanticException {
		Variable df2 = new Variable(PandasDataframeType.INSTANCE, "df2", SyntheticLocation.INSTANCE);
		String fname2 = "foo1.csv";

		DataframeGraphDomain df2GraphDomain = new DataframeGraphDomain(new ReadFromFile(fname2));
		df2GraphDomain = new DataframeGraphDomain(df2GraphDomain, new DropColumns(new HashSet<>()));

		ValueEnvironment<DFOrConstant> valEnv = base.putState(df2, new DFOrConstant(df2GraphDomain));
		BinaryExpression bin = new BinaryExpression(PandasDataframeType.INSTANCE, df1, df2, ConcatRows.INSTANCE, SyntheticLocation.INSTANCE);
		
		ValueEnvironment<DFOrConstant> sss = valEnv.smallStepSemantics(bin, fake);

		DataframeGraph concatGraph = new DataframeGraph();
		DataframeOperation rff1 = new ReadFromFile(fname);
		DataframeOperation rff2 = new ReadFromFile(fname2);
		DataframeOperation drop = new DropColumns(new HashSet<>());
		DataframeOperation concat = new Concat(Concat.Axis.CONCAT_ROWS);

		concatGraph.addNode(rff1);
		concatGraph.addNode(rff2);
		concatGraph.addNode(drop);
		concatGraph.addNode(concat);

		concatGraph.addEdge(new SimpleEdge(rff1, concat, 0));
		concatGraph.addEdge(new SimpleEdge(rff2, drop));
		concatGraph.addEdge(new SimpleEdge(drop, concat, 1));

		DataframeGraphDomain expected = new DataframeGraphDomain(concatGraph);
		DataframeGraphDomain stack = sss.getValueOnStack().df();

		assertEquals(expected, stack);
	}

	@Test
	public void testRowProjection() throws SemanticException {
		TernaryExpression ternary = new TernaryExpression(PandasDataframeType.INSTANCE,
				df1,
				new Constant(Int32.INSTANCE, 0, SyntheticLocation.INSTANCE),
				new Constant(Int32.INSTANCE, 100, SyntheticLocation.INSTANCE),
				ProjectRows.INSTANCE,
				SyntheticLocation.INSTANCE);
		ValueEnvironment<DFOrConstant> sss = base.smallStepSemantics(ternary, fake);
		DataframeGraphDomain stack = sss.getValueOnStack().df();
		DataframeGraphDomain expected = new DataframeGraphDomain(base.getState(df1).df(),
				new RowProjection(fake.getLocation(), new Interval(0, 100)));

		assertEquals(expected, stack);
	}

}
