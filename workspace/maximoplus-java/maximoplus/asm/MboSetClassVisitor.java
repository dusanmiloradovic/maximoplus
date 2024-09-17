package maximoplus.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MboSetClassVisitor extends ClassVisitor implements Opcodes {

    public MboSetClassVisitor(ClassVisitor cv) {
        super(ASM4, cv);
    }

    @Override
    public void visitEnd() {
        FieldVisitor fv = cv.visitField(ACC_PRIVATE, "counter",
                "Ljava/util/concurrent/atomic/AtomicLong;", null, null);
        fv.visitEnd();

        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "getNewId", "()J", null,
                null);
        mv.visitCode();
        // Label l0 = new Label();
        // mv.visitLabel(l0);
        // mv.visitLineNumber(12, l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "psdi/mbo/MboSet", "counter",
                "Ljava/util/concurrent/atomic/AtomicLong;");
        mv.visitMethodInsn(INVOKEVIRTUAL,
                "java/util/concurrent/atomic/AtomicLong", "incrementAndGet",
                "()J",false);
        mv.visitInsn(LRETURN);
        // Label l1 = new Label();
        // mv.visitLabel(l1);
        // mv.visitLocalVariable("this", "Lpsdi/mbo/MboSet;", null, l0, l1, 0);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {

        //System.out.println ("Visiting method:"+name+".."+desc+".."+signature);
        MethodVisitor mv = super.visitMethod(access, name, desc, signature,
                exceptions);
        if (!"loadMboVecFromMrd".equals(name)) {
            mv = new MboSetMethodVisitorRemove(mv);
        }
        mv = new MboSetCurrIndexMethodVisitor(mv, false);
        if (name.equalsIgnoreCase("addMbo")) {
            return new MboSetMethodVisitorAddMboToSet(mv, desc);
        }
        if (name.equals("resetForRefreshOnSave")) {
            return new MboSetMethodVisitorResetRefresh(mv);
        }
        if (name.equals("resetWithSelection")) {
            return new MboSetMethodVisitorResetSelect(mv);
        }
        if (name.equals("resetThis")) {
            return new MboSetMethodVisitorResetThis(mv);
        }
        if (name.equals("<init>")) {
            return new MboSetCounterInitMethodVisitor(mv);
        }


        return mv;
    }


}