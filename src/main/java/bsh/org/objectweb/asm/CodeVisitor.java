//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package bsh.org.objectweb.asm;

public interface CodeVisitor {
    void visitInsn(int var1);

    void visitIntInsn(int var1, int var2);

    void visitVarInsn(int var1, int var2);

    void visitTypeInsn(int var1, String var2);

    void visitFieldInsn(int var1, String var2, String var3, String var4);

    void visitMethodInsn(int var1, String var2, String var3, String var4);

    void visitJumpInsn(int var1, Label var2);

    void visitLabel(Label var1);

    void visitLdcInsn(Object var1);

    void visitIincInsn(int var1, int var2);

    void visitTableSwitchInsn(int var1, int var2, Label var3, Label[] var4);

    void visitLookupSwitchInsn(Label var1, int[] var2, Label[] var3);

    void visitMultiANewArrayInsn(String var1, int var2);

    void visitTryCatchBlock(Label var1, Label var2, Label var3, String var4);

    void visitMaxs(int var1, int var2);

    void visitLocalVariable(String var1, String var2, Label var3, Label var4, int var5);

    void visitLineNumber(int var1, Label var2);
}
