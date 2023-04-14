package it.unive.pylisa.libraries;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.lang3.tuple.Pair;

import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.literal.FalseLiteral;
import it.unive.lisa.program.cfg.statement.literal.Int32Literal;
import it.unive.lisa.program.cfg.statement.literal.StringLiteral;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import it.unive.lisa.type.Type;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.pylisa.PythonFeatures;
import it.unive.pylisa.PythonTypeSystem;
import it.unive.pylisa.antlr.LibraryDefinitionLexer;
import it.unive.pylisa.antlr.LibraryDefinitionParser;
import it.unive.pylisa.antlr.LibraryDefinitionParser.ClassDefContext;
import it.unive.pylisa.antlr.LibraryDefinitionParser.FieldContext;
import it.unive.pylisa.antlr.LibraryDefinitionParser.FileContext;
import it.unive.pylisa.antlr.LibraryDefinitionParser.LibraryContext;
import it.unive.pylisa.antlr.LibraryDefinitionParser.LibtypeContext;
import it.unive.pylisa.antlr.LibraryDefinitionParser.LisatypeContext;
import it.unive.pylisa.antlr.LibraryDefinitionParser.MethodContext;
import it.unive.pylisa.antlr.LibraryDefinitionParser.ParamContext;
import it.unive.pylisa.antlr.LibraryDefinitionParser.TypeContext;
import it.unive.pylisa.antlr.LibraryDefinitionParserBaseVisitor;
import it.unive.pylisa.cfg.expression.NoneLiteral;
import it.unive.pylisa.cfg.type.PyClassType;

public class LibrarySpecificationParser extends LibraryDefinitionParserBaseVisitor<Object> {

	private final String file;
	private final Program program;

	private Map<String, ClassUnit> parsed;
	private Collection<Pair<ClassUnit, ClassUnit>> toType;

	ClassUnit root;
	private Unit library, clazz, fieldContainer;
	private CodeLocation location;
	private CFG init;

	private boolean typedefsOnly;

	public LibrarySpecificationParser(String file, Program program) {
		this.file = file;
		this.program = program;
	}

	private void makeInit() {
		if (init != null)
			return;

		init = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, program, false, "LiSA$init"));
		init.addNode(new Ret(init, SyntheticLocation.INSTANCE), true);
		program.addCodeMember(init);
	}

	@Override
	public CompilationUnit visitClassDef(ClassDefContext ctx) {
		String name = ctx.name.getText();
		ClassUnit unit = typedefsOnly ? new ClassUnit(location, program, name, ctx.SEALED() != null)
				: parsed.get(name);
		if (typedefsOnly && ctx.ROOT() != null)
			if (root != null)
				throw new IllegalStateException("More than one root class defined as hierarchy root");
			else
				root = unit;

		if (!typedefsOnly) {
			clazz = unit;
			if (ctx.base != null)
				unit.addAncestor(parsed.get(ctx.base.getText()));
			else if (root != null && unit != root)
				unit.addAncestor(root);

			for (MethodContext mtd : ctx.method()) {
				NativeCFG construct = visitMethod(mtd);
				if (construct.getDescriptor().isInstance())
					unit.addInstanceCodeMember(construct);
				else
					unit.addCodeMember(construct);
			}

			fieldContainer = clazz;
			for (FieldContext fld : ctx.field()) {
				Global field = visitField(fld);
				if (fld.INSTANCE() != null)
					unit.addInstanceGlobal(field);
				else
					unit.addGlobal(field);
			}

			clazz = null;
		} else {
			parsed.put(name, unit);
			toType.add(library == program ? Pair.of(null, unit) : Pair.of((ClassUnit) library, unit));
		}

		return unit;
	}

	@Override
	public Global visitField(FieldContext ctx) {
		Type type = visitType(ctx.type());
		String name = ctx.name.getText();
		return new Global(location, fieldContainer, name, ctx.INSTANCE() != null, type);
	}

	@Override
	public Parameter visitParam(ParamContext ctx) {
		Type type = visitType(ctx.type());
		String name = ctx.name.getText();
		if (ctx.DEFAULT() == null)
			return new Parameter(location, name, type);

		makeInit();
		Expression def;
		if (ctx.val.NONE() != null)
			def = new NoneLiteral(init, location);
		else if (type == BoolType.INSTANCE)
			if (ctx.val.BOOLEAN().getText().equals("true"))
				def = new TrueLiteral(init, location);
			else
				def = new FalseLiteral(init, location);
		else if (type == StringType.INSTANCE)
			def = new StringLiteral(init, location, clean(ctx.val.STRING().getText()));
		else if (type == Int32Type.INSTANCE)
			def = new Int32Literal(init, location, Integer.parseInt(ctx.val.NUMBER().getText()));
		else
			throw new LibraryParsingException(file, "Unsupported default parameter type: " + type);

		return new Parameter(location, name, type, def, new Annotations());
	}

	private String clean(String text) {
		return text.substring(1, text.length() - 1);
	}

	@Override
	public Type visitType(TypeContext ctx) {
		if (ctx.libtype() != null)
			return visitLibtype(ctx.libtype());
		else
			return visitLisatype(ctx.lisatype());
	}

	@Override
	public Type visitLibtype(LibtypeContext ctx) {
		Type t = PyClassType.lookup(ctx.type_name.getText());
		if (ctx.STAR() != null)
			t = ((PyClassType) t).getReference();
		return t;
	}

	@Override
	public Type visitLisatype(LisatypeContext ctx) {
		try {
			Class<?> type = Class.forName(ctx.type_name.getText());
			Field field = type.getField(ctx.type_field.getText());
			return (Type) field.get(null);
		} catch (ClassNotFoundException
				| NoSuchFieldException
				| SecurityException
				| IllegalArgumentException
				| IllegalAccessException e) {
			throw new LibraryParsingException(file, e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public NativeCFG visitMethod(MethodContext ctx) {
		Parameter[] pars = new Parameter[ctx.param().size()];
		for (int i = 0; i < pars.length; i++)
			pars[i] = visitParam(ctx.param(i));

		CodeMemberDescriptor desc = new CodeMemberDescriptor(
				location,
				clazz == null ? library : clazz,
				ctx.INSTANCE() != null,
				ctx.name.getText(),
				visitType(ctx.type()),
				pars);

		desc.setOverridable(ctx.SEALED() != null);

		try {
			return new NativeCFG(desc, (Class<? extends NaryExpression>) Class.forName(ctx.implementation.getText()));
		} catch (ClassNotFoundException e) {
			throw new LibraryParsingException(file, e);
		}
	}

	@Override
	public CompilationUnit visitLibrary(LibraryContext ctx) {
		location = new SourceCodeLocation(ctx.loc.getText(), 0, 0);
		String name = ctx.name.getText();
		ClassUnit unit = typedefsOnly ? new ClassUnit(location, program, name, false) : parsed.get(name);
		library = unit;

		if (!typedefsOnly) {
			for (MethodContext mtd : ctx.method())
				unit.addCodeMember(visitMethod(mtd));

			fieldContainer = library;
			for (FieldContext fld : ctx.field())
				unit.addGlobal(visitField(fld));
		}

		for (ClassDefContext cls : ctx.classDef()) {
			CompilationUnit c = visitClassDef(cls);
			if (typedefsOnly)
				// we add it only in the first pass
				program.addUnit(c);
		}

		if (typedefsOnly) {
			program.addUnit(unit);
			parsed.put(name, unit);
		}

		library = null;
		location = null;
		return unit;
	}

	@Override
	public Collection<ClassUnit> visitFile(FileContext ctx) {
		parsed = new HashMap<>();
		toType = new HashSet<>();

		// only parse type definitions
		typedefsOnly = true;

		// setup for the elements that will go directly into the program
		location = new SourceCodeLocation("standard_library", 0, 0);
		clazz = program;
		library = program;

		for (ClassDefContext cls : ctx.classDef())
			program.addUnit(visitClassDef(cls));
		for (LibraryContext lib : ctx.library())
			visitLibrary(lib);

		// generate types
		for (Pair<ClassUnit, ClassUnit> pair : toType)
			if (pair.getLeft() == null)
				PyClassType.lookup(pair.getRight().getName(), pair.getRight());
			else
				// registering is a side effect of the constructor
				new PyLibraryUnitType(pair.getLeft(), pair.getRight());

		// parse signatures
		typedefsOnly = false;

		// setup for the elements that will go directly into the program
		location = new SourceCodeLocation("standard_library", 0, 0);
		clazz = program;
		library = program;

		for (MethodContext mtd : ctx.method())
			program.addCodeMember(visitMethod(mtd));
		fieldContainer = program;
		for (FieldContext lfd : ctx.field())
			program.addGlobal(visitField(lfd));
		for (ClassDefContext cls : ctx.classDef())
			visitClassDef(cls);

		location = null;
		clazz = null;
		library = null;

		for (LibraryContext lib : ctx.library())
			visitLibrary(lib);

		return parsed.values();
	}

	public Collection<Pair<ClassUnit, ClassUnit>> getToType() {
		return toType;
	}

	public static class LibraryParsingException extends RuntimeException {

		private static final long serialVersionUID = 719012473263650111L;

		public LibraryParsingException(String file, String reason) {
			super("Error while parsing library definitions from " + file + ": " + reason);
		}

		public LibraryParsingException(String file, Throwable cause) {
			super("Error while parsing library definitions from " + file, cause);
		}
	}

	public static void main(String[] args) throws IOException {
		String file = "/libs.txt";

		LibraryDefinitionLexer lexer = null;
		try (InputStream stream = LibrarySpecificationParser.class.getResourceAsStream(file);) {
			lexer = new LibraryDefinitionLexer(CharStreams.fromStream(stream, StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IOException("Unable to parse '" + file + "'", e);
		}

		Program program = new Program(new PythonFeatures(), new PythonTypeSystem());
		LibraryDefinitionParser parser = new LibraryDefinitionParser(new CommonTokenStream(lexer));
		new LibrarySpecificationParser(file, program).visitFile(parser.file());
		dump(program);
	}

	private static void dump(Program program) {
		System.out.println(program.getName());
		for (Global global : program.getGlobals())
			System.out.println("\t" + global.getName());
		for (CodeMember cfg : program.getCodeMembers())
			System.out.println("\t" + cfg.getDescriptor().getFullSignatureWithParNames());
		for (Unit unit : program.getUnits())
			dump(unit, 1);
	}

	private static void dump(Unit unit, int indent) {
		System.out.println("\t".repeat(indent) + unit.getName());
		for (Global global : unit.getGlobals())
			System.out.println("\t".repeat(indent + 1) + global.getName());
		if (unit instanceof CompilationUnit)
			for (Global global : ((CompilationUnit) unit).getInstanceGlobals(false))
				System.out.println("\t".repeat(indent + 1) + "instance " + global.getName());
		for (CodeMember cm : unit.getCodeMembers())
			System.out.println("\t".repeat(indent + 1) + cm.getDescriptor().getFullSignatureWithParNames());
		if (unit instanceof CompilationUnit)
			for (CodeMember cm : ((CompilationUnit) unit).getInstanceCodeMembers(false))
				System.out.println(
						"\t".repeat(indent + 1) + "instance " + cm.getDescriptor().getFullSignatureWithParNames());
	}
}
