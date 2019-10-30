//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package bsh.org.objectweb.asm;

public interface ClassVisitor {
    void visit(int var1, String var2, String var3, String[] var4, String var5);

    void visitInnerClass(String var1, String var2, String var3, int var4);

    void visitField(int var1, String var2, String var3, Object var4);

    CodeVisitor visitMethod(int var1, String var2, String var3, String[] var4);

    void visitEnd();
}
