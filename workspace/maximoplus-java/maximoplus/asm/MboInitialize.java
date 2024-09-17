package maximoplus.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MboInitialize extends MethodVisitor implements Opcodes {

    public MboInitialize(MethodVisitor mv) {
        super(ASM4, mv);
    }


    @Override
    public void visitInsn(int opcode) {
        if (opcode == RETURN) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/Mbo", "mySet",
                    "Lpsdi/mbo/MboSet;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "psdi/mbo/MboSet", "getNewId",
                    "()J",false);
            mv.visitFieldInsn(PUTFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");

            mv.visitMethodInsn(INVOKESTATIC, "maximoplus/Hook", "createMbo",
                    "(Lpsdi/mbo/MboRemote;J)V",false);
        }
        mv.visitInsn(opcode);
    }


}
