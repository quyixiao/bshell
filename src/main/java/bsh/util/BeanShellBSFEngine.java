//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package bsh.util;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.Primitive;
import bsh.TargetError;
import bsh.This;
import java.util.Vector;
import org.apache.bsf.BSFDeclaredBean;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.bsf.util.BSFEngineImpl;

public class BeanShellBSFEngine extends BSFEngineImpl {
    Interpreter interpreter;
    boolean installedApplyMethod;
    static final String bsfApplyMethod = "_bsfApply( _bsfNames, _bsfArgs, _bsfText ) {for(i=0;i<_bsfNames.length;i++)this.namespace.setVariable(_bsfNames[i], _bsfArgs[i],false);return this.interpreter.eval(_bsfText, this.namespace);}";

    public BeanShellBSFEngine() {
    }

    public void initialize(BSFManager mgr, String lang, Vector declaredBeans) throws BSFException {
        super.initialize(mgr, lang, declaredBeans);
        this.interpreter = new Interpreter();

        try {
            this.interpreter.set("bsf", mgr);
        } catch (EvalError var6) {
            throw new BSFException("bsh internal error: " + var6.toString());
        }

        for(int i = 0; i < declaredBeans.size(); ++i) {
            BSFDeclaredBean bean = (BSFDeclaredBean)declaredBeans.get(i);
            this.declareBean(bean);
        }

    }

    public void setDebug(boolean debug) {
        Interpreter var10000 = this.interpreter;
        Interpreter.DEBUG = debug;
    }

    public Object call(Object object, String name, Object[] args) throws BSFException {
        if (object == null) {
            try {
                object = this.interpreter.get("global");
            } catch (EvalError var8) {
                throw new BSFException("bsh internal error: " + var8.toString());
            }
        }

        if (object instanceof This) {
            try {
                Object value = ((This)object).invokeMethod(name, args);
                return Primitive.unwrap(value);
            } catch (InterpreterError var5) {
                throw new BSFException("BeanShell interpreter internal error: " + var5);
            } catch (TargetError var6) {
                throw new BSFException("The application script threw an exception: " + var6.getTarget());
            } catch (EvalError var7) {
                throw new BSFException("BeanShell script error: " + var7);
            }
        } else {
            throw new BSFException("Cannot invoke method: " + name + ". Object: " + object + " is not a BeanShell scripted object.");
        }
    }

    public Object apply(String source, int lineNo, int columnNo, Object funcBody, Vector namesVec, Vector argsVec) throws BSFException {
        if (namesVec.size() != argsVec.size()) {
            throw new BSFException("number of params/names mismatch");
        } else if (!(funcBody instanceof String)) {
            throw new BSFException("apply: functino body must be a string");
        } else {
            String[] names = new String[namesVec.size()];
            namesVec.copyInto(names);
            Object[] args = new Object[argsVec.size()];
            argsVec.copyInto(args);

            try {
                if (!this.installedApplyMethod) {
                    this.interpreter.eval("_bsfApply( _bsfNames, _bsfArgs, _bsfText ) {for(i=0;i<_bsfNames.length;i++)this.namespace.setVariable(_bsfNames[i], _bsfArgs[i],false);return this.interpreter.eval(_bsfText, this.namespace);}");
                    this.installedApplyMethod = true;
                }

                This global = (This)this.interpreter.get("global");
                Object value = global.invokeMethod("_bsfApply", new Object[]{names, args, (String)funcBody});
                return Primitive.unwrap(value);
            } catch (InterpreterError var11) {
                throw new BSFException("BeanShell interpreter internal error: " + var11 + this.sourceInfo(source, lineNo, columnNo));
            } catch (TargetError var12) {
                throw new BSFException("The application script threw an exception: " + var12.getTarget() + this.sourceInfo(source, lineNo, columnNo));
            } catch (EvalError var13) {
                throw new BSFException("BeanShell script error: " + var13 + this.sourceInfo(source, lineNo, columnNo));
            }
        }
    }

    public Object eval(String source, int lineNo, int columnNo, Object expr) throws BSFException {
        if (!(expr instanceof String)) {
            throw new BSFException("BeanShell expression must be a string");
        } else {
            try {
                return this.interpreter.eval((String)expr);
            } catch (InterpreterError var6) {
                throw new BSFException("BeanShell interpreter internal error: " + var6 + this.sourceInfo(source, lineNo, columnNo));
            } catch (TargetError var7) {
                throw new BSFException("The application script threw an exception: " + var7.getTarget() + this.sourceInfo(source, lineNo, columnNo));
            } catch (EvalError var8) {
                throw new BSFException("BeanShell script error: " + var8 + this.sourceInfo(source, lineNo, columnNo));
            }
        }
    }

    public void exec(String source, int lineNo, int columnNo, Object script) throws BSFException {
        this.eval(source, lineNo, columnNo, script);
    }

    public void declareBean(BSFDeclaredBean bean) throws BSFException {
        try {
            this.interpreter.set(bean.name, bean.bean);
        } catch (EvalError var3) {
            throw new BSFException("error declaring bean: " + bean.name + " : " + var3.toString());
        }
    }

    public void undeclareBean(BSFDeclaredBean bean) throws BSFException {
        try {
            this.interpreter.unset(bean.name);
        } catch (EvalError var3) {
            throw new BSFException("bsh internal error: " + var3.toString());
        }
    }

    public void terminate() {
    }

    private String sourceInfo(String source, int lineNo, int columnNo) {
        return " BSF info: " + source + " at line: " + lineNo + " column: columnNo";
    }
}
