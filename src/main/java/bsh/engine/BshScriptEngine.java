//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package bsh.engine;

import bsh.*;

import javax.script.*;
import java.io.*;
import java.util.Map;

public class BshScriptEngine extends AbstractScriptEngine implements Compilable, Invocable {
    static final String engineNameSpaceKey = "org_beanshell_engine_namespace";
    private BshScriptEngineFactory factory;
    private Interpreter interpreter;

    public BshScriptEngine() {
        this((BshScriptEngineFactory) null);
    }

    public BshScriptEngine(BshScriptEngineFactory factory) {
        this.factory = factory;
        this.getInterpreter();
    }

    protected Interpreter getInterpreter() {
        if (this.interpreter == null) {
            this.interpreter = new Interpreter();
            this.interpreter.setNameSpace((NameSpace) null);
        }

        return this.interpreter;
    }

    public Object eval(String script, ScriptContext scriptContext) throws ScriptException {
        return this.evalSource(script, scriptContext);
    }

    public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
        return this.evalSource(reader, scriptContext);
    }

    private Object evalSource(Object source, ScriptContext scriptContext) throws ScriptException {
        NameSpace contextNameSpace = getEngineNameSpace(scriptContext);
        Interpreter bsh = this.getInterpreter();
        bsh.setNameSpace(contextNameSpace);
        bsh.setOut(new PrintStream(new BshScriptEngine.WriterOutputStream(scriptContext.getWriter())));
        bsh.setErr(new PrintStream(new BshScriptEngine.WriterOutputStream(scriptContext.getErrorWriter())));

        try {
            return source instanceof Reader ? bsh.eval((Reader) source) : bsh.eval((String) source);
        } catch (ParseException var7) {
            throw new ScriptException(var7.toString(), var7.getErrorSourceFile(), var7.getErrorLineNumber());
        } catch (TargetError var8) {
            ScriptException se = new ScriptException(var8.toString(), var8.getErrorSourceFile(), var8.getErrorLineNumber());
            se.initCause(var8.getTarget());
            throw se;
        } catch (EvalError var9) {
            throw new ScriptException(var9.toString(), var9.getErrorSourceFile(), var9.getErrorLineNumber());
        } catch (InterpreterError var10) {
            throw new ScriptException(var10.toString());
        }
    }

    private static NameSpace getEngineNameSpace(ScriptContext scriptContext) {
        NameSpace ns = (NameSpace) scriptContext.getAttribute("org_beanshell_engine_namespace", 100);
        if (ns == null) {
            Map engineView = new ScriptContextEngineView(scriptContext);
            ns = new ExternalNameSpace((NameSpace) null, "javax_script_context", engineView);
            scriptContext.setAttribute("org_beanshell_engine_namespace", ns, 100);
        }

        return (NameSpace) ns;
    }

    public Bindings createBindings() {
        return new SimpleBindings();
    }

    public ScriptEngineFactory getFactory() {
        if (this.factory == null) {
            this.factory = new BshScriptEngineFactory();
        }

        return this.factory;
    }

    public CompiledScript compile(String script) throws ScriptException {
        return this.compile((Reader) (new StringReader(script)));
    }

    public CompiledScript compile(Reader script) throws ScriptException {
        throw new Error("unimplemented");
    }

    public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
        if (!(thiz instanceof This)) {
            throw new ScriptException("Illegal objec type: " + thiz.getClass());
        } else {
            This bshObject = (This) thiz;

            try {
                return bshObject.invokeMethod(name, args);
            } catch (ParseException var7) {
                throw new ScriptException(var7.toString(), var7.getErrorSourceFile(), var7.getErrorLineNumber());
            } catch (TargetError var8) {
                ScriptException se = new ScriptException(var8.toString(), var8.getErrorSourceFile(), var8.getErrorLineNumber());
                se.initCause(var8.getTarget());
                throw se;
            } catch (EvalError var9) {
                throw new ScriptException(var9.toString(), var9.getErrorSourceFile(), var9.getErrorLineNumber());
            } catch (InterpreterError var10) {
                throw new ScriptException(var10.toString());
            }
        }
    }

    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        return this.invokeMethod(this.getGlobal(), name, args);
    }

    public <T> T getInterface(Class<T> clasz) {
        try {
            return (T) this.getGlobal().getInterface(clasz);
        } catch (UtilEvalError var3) {
            var3.printStackTrace();
            return null;
        }
    }

    public <T> T getInterface(Object thiz, Class<T> clasz) {
        if (!(thiz instanceof This)) {
            throw new IllegalArgumentException("invalid object type: " + thiz.getClass());
        } else {
            try {
                This bshThis = (This) thiz;
                return (T) bshThis.getInterface(clasz);
            } catch (UtilEvalError var4) {
                var4.printStackTrace(System.err);
                return null;
            }
        }
    }

    private This getGlobal() {
        return getEngineNameSpace(this.getContext()).getThis(this.getInterpreter());
    }

    class WriterOutputStream extends OutputStream {
        Writer writer;

        WriterOutputStream(Writer writer) {
            this.writer = writer;
        }

        public void write(int b) throws IOException {
            this.writer.write(b);
        }

        public void flush() throws IOException {
            this.writer.flush();
        }

        public void close() throws IOException {
            this.writer.close();
        }
    }
}
