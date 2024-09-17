package maximoplus.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MboSetMethodVisitorRemove extends MethodVisitor implements Opcodes {
    public MboSetMethodVisitorRemove(MethodVisitor mv) {
        super(ASM4, mv);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
                                String desc,boolean isInterface) {

        mv.visitMethodInsn(opcode, owner, name, desc,false);
        if (owner.equals("java/util/Vector") && name.equals("removeElement")) {
            //			System.out.println ("Called remove"+owner+".."+desc);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "psdi/mbo/Mbo");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "psdi/mbo/Mbo");
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");

            mv.visitMethodInsn(INVOKESTATIC, "maximoplus/Hook", "removeMbo",
                    "(Lpsdi/mbo/MboRemote;J)V",false);
        }
        if (owner.equals("java/util/Vector") && name.equals("removeAllElements")) {
            //	System.out.println ("Called removeAllElements");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn("removeAllFromSet");
            mv.visitMethodInsn(INVOKESTATIC, "maximoplus/Hook", "commandMboSet",
                    "(Lpsdi/mbo/MboSetRemote;Ljava/lang/String;)V",false);
        }
    }

}
