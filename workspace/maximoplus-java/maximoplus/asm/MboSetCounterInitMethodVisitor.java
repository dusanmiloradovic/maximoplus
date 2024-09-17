package maximoplus.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MboSetCounterInitMethodVisitor extends MethodVisitor implements Opcodes {

    public MboSetCounterInitMethodVisitor(MethodVisitor mv) {
        super(ASM4, mv);
    }

    @Override
    public void visitInsn(int instrCode) {
        if (instrCode == RETURN) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(NEW, "java/util/concurrent/atomic/AtomicLong");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/atomic/AtomicLong", "<init>", "()V",false);
            mv.visitFieldInsn(PUTFIELD, "psdi/mbo/MboSet", "counter", "Ljava/util/concurrent/atomic/AtomicLong;");
        }
        mv.visitInsn(instrCode);
    }
}
