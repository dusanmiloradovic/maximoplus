package maximoplus.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MboSetCurrIndexMethodVisitor extends MethodVisitor implements
        Opcodes {

    private boolean currIndexCalled = false;
    private boolean faux;

    public MboSetCurrIndexMethodVisitor(MethodVisitor mv, boolean faux) {
        super(ASM4, mv);
        this.faux = faux;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name,
                               String desc) {
        if (name.equals("currIndex") && opcode == PUTFIELD) {
            currIndexCalled = true;
        }

        mv.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitInsn(int instrCode) {
        if ((instrCode == RETURN || instrCode == IRETURN
                || instrCode == FRETURN || instrCode == DRETURN || instrCode == ARETURN)
                && currIndexCalled) {

            String mboSetClass = "psdi/mbo/MboSet";
            if (faux) {
                mboSetClass = "psdi/mbo/FauxMboSet";
            }

            mv.visitVarInsn(ALOAD, 0);
            //MboSet on the stack
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, mboSetClass, "currMbo", "Lpsdi/mbo/MboRemote;");
            //mv.visitTypeInsn(CHECKCAST, "psdi/mbo/Mbo");
            //MboSet Mbo(currMbo) on the stack

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, mboSetClass, "currIndex", "I");
            mv.visitInsn(I2L);
            //MboSetRemote MboRemote(currMbo) and currIndex (int cast to long) on stack

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, mboSetClass, "currMbo", "Lpsdi/mbo/MboRemote;");
            Label l1 = new Label();
            mv.visitJumpInsn(IFNONNULL, l1);
            mv.visitLdcInsn(new Long(-1L));
            Label l2 = new Label();
            mv.visitJumpInsn(GOTO, l2);
            mv.visitLabel(l1);
            //mv.visitFrame(Opcodes.F_FULL, 1, new Object[] {"psdi/mbo/MboSet"}, 2, new Object[] {"psdi/mbo/Mbo", Opcodes.LONG});
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, mboSetClass, "currMbo", "Lpsdi/mbo/MboRemote;");
            mv.visitTypeInsn(CHECKCAST, "psdi/mbo/Mbo");
            mv.visitFieldInsn(GETFIELD, "psdi/mbo/Mbo", "uniqueMboId", "J");
            mv.visitLabel(l2);
            //mv.visitFrame(Opcodes.F_FULL, 1, new Object[] {"psdi/mbo/MboSet"}, 3, new Object[] {"psdi/mbo/Mbo", Opcodes.LONG, Opcodes.LONG});
            //MboSet Mbo(currMbo) and currIndex and uniqueid (int cast to long) on stack

            mv.visitMethodInsn(INVOKESTATIC, "maximoplus/Hook", "setMboSetCurrIndex",
                    "(Lpsdi/mbo/MboSetRemote;Lpsdi/mbo/MboRemote;JJ)V",false);

        }

        mv.visitInsn(instrCode);
    }

    @Override
    public void visitCode() {
        currIndexCalled = false;
        mv.visitCode();
    }

}
