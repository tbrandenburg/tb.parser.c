package tb.parser.c;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTTypeUtil;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.IParameter;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.json.JSONObject;

import py4j.GatewayServer;

public class CParser {

    IASTTranslationUnit tu = null;

    public IASTTranslationUnit parse(String filePath, String code) throws Exception {
        FileContent fc = FileContent.create(filePath, code.toCharArray());
        Map<String, String> macroDefinitions = new HashMap<String, String>();
        String[] includeSearchPaths = new String[0];
        IScannerInfo si = new ScannerInfo(macroDefinitions, includeSearchPaths);
        IncludeFileContentProvider ifcp = IncludeFileContentProvider.getEmptyFilesProvider();
        IIndex idx = null;
        @SuppressWarnings("deprecation")
		int options = ILanguage.OPTION_IS_SOURCE_UNIT & ILanguage.OPTION_ADD_COMMENTS;
        IParserLogService log = new DefaultLogService();
        tu = GPPLanguage.getDefault().getASTTranslationUnit(fc, si, ifcp, idx, options, log);
        return tu;
    }

    public void printComments() {
        if(tu != null) {
            List<IASTComment> comments = Arrays.asList(tu.getComments());
            for (IASTComment comment : comments) {
                System.out.print(comment.getComment());
                System.out.println(" at line: " + comment.getFileLocation().getStartingLineNumber());
            }
        }
    }

    public void printPreprocessorStatements() {
        if(tu != null) {
            List<IASTPreprocessorStatement> preprocs = Arrays.asList(tu.getAllPreprocessorStatements());
            for (IASTPreprocessorStatement preproc : preprocs) {
                System.out.print(preproc.toString());
                System.out.println(" at line: " + preproc.getFileLocation().getStartingLineNumber());
            }
        }
    }


    public void printASTNames() {
        if(tu != null) {
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
    }


    public void printChildren(IASTNode node, String prefix) {
        if(tu != null) {
            System.out.println(prefix + node.getClass().getSimpleName());
            List<IASTNode> nodes = Arrays.asList(node.getChildren());
            for (IASTNode curNode : nodes) {
                printChildren(curNode, prefix + "  ");
            }
        }
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        if(tu != null) {
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
        }

        return json;
    }
    
    public String getSomething() {
    	return "Test";
    }

    /**
     * Usage:
     *   AST to console:
     *     java -jar CParser.jar [Files]
     *   Start py4j gateway to CParser instance:
     *     java -jar CParser.jar
     *     
     * @param args
     */
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
                    CParser cp = new CParser();
                    try {
                        cp.parse(arg,code);

                        files.put(arg, cp.toJSON());
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
            GatewayServer gatewayServer = new GatewayServer(new CParser());
            gatewayServer.start();
            System.out.println("[CParser] py4j gateway to CParser instance started!");
        }
    }

}
