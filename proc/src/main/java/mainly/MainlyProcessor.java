package mainly;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.annotation.processing.Filer;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.element.Element;

import static javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import com.google.auto.service.AutoService;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

@AutoService(Processor.class)
@SupportedAnnotationTypes("mainly.*")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class MainlyProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annos,
                        RoundEnvironment round) {
        for (TypeElement anno : annos) {
            Set<? extends Element> annotatedElements 
                = round.getElementsAnnotatedWith(anno);
            log("Checking " + annotatedElements);
            for (Element elem : annotatedElements) {
                generate(elem, round);
            }
        }
        return true;
    }

    private void log(String msg) {
        processingEnv.getMessager().printMessage(Kind.MANDATORY_WARNING, msg);
    } 

    private void generate(Element elem, RoundEnvironment round) {
        try {
            Main main = elem.getAnnotation(Main.class);
            if (main == null) {
                return;
            }
            String fq = main.value();
            int dot = fq.lastIndexOf(".");
            String pkgName = fq.substring(0, dot);
            String className = fq.substring(dot+1);
            Filer filer = processingEnv.getFiler();
            JavaFileObject file = filer.createSourceFile(fq, elem);
            Writer w = file.openWriter();
            
            write(w, 0, "package ", pkgName, ";\n\n");
            write(w, 0, "import java.util.LinkedList;\n\n");
            write(w, 0, "import java.util.function.Consumer;\n\n");
            
            write(w, 0, "public class ", className, " {\n\n");

            String rootType = elem.getSimpleName().toString();

            write(w, 4, "public static void main(String[] argv) {\n");
            write(w, 8, "final ", rootType, " root = new ", rootType, "();\n");

            writeReducer(w, elem, "root");

            write(w, 8, "root.run();\n");
            write(w, 4, "}\n");
            write(w, 0, "}\n").close();
        } catch (IOException iox) {
            throw new RuntimeException(iox);
        }
    }

    private Writer write(Writer w, int indent, String... ss) throws IOException {        
        for (int i=0;i<indent;i++) {
            w.append(" ");
        }
        for (String s : ss) {
            w.append(s);
        }
        return w;
    }

    class Flagged {
        Flag flag;
        Element element;
        String name;
        Flagged(Flag flag, Element element) {
            this.flag = flag;
            this.element = element;
            this.name = element.getSimpleName().toString();
        }
        
        String assignment() {
            return assign(element);
        }
        
        void assigner(Writer w, int indent, String var) throws IOException {
            write(w, indent, "(String a) -> {",
                             var, ".", name, 
                             assignment(), ";}");
        }
    }

    private void writeReducer(Writer w, Element elem, String var) throws IOException {
    
        List<Flagged> namedFlags = new ArrayList<>();
        List<Flagged> orderedFlags = new LinkedList<>();
        for (Element ee : elem.getEnclosedElements()) {
            Flag flag = ee.getAnnotation(Flag.class);
            if (flag == null) {
                continue;
            }
            if (flag.names().length > 0) {
                namedFlags.add(new Flagged(flag, ee));
            } else {
                orderedFlags.add(new Flagged(flag, ee));    
            }
        }
        
        orderedFlags.sort((a,b) -> a.flag.order() - b.flag.order());
        write(w, 8, "LinkedList<Consumer<String>> stack = new LinkedList<>();\n");
        for (Flagged flagged : orderedFlags) {
            write(w, 8, "stack.addLast(");
            flagged.assigner(w, 0, var);
            write(w, 0, ");\n ");
        }

        write(w, 8, "String expectingSlot = null;\n"); 
        write(w, 8, "for(int i=0;i<argv.length;i++) {\n");
        write(w, 12, "String arg = argv[i];\n");
        write(w, 12, "switch (arg) {\n");
        for (Flagged flagged : namedFlags) {
            for (String name : flagged.flag.names()) {
                write(w, 16, "case \"", name, "\": \n");                          
            }
            write(w, 20, "if (expectingSlot != null) {\n");
            write(w, 24, "System.out.println(\"'", flagged.name, "' is a flag, will not apply as argument to other flag '\" + expectingSlot + \"'\");\n");
            write(w, 24, "System.exit(1);\n");
            write(w, 20, "}\n");
            write(w, 20, "stack.push(");
            flagged.assigner(w, 0, var);
            write(w, 0, ");\n");
            write(w, 20, "expectingSlot = \"", flagged.name, "\";\n");
            write(w, 20, "break;\n");
        }
        write(w, 16, "default: \n"); //pop callable
        write(w, 20, "stack.pop().accept(arg);\n");
        write(w, 20, "expectingSlot = null;\n");
        write(w, 20, "break;\n");
        write(w, 12, "}\n");
        write(w, 8, "}\n");
        write(w, 8, "if (expectingSlot != null) {\n");
        write(w, 12, "System.out.println(\"missing argument for parameter \" + expectingSlot);");
        write(w, 12, "System.exit(2);");
        write(w, 8, "}\n");
    }

    final static String JUST_ASSIGN = "= a";
    final static String PARSEINT_ASSIGN = " = Integer.parseInt(a)";
    final static String PARSEBOOL_ASSIGN = " = Boolean.parseBoolean(a)";
    
    String assign(Element ee) {
        String[] result = new String[1]; 
        TypeMirror typeMirror = ee.asType();
        typeMirror.accept(typeVisitor, result);
        if (result[0] != null) {
            return result[0];
        }
        return JUST_ASSIGN;
    }

    private static final TypeVisitor<Void,String[]> typeVisitor = new TypeVisitor<Void,String[]>() {
        
        @Override
        public Void visit(TypeMirror t, String[] p) {
            return null;
        }

        @Override
        public Void visit(TypeMirror t) {
            return null;
        }

        @Override
        public Void visitArray(ArrayType t, String[] p) {
            return null;
        }

        @Override
        public Void visitExecutable(ExecutableType t, String[] p) {
            return null;
        }

        @Override
        public Void visitDeclared(DeclaredType t, String[] p) {
            return null;
        }
        
        @Override
        public Void visitError(ErrorType t, String[] p) {
            return null;
        }
        @Override
        public Void visitIntersection(IntersectionType t, String[] p) {
            return null;
        }
                
        @Override
        public Void visitNoType(NoType t, String[] p) {
            return null;
        }

        @Override
        public Void visitNull(NullType t, String[] p) {
            return null;
        }
                
        @Override
        public Void visitPrimitive(PrimitiveType t, String[] p) {
            if (t.getKind() == TypeKind.INT) {
                p[0] = PARSEINT_ASSIGN;
            } else if (t.getKind() == TypeKind.BOOLEAN) {
                p[0] = PARSEBOOL_ASSIGN;
            }
            return null;
        }
                
        @Override
        public Void visitTypeVariable(TypeVariable t, String[] p) {
            return null;
        }
                
        @Override
        public Void visitUnion(UnionType t, String[] p) {
            return null;
        }
                
        @Override
        public Void visitUnknown(TypeMirror t, String[] p) {
            return null;
        }

        @Override
        public Void visitWildcard(WildcardType t, String[] p) {
            return null;
        }
    };
}