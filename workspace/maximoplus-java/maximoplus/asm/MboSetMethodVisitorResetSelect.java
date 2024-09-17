package maximoplus.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MboSetMethodVisitorResetSelect extends MethodVisitor implements Opcodes {
    public MboSetMethodVisitorResetSelect(MethodVisitor mv) {
        super(ASM4, mv);
    }

    @Override
    public void visitInsn(int instrCode) {
        if (instrCode == RETURN) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn("resetWithSelecion");
            mv.visitMethodInsn(INVOKESTATIC, "maximoplus/Hook", "commandMboSet",
                    "(Lpsdi/mbo/MboSetRemote;Ljava/lang/String;)V",false);
        }
        mv.visitInsn(instrCode);
    }
}
