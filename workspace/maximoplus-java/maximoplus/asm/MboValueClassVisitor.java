package maximoplus.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MboValueClassVisitor extends ClassVisitor implements Opcodes {

    public MboValueClassVisitor(ClassVisitor cv) {
        super(ASM4, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

        MethodVisitor mv = super.visitMethod(access, name, desc, signature,
                exceptions);
        if (name.equals("setValue") && desc.equals("(Lpsdi/util/MaxType;)V")) {
            return new MboValueMethodVisitor(mv);
        }
        if (name.equals("construct")) {
            //return new MboValueConstructVisitor(mv);
            //probacu bez ovoga, problem je za long description. Mislim da nece biti potrebno, jer ce uvek
            //morati da se radi fetch
        }
        if (name.equals("setFlag") && desc.equals("(JZ)V")) {
            return new MboValueSetFlagVisitor(mv);
        }
        return mv;
    }


}
