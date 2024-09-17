package maximoplus.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.net.URI;
import java.net.URLClassLoader;
import java.util.Deque;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MinimoClassChanger implements Opcodes, Runnable {

    private String STORE_FOLDER;

    public MinimoClassChanger() {

        this.STORE_FOLDER = System.getProperty("java.io.tmpdir") + "/";

    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int readCount = in.read(buffer);
            if (readCount < 0) {
                break;
            }
            out.write(buffer, 0, readCount);
        }
    }

    private static void copy(File file, OutputStream out) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            copy(in, out);
        } finally {
            in.close();
        }
    }

    private static void copy(InputStream in, File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        try {
            copy(in, out);
        } finally {
            out.close();
        }
    }

    public static void zip(File directory, File zipfile) throws IOException {
        URI base = directory.toURI();
        Deque<File> queue = new LinkedList<File>();
        queue.push(directory);
        OutputStream out = new FileOutputStream(zipfile);
        Closeable res = out;
        try {
            ZipOutputStream zout = new ZipOutputStream(out);
            res = zout;
            while (!queue.isEmpty()) {
                directory = queue.pop();
                for (File kid : directory.listFiles()) {
                    String name = base.relativize(kid.toURI()).getPath();
                    if (kid.isDirectory()) {
                        queue.push(kid);
                        name = name.endsWith("/") ? name : name + "/";
                        zout.putNextEntry(new ZipEntry(name));
                    } else {
                        zout.putNextEntry(new ZipEntry(name));
                        copy(kid, zout);
                        zout.closeEntry();
                    }
                }
            }
        } finally {
            res.close();
        }
    }

    public void doit() throws IOException, ClassNotFoundException {
        URLClassLoader contextClassLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
        //    	System.out.println(contextClassLoader);
        //    	System.out.println(contextClassLoader.getURLs());

        contextClassLoader.loadClass("psdi.mbo.Mbo");
        contextClassLoader.loadClass("psdi.mbo.MboSet");
        contextClassLoader.loadClass("psdi.mbo.MboValue");
        contextClassLoader.loadClass("psdi.mbo.FauxMboSet");
        contextClassLoader.loadClass("psdi.util.MXException");
        contextClassLoader.loadClass("psdi.server.ServiceStorage");
        contextClassLoader.loadClass("psdi.util.CommonUtil");
        contextClassLoader.loadClass("psdi.server.MXServer");

        //System.out.println("$$$$"+this.getClass().getClassLoader());

        ClassReader cr = new ClassReader(contextClassLoader.getResourceAsStream("psdi/mbo/Mbo.class"));

        ClassWriter cw = new ComputeClassWriter(ClassWriter.COMPUTE_FRAMES);
        MboClassVisitor mvs = new MboClassVisitor(cw);
        cr.accept(mvs, 0);
        byte[] arr = cw.toByteArray();
        File f = new File(STORE_FOLDER + "Mbo.class");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(arr);
        fos.flush();
        fos.close();

        cr = new ClassReader(contextClassLoader.getResourceAsStream("psdi/mbo/MboSet.class"));
        cw = new ComputeClassWriter(ClassWriter.COMPUTE_FRAMES);
        MboSetClassVisitor mcvs = new MboSetClassVisitor(cw);
        cr.accept(mcvs, 0);
        arr = cw.toByteArray();
        f = new File(STORE_FOLDER + "MboSet.class");
        f.createNewFile();
        fos = new FileOutputStream(f);
        fos.write(arr);
        fos.flush();
        fos.close();

        cr = new ClassReader(contextClassLoader.getResourceAsStream("psdi/mbo/MboValue.class"));
        cw = new ComputeClassWriter(ClassWriter.COMPUTE_FRAMES);
        MboValueClassVisitor mvcvs = new MboValueClassVisitor(cw);
        cr.accept(mvcvs, 0);
        arr = cw.toByteArray();
        f = new File(STORE_FOLDER + "MboValue.class");
        f.createNewFile();
        fos = new FileOutputStream(f);
        fos.write(arr);
        fos.flush();
        fos.close();

        cr = new ClassReader(contextClassLoader.getResourceAsStream("psdi/mbo/FauxMboSet.class"));
        cw = new ComputeClassWriter(ClassWriter.COMPUTE_FRAMES);
        FauxMboSetClassVisitor fvs = new FauxMboSetClassVisitor(cw);
        cr.accept(fvs, 0);
        arr = cw.toByteArray();
        f = new File(STORE_FOLDER + "FauxMboSet.class");
        f.createNewFile();
        fos = new FileOutputStream(f);
        fos.write(arr);
        fos.flush();
        fos.close();

        cr = new ClassReader(contextClassLoader.getResourceAsStream("psdi/server/ServiceStorage.class"));
        //this should stop starting of OptimizationService
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ServiceStorageClassVisitor vvv = new ServiceStorageClassVisitor(cw);
        cr.accept(vvv, 0);
        arr = cw.toByteArray();
        f = new File(STORE_FOLDER + "ServiceStorage.class");
        fos = new FileOutputStream(f);
        fos.write(arr);
        fos.flush();
        fos.close();

        cr = new ClassReader(contextClassLoader.getResourceAsStream("psdi/util/CommonUtil.class"));

        cw = new CommonUtilClassWriter(ClassWriter.COMPUTE_FRAMES);
        CommonUtilVisitor cuv = new CommonUtilVisitor(cw);
        cr.accept(cuv, 0);
        arr = cw.toByteArray();
        f = new File(STORE_FOLDER + "CommonUtil.class");
        fos = new FileOutputStream(f);
        fos.write(arr);
        fos.flush();
        fos.close();


        cr = new ClassReader(contextClassLoader.getResourceAsStream("com/ibm/tivoli/maximo/report/birt/admin/ReportAdminService.class"));

        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        BirtReportVisitor birtSkipper = new BirtReportVisitor(cw);
        cr.accept(birtSkipper, 0);
        arr = cw.toByteArray();
        f = new File(STORE_FOLDER + "ReportAdminService.class");
        fos = new FileOutputStream(f);
        fos.write(arr);
        fos.flush();
        fos.close();
    }

    @Override
    public void run() {
        try {
            doit();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * A ClassWriter that computes the common super class of two classes without
     * actually loading them with a ClassLoader.
     *
     * @author Eric Bruneton
     */
    class ComputeClassWriter extends ClassWriter {

        //  private ClassLoader l = getClass().getClassLoader();
        private ClassLoader l = Thread.currentThread().getContextClassLoader();

        public ComputeClassWriter(final int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(final String type1, final String type2) {
            try {
                ClassReader info1 = typeInfo(type1);
                ClassReader info2 = typeInfo(type2);
                if ((info1.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                    if (typeImplements(type2, info2, type1)) {
                        return type1;
                    }
                    if ((info2.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                        if (typeImplements(type1, info1, type2)) {
                            return type2;
                        }
                    }
                    return "java/lang/Object";
                }
                if ((info2.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                    if (typeImplements(type1, info1, type2)) {
                        return type2;
                    } else {
                        return "java/lang/Object";
                    }
                }
                StringBuilder b1 = typeAncestors(type1, info1);
                StringBuilder b2 = typeAncestors(type2, info2);
                String result = "java/lang/Object";
                int end1 = b1.length();
                int end2 = b2.length();
                while (true) {
                    int start1 = b1.lastIndexOf(";", end1 - 1);
                    int start2 = b2.lastIndexOf(";", end2 - 1);
                    if (start1 != -1 && start2 != -1
                            && end1 - start1 == end2 - start2) {
                        String p1 = b1.substring(start1 + 1, end1);
                        String p2 = b2.substring(start2 + 1, end2);
                        if (p1.equals(p2)) {
                            result = p1;
                            end1 = start1;
                            end2 = start2;
                        } else {
                            return result;
                        }
                    } else {
                        return result;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e.toString());
            }
        }

        /**
         * Returns the internal names of the ancestor classes of the given type.
         *
         * @param type the internal name of a class or interface.
         * @param info the ClassReader corresponding to 'type'.
         * @return a StringBuilder containing the ancestor classes of 'type',
         * separated by ';'. The returned string has the following format:
         * ";type1;type2 ... ;typeN", where type1 is 'type', and typeN is a
         * direct subclass of Object. If 'type' is Object, the returned
         * string is empty.
         * @throws IOException if the bytecode of 'type' or of some of its ancestor class
         *                     cannot be loaded.
         */
        private StringBuilder typeAncestors(String type, ClassReader info)
                throws IOException {
            StringBuilder b = new StringBuilder();
            while (!"java/lang/Object".equals(type)) {
                b.append(';').append(type);
                type = info.getSuperName();
                info = typeInfo(type);
            }
            return b;
        }

        /**
         * Returns true if the given type implements the given interface.
         *
         * @param type the internal name of a class or interface.
         * @param info the ClassReader corresponding to 'type'.
         * @param itf  the internal name of a interface.
         * @return true if 'type' implements directly or indirectly 'itf'
         * @throws IOException if the bytecode of 'type' or of some of its ancestor class
         *                     cannot be loaded.
         */
        private boolean typeImplements(String type, ClassReader info, String itf)
                throws IOException {
            while (!"java/lang/Object".equals(type)) {
                String[] itfs = info.getInterfaces();
                for (int i = 0; i < itfs.length; ++i) {
                    if (itfs[i].equals(itf)) {
                        return true;
                    }
                }
                for (int i = 0; i < itfs.length; ++i) {
                    if (typeImplements(itfs[i], typeInfo(itfs[i]), itf)) {
                        return true;
                    }
                }
                type = info.getSuperName();
                info = typeInfo(type);
            }
            return false;
        }

        /**
         * Returns a ClassReader corresponding to the given class or interface.
         *
         * @param type the internal name of a class or interface.
         * @return the ClassReader corresponding to 'type'.
         * @throws IOException if the bytecode of 'type' cannot be loaded.
         */
        private ClassReader typeInfo(final String type) throws IOException {
            InputStream is = l.getResourceAsStream(type + ".class");
            try {
                return new ClassReader(is);
            } finally {
                is.close();
            }
        }
    }

}
