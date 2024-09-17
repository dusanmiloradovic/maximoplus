package maximoplus.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class FauxMboSetClassVisitor extends ClassVisitor implements Opcodes {

    public FauxMboSetClassVisitor(ClassVisitor cv) {
        super(ASM4, cv);
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
        mv = new MboSetCurrIndexMethodVisitor(mv, true);


        return mv;
    }


}