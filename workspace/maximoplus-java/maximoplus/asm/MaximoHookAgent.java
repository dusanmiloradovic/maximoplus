package maximoplus.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class MaximoHookAgent implements ClassFileTransformer {

    public static void premain(String agentArgs, Instrumentation instr) {
        System.out.println("#######################################");
        System.out.println("Starting Maximo monkey patch");
        System.out.println("#######################################");
        instr.addTransformer(new MaximoHookAgent());
    }

    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {

        if (className.equals("psdi/mbo/Mbo")) {
            System.out.println("---------------------------------->Changing the Mbo class");
            new Throwable().printStackTrace();
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            maximoplus.asm.MboClassVisitor mvs = new MboClassVisitor(cw);
            cr.accept(mvs, 0);
            byte[] arr = cw.toByteArray();
//			File f= new File("/home/dusan/tmp/psdi/mbo/Mbo.class");
//			try {
//				f.createNewFile();
//				FileOutputStream fos= new FileOutputStream(f);
//				fos.write(arr);
//				fos.flush();
//				fos.close();
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
            return arr;
        }
        if (className.equals("psdi/mbo/MboSet")) {
            System.out.println("---------------------------------->Changing the MboSet class bajo");
            new Throwable().printStackTrace();
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            maximoplus.asm.MboSetClassVisitor mvs = new MboSetClassVisitor(cw);
            cr.accept(mvs, 0);
            System.out.println("BOMBA****************************************************************");
            return cw.toByteArray();
        }
        if (className.equals("psdi/mbo/MboValue")) {
            System.out.println("---------------------------------->Changing the MboValue class");
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            maximoplus.asm.MboValueClassVisitor mvs = new MboValueClassVisitor(cw);
            cr.accept(mvs, 0);
            return cw.toByteArray();
        }
        return null;
    }
}
