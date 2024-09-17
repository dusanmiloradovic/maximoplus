package maximoplus.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AsmRuntimeTester implements Opcodes {


    private static final String STORE_FOLDER = "/home/dusan/temp/";

    public static void main(String[] args) {
        try {
            new AsmRuntimeTester().doit();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void init() throws IOException {


    }

    public void doit() throws IOException {
        ClassReader cr = new ClassReader("psdi.mbo.Mbo");
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        MboClassVisitor mvs = new MboClassVisitor(cw);
        cr.accept(mvs, 0);
        byte[] arr = cw.toByteArray();
        File f = new File(STORE_FOLDER + "Mbo.class");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(arr);
        fos.flush();
        fos.close();

        cr = new ClassReader("psdi.mbo.MboSet");
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        MboSetClassVisitor mcvs = new MboSetClassVisitor(cw);
        cr.accept(mcvs, 0);
        arr = cw.toByteArray();
        f = new File(STORE_FOLDER + "MboSet.class");
        f.createNewFile();
        fos = new FileOutputStream(f);
        fos.write(arr);
        fos.flush();
        fos.close();

        cr = new ClassReader("psdi.mbo.MboValue");
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        MboValueClassVisitor mvcvs = new MboValueClassVisitor(cw);
        cr.accept(mvcvs, 0);
        arr = cw.toByteArray();
        f = new File(STORE_FOLDER + "MboValue.class");
        f.createNewFile();
        fos = new FileOutputStream(f);
        fos.write(arr);
        fos.flush();
        fos.close();

        cr = new ClassReader("psdi.mbo.FauxMboSet");
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        FauxMboSetClassVisitor fvs = new FauxMboSetClassVisitor(cw);
        cr.accept(fvs, 0);
        arr = cw.toByteArray();
        f = new File(STORE_FOLDER + "FauxMboSet.class");
        f.createNewFile();
        fos = new FileOutputStream(f);
        fos.write(arr);
        fos.flush();
        fos.close();

        cr = new ClassReader("psdi.server.ServiceStorage");//fix for Optimization service
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ServiceStorageClassVisitor vvv = new ServiceStorageClassVisitor(cw);
        cr.accept(vvv, 0);
        arr = cw.toByteArray();
        f = new File(STORE_FOLDER + "ServiceStorage.class");
        fos = new FileOutputStream(f);
        fos.write(arr);
        fos.flush();
        fos.close();

        cr = new ClassReader("psdi.util.CommonUtil");
        //this should stop starting of OptimizationService
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        CommonUtilVisitor cuv = new CommonUtilVisitor(cw);
        cr.accept(cuv, 0);
        arr = cw.toByteArray();
        f = new File(STORE_FOLDER + "CommonUtil.class");
        fos = new FileOutputStream(f);
        fos.write(arr);
        fos.flush();
        fos.close();
    }
}
