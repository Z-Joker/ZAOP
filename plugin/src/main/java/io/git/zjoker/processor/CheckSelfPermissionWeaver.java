package io.git.zjoker.processor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.git.zjoker.processor.CodeWeaveUtils.CLASS_PATH_ACTION;
import static io.git.zjoker.processor.CodeWeaveUtils.CLASS_PATH_ZAOP;
import static io.git.zjoker.processor.CodeWeaveUtils.CONSTRUCTOR_NAME;
import static io.git.zjoker.processor.CodeWeaveUtils.getClassName;
import static io.git.zjoker.processor.CodeWeaveUtils.getMethodDesc;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

public class CheckSelfPermissionWeaver {
    private static Map<String, Integer> innerClassIndex = new HashMap<>(0);

    public static void clearInnerClassIndex() {
        if(innerClassIndex != null) {
            innerClassIndex.clear();
        }
    }

    public static void weave(ClassVisitor cw, ClassNode classNode, MethodNode methodNode, AnnotationNode checkPermissionAnn, String directory) {
        String innerClass = createInnerClass(classNode, methodNode, directory);

        createOuterMethodProxy(cw, classNode, methodNode, checkPermissionAnn, innerClass);
    }

    private static String createInnerClass(ClassNode outerClass, MethodNode outerMethod, String directory) {
        String innerClassName = getInnerClassName(outerClass.name);
        MethodNode methodToCall = new MethodNode(ClassProcessor.api);
        methodToCall.name = getProxyMethodName(outerMethod.name);
        methodToCall.desc = outerMethod.desc;

        createInnerClassAndCallOuter(
                outerClass
                , outerMethod
                , innerClassName
                , Type.getInternalName(Object.class)
                , new String[]{CLASS_PATH_ACTION}
                , methodToCall
                , directory);
        return innerClassName;
    }

    private static void createOuterMethodProxy(ClassVisitor cw, ClassNode classNode, MethodNode methodNode, AnnotationNode checkPermissionAnn, String innerClass) {
        cw.visitInnerClass(innerClass, null, null, 0);
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodNode.name, methodNode.desc, methodNode.signature, CodeWeaveUtils.getExceptionArr(methodNode.exceptions));

            mv.visitCode();

//            //????????????
//            CodeWeaveUtils.callStaticMethod(mv, Type.getInternalName(ZAOP.class), "returnValueIgnoreAlert", getMethodDesc(void.class));

            //???@CheckPermission?з╓?permission
            List<String> permissions = new ArrayList<>();
            if (checkPermissionAnn.values.size() > 1) {
                permissions = (List<String>) checkPermissionAnn.values.get(1);
            }
            mv.visitIntInsn(BIPUSH, permissions.size());
            mv.visitTypeInsn(ANEWARRAY, Type.getInternalName(String.class));

            //????permission????
            for (int i = 0; i < permissions.size(); i++) {
                mv.visitInsn(DUP);
                mv.visitIntInsn(BIPUSH, i);
                mv.visitLdcInsn(permissions.get(i));
                mv.visitInsn(AASTORE);
            }
            Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
            int permissionArrVarIndex = argumentTypes.length + 1;
            mv.visitVarInsn(ASTORE, permissionArrVarIndex);


            //???PermissionsCodeWeaveUtils.checkSelfPermissions????????
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, permissionArrVarIndex);
            mv.visitTypeInsn(NEW, innerClass); //Action?????
            mv.visitInsn(DUP);

            //??????Action??????????????????????????????????????????????????????????????
            //1.????????
            mv.visitVarInsn(ALOAD, 0);

            //2.???????????????????????????
            CodeWeaveUtils.aloadVars(mv, argumentTypes, 1);

            //new ????????????
            List<Type> arguments = new ArrayList<>(Arrays.asList(Type.getArgumentTypes(methodNode.desc)));
            arguments.add(0, Type.getObjectType(classNode.name));
            CodeWeaveUtils.newObj(mv, innerClass, getMethodDesc(Type.VOID_TYPE, arguments));

            CodeWeaveUtils.callStaticMethod(
                    mv
                    , CLASS_PATH_ZAOP
                    , "checkSelfPermissions"
                    , getMethodDesc(
                            Type.VOID_TYPE
                            , Type.getType(Object.class)
                            , Type.getType(String[].class)
                            , Type.getObjectType(CLASS_PATH_ACTION)));

            CodeWeaveUtils.returnDefault(mv, methodNode.desc);

            mv.visitMaxs(5, 2);
            mv.visitEnd();
        }
    }

    //????Permission.CheckSelfPermissions????Action???????????
    private static void createInnerClassAndCallOuter(
            ClassNode outerClass
            , MethodNode outerMethod
            , String innerClassName
            , String supperName
            , String[] interfacesName
            , MethodNode methodToCall
            , String directory) {

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        cw.visit(V1_8, ACC_SUPER | ACC_PUBLIC, innerClassName, null, supperName, interfacesName);

        cw.visitSource(outerClass.sourceFile, null);

        cw.visitOuterClass(outerClass.name, outerMethod.name, outerMethod.desc);

        cw.visitInnerClass(innerClassName, null, null, 0);


        List<Type> innerClassFiledTs = new ArrayList<>(Arrays.asList(Type.getArgumentTypes(methodToCall.desc)));
        innerClassFiledTs.add(0, Type.getObjectType(outerClass.name));

        //???????????????????????????????????????????????????????
        {
            for (int i = 0; i < innerClassFiledTs.size(); i++) {
                Type argumentType = innerClassFiledTs.get(i);
                FieldVisitor fvArg = cw.visitField(ACC_FINAL + ACC_SYNTHETIC, String.format("val$%s", i), argumentType.getDescriptor(), null, null);
                fvArg.visitEnd();
            }
        }
        {
            //new ????
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, CONSTRUCTOR_NAME, getMethodDesc(Type.VOID_TYPE, innerClassFiledTs), null, null);
            mv.visitCode();

            //??????????
            for (int i = 0; i < innerClassFiledTs.size(); i++) {
                Type argumentType = innerClassFiledTs.get(i);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(CodeWeaveUtils.getLoadCode(argumentType), i + 1);
                mv.visitFieldInsn(PUTFIELD, innerClassName, String.format("val$%s", i), argumentType.getDescriptor());
            }

            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), CONSTRUCTOR_NAME, getMethodDesc(void.class), false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }

        //run????
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
            mv.visitCode();

            //??ио????????????????????????
            for (int i = 0; i < innerClassFiledTs.size(); i++) {
                Type argumentType = innerClassFiledTs.get(i);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, innerClassName, String.format("val$%s", i), argumentType.getDescriptor());
            }

            mv.visitMethodInsn(INVOKEVIRTUAL, outerClass.name, methodToCall.name, methodToCall.desc, false);

            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            String fileName = innerClassName.substring(innerClassName.lastIndexOf("/") + 1);

            mv.visitLdcInsn(directory + File.separator + fileName + ".class");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

            mv.visitInsn(RETURN);

            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        cw.visitEnd();
//        CodeWeaveUtils.writeByteToFile(cw.toByteArray(), System.getProperty("user.dir")
//                + String.format("/build/classes/java/main/%s.class", innerClassName));

        String fileName = innerClassName.substring(innerClassName.lastIndexOf("/") + 1);

        CodeWeaveUtils.writeByteToFile(cw.toByteArray(), directory + File.separator + fileName + ".class");
    }


    private static String getInnerClassName(String outerClassName) {
        Integer index = innerClassIndex.get(outerClassName);
        if (index == null) {
            index = 1;
        }
        innerClassIndex.put(outerClassName, index + 1);

        return String.format("%s$CheckSelfPermission$%s", outerClassName, index);
    }

    public static String getProxyMethodName(String methodName) {
        return methodName + "$CheckSelfPermission";
    }
}
