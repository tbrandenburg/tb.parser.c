package tb.parser.c;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.parser.*;
import org.json.JSONObject;

public class CParser {

	private static IASTTranslationUnit parse(String filePath, char[] code) throws Exception {
		FileContent fc = FileContent.create(filePath, code);
		Map<String, String> macroDefinitions = new HashMap<String, String>();
		String[] includeSearchPaths = new String[0];
		IScannerInfo si = new ScannerInfo(macroDefinitions, includeSearchPaths);
		IncludeFileContentProvider ifcp = IncludeFileContentProvider.getEmptyFilesProvider();
		IIndex idx = null;
		int options = ILanguage.OPTION_IS_SOURCE_UNIT & ILanguage.OPTION_ADD_COMMENTS;
		IParserLogService log = new DefaultLogService();
		return GPPLanguage.getDefault().getASTTranslationUnit(fc, si, ifcp, idx, options, log);
	}

	private static void printComments(IASTTranslationUnit tu) {
		List<IASTComment> comments = Arrays.asList(tu.getComments());
		for (IASTComment comment : comments) {
			System.out.print(comment.getComment());
			System.out.println(" at line: " + comment.getFileLocation().getStartingLineNumber());
		}
	}

	private static void printPreprocessorStatements(IASTTranslationUnit tu) {
		List<IASTPreprocessorStatement> preprocs = Arrays.asList(tu.getAllPreprocessorStatements());
		for (IASTPreprocessorStatement preproc : preprocs) {
			System.out.print(preproc.toString());
			System.out.println(" at line: " + preproc.getFileLocation().getStartingLineNumber());
		}
	}


	private static void printASTNames(IASTTranslationUnit tu) {
		ASTVisitor visitor = new ASTVisitor() {
			@Override public int visit(IASTName name) {
				if(name.isDeclaration()) {
					IBinding b = name.resolveBinding();
					if(b instanceof IFunction) {
						if(((IFunction) b).isAuto()) {
							System.out.print("auto ");
						}
						if(((IFunction) b).isExtern()) {
							System.out.print("extern ");
						}
						if(((IFunction) b).isStatic()) {
							System.out.print("static ");
						}
						if(((IFunction) b).isRegister()) {
							System.out.print("register ");
						}
						if(((IFunction) b).isInline()) {
							System.out.print("inline ");
						}
						System.out.print(ASTTypeUtil.getType(((IFunction) b).getType().getReturnType()) + " ");
						System.out.print(name);
						System.out.print("(");
						boolean first = true;
						for(IType paramType : ((IFunction) b).getType().getParameterTypes()) {
							if(!first) {
								System.out.print(", ");
							}
							first = false;
							System.out.print(ASTTypeUtil.getType(paramType));
						}
						System.out.print(")");
						System.out.println(" at line: " + name.getFileLocation().getStartingLineNumber());
					}
					if((b instanceof IVariable) && !(b instanceof IParameter)) {
						if(((IVariable) b).isAuto()) {
							System.out.print("auto ");
						}
						if(((IVariable) b).isExtern()) {
							System.out.print("extern ");
						}
						if(((IVariable) b).isStatic()) {
							System.out.print("static ");
						}
						if(((IVariable) b).isRegister()) {
							System.out.print("register ");
						}
						System.out.print(ASTTypeUtil.getType(((IVariable) b).getType()) + " ");
						System.out.print(name);
						System.out.println(" at line: " + name.getFileLocation().getStartingLineNumber());
					}

				}
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};

		visitor.shouldVisitNames = true;

		tu.accept(visitor);
	}


	private static void printChildren(IASTNode node, String prefix) {
		System.out.println(prefix + node.getClass().getSimpleName());
		List<IASTNode> nodes = Arrays.asList(node.getChildren());
		for (IASTNode curNode : nodes) {
			printChildren(curNode, prefix + "  ");
		}
	}

	private static JSONObject toJSON(IASTTranslationUnit tu) {
		JSONObject json = new JSONObject();

		/* Initialize dictionary for variables and functions */
		json.put("variables",new JSONObject());
		json.put("functions",new JSONObject());

		ASTVisitor visitor = new ASTVisitor() {
			@Override public int visit(IASTName name) {
				if(name.isDeclaration()) {
					IBinding b = name.resolveBinding();
					if(b instanceof IFunction) {
						/* Get functions dictionary */
						JSONObject functions = json.getJSONObject("functions");
						JSONObject functionDecl = new JSONObject();
						functionDecl.put("return",ASTTypeUtil.getType(((IFunction) b).getType().getReturnType()));
						JSONObject parameters = new JSONObject();
						int i = 0;
						for(IType paramType : ((IFunction) b).getType().getParameterTypes()) {
							parameters.put(String.valueOf(i), ASTTypeUtil.getType(paramType));
							i++;
						}
						if(parameters.length()>0) {
							functionDecl.put("parameters", parameters);
						}
						functionDecl.put("line", name.getFileLocation().getStartingLineNumber());
						functions.put(name.toString(), functionDecl);
					}
					if((b instanceof IVariable) && !(b instanceof IParameter)) {
						/* Get variable dictionary */
						JSONObject variables = json.getJSONObject("variables");
						JSONObject varDecl = new JSONObject();
						varDecl.put("type",ASTTypeUtil.getType(((IVariable) b).getType()));
						varDecl.put("line", name.getFileLocation().getStartingLineNumber());
						variables.put(name.toString(), varDecl);
					}

				}
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};

		visitor.shouldVisitNames = true;

		tu.accept(visitor);
		
		return json;
	}


	public static void main(String[] args) {
		String code = "";
		if(args.length>0) {
			JSONObject files = new JSONObject();
			for(String arg : args) {
				File f = new File(arg);
				if(f.exists() && !f.isDirectory()) { 
					/* Read file */
					try
					{
						byte[] bytes = Files.readAllBytes(Paths.get(arg));
						code = new String (bytes);
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
					}

					/* Get AST */
					IASTTranslationUnit translationUnit = null;
					try {
						translationUnit = parse(arg,code.toCharArray());

						//printComments(translationUnit);

						//printPreprocessorStatements(translationUnit);

						//printChildren(translationUnit,"");

						//printASTNames(translationUnit);
						
						files.put(arg, toJSON(translationUnit));

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			if (files.length()>1) {
				System.out.println(files.toString(4));
			} else {
				System.out.println(((JSONObject)files.get(args[0])).toString(4));
			}
		} else {
			System.out.println("Usage:");
			System.out.println("  java -jar CParser.jar [Files]");
		}
	}

}
