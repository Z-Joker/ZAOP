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
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

public class ThreadSwitchWeaver {

    private static Map<String, Integer> innerClassIndex = new HashMap<>();

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

    private static void createOuterMethodProxy(ClassVisitor cw, ClassNode classNode, MethodNode methodNode, AnnotationNode threadOnAnn, String innerClass) {
        String threadUtilsMethodName = "POSTING";
        if (threadOnAnn.values != null && threadOnAnn.values.size() > 1) {
            String[] valueArr = (String[]) threadOnAnn.values.get(1);
            if (valueArr != null && valueArr.length > 1) {
                threadUtilsMethodName = valueArr[1];
            }
        }
        cw.visitInnerClass(innerClass, null, null, 0);
        {
            Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);

            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodNode.name, methodNode.desc, methodNode.signature, CodeWeaveUtils.getExceptionArr(methodNode.exceptions));
            mv.visitCode();

            //????????????
//            CodeWeaveUtils.callStaticMethod(mv, Type.getInternalName(ThreadOnExecutor.class), "returnValueIgnoreAlert", CodeWeaveUtils.getMethodDesc(void.class));

            //???PermissionsUtils.checkSelfPermissions????????
            mv.visitTypeInsn(NEW, innerClass);
            mv.visitInsn(DUP);


            //??????Action??????????????????????????????????????????????????????????????
            //1.????????
            mv.visitVarInsn(ALOAD, 0);

            //2.???????????????????????????
            CodeWeaveUtils.aloadVars(mv, argumentTypes, 1);

            //new ????????????
            List<Type> arguments = new ArrayList<>(Arrays.asList(Type.getArgumentTypes(methodNode.desc)));
            arguments.add(0, Type.getObjectType(classNode.name));
            CodeWeaveUtils.newObj(mv, innerClass, CodeWeaveUtils.getMethodDesc(Type.VOID_TYPE, arguments));


            //?????з▌???????
            CodeWeaveUtils.callStaticMethod(
                    mv
                    , CLASS_PATH_ZAOP
                    , threadUtilsMethodName.toLowerCase()
                    , CodeWeaveUtils.getMethodDesc(
                            Type.VOID_TYPE
                            , Type.getObjectType(CLASS_PATH_ACTION)));

            CodeWeaveUtils.returnDefault(mv, methodNode.desc);

            mv.visitMaxs(4, 2);
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
                FieldVisitor fvArg = cw.visitField(ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, String.format("val$%s", i), argumentType.getDescriptor(), null, null);
                fvArg.visitEnd();
            }
        }

        {
            //new ????
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, CONSTRUCTOR_NAME, CodeWeaveUtils.getMethodDesc(Type.VOID_TYPE, innerClassFiledTs), null, null);
            mv.visitCode();

            //??????????
            for (int i = 0; i < innerClassFiledTs.size(); i++) {
                Type argumentType = innerClassFiledTs.get(i);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(CodeWeaveUtils.getLoadCode(argumentType), i + 1);
                mv.visitFieldInsn(PUTFIELD, innerClassName, String.format("val$%s", i), argumentType.getDescriptor());
            }

            CodeWeaveUtils.aloadThis(mv);
            CodeWeaveUtils.callSuperMethod(mv, Type.getInternalName(Object.class), CONSTRUCTOR_NAME, CodeWeaveUtils.getMethodDesc(void.class), false, false);

            mv.visitInsn(RETURN);

            mv.visitMaxs(2, 3);
            mv.visitEnd();
        }
        {
            //run????
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "run", CodeWeaveUtils.getMethodDesc(void.class), null, null);
            mv.visitCode();

            //??ио????????????????????????
            for (int i = 0; i < innerClassFiledTs.size(); i++) {
                Type argumentType = innerClassFiledTs.get(i);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, innerClassName, String.format("val$%s", i), argumentType.getDescriptor());
            }


//            CodeWeaveUtils.callInstanceMethod(mv, outerClass.name, methodToCall.name, methodToCall.desc, false, false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, outerClass.name, methodToCall.name,  methodToCall.desc, false);

            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }
        cw.visitEnd();

        String fileName = innerClassName.substring(innerClassName.lastIndexOf("/") + 1);

        CodeWeaveUtils.writeByteToFile(cw.toByteArray(), directory + File.separator + fileName + ".class");

    }


    public static String getProxyMethodName(String methodName) {
        return methodName + "$ThreadSwitch";
    }

    private static String getInnerClassName(String outerClassName) {
        Integer index = innerClassIndex.get(outerClassName);
        if (index == null) {
            index = 1;
        }
        innerClassIndex.put(outerClassName, index + 1);

        return String.format("%s$ThreadSwitch$%s", outerClassName, index);
    }
}
