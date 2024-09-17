package maximoplus.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MboCommandSetFieldFlag extends MethodVisitor implements Opcodes {

    public MboCommandSetFieldFlag(MethodVisitor mv) {
        super(ASM4, mv);
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == RETURN) {

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");

            mv.visitLdcInsn("setfieldflag");
            mv.visitInsn(ICONST_3);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(AASTORE);
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_1);
            mv.visitVarInsn(LLOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;",false);
            mv.visitInsn(AASTORE);
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_2);
            mv.visitVarInsn(ILOAD, 4);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;",false);
            mv.visitInsn(AASTORE);
            mv.visitMethodInsn(INVOKESTATIC, "maximoplus/Hook", "commandMboWithParam",
                    "(Lpsdi/mbo/MboRemote;JLjava/lang/String;[Ljava/lang/Object;)V",false);
        }
        mv.visitInsn(opcode);
    }
}
