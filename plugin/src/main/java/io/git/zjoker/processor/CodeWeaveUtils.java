package io.git.zjoker.processor;


import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

public class CodeWeaveUtils {

    public static final String CONSTRUCTOR_NAME = "<init>";
    public static final String CLASS_PATH_ZAOP = getClassPath("io.git.zjoker.zaop.ZAOP");
    public static final String CLASS_PATH_ACTION = getClassPath("io.git.zjoker.zaop.utils.Action");

    public static void prepareArgsForSelfMethod(MethodVisitor mv, Type[] arguments, boolean isStatic) {
        int offset = 0;
        if (!isStatic) {
            offset = 1;
            aloadThis(mv);
        }
        aloadVars(mv, arguments, offset);
    }

    public static void aloadThis(MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
    }

    public static void aloadVars(MethodVisitor mv, Type[] arguments, int offset) {
        for (int i = 0; i < arguments.length; i++) {
            mv.visitVarInsn(getLoadCode(arguments[i]), i + offset);
        }
    }

    public static boolean isStatic(int access) {
        return hasMark(access, ACC_STATIC);
    }

    public static int getLoadCode(Type argumentType) {
        if (argumentType.equals(Type.DOUBLE_TYPE)) {
            return DLOAD;
        } else if (argumentType.equals(Type.FLOAT_TYPE)) {
            return FLOAD;
        } else if (argumentType.equals(Type.LONG_TYPE)) {
            return LLOAD;
        } else if (argumentType.equals(Type.CHAR_TYPE)
                || argumentType.equals(Type.BOOLEAN_TYPE)
                || argumentType.equals(Type.BYTE_TYPE)
                || argumentType.equals(Type.INT_TYPE)) {
            return ILOAD;
        } else {
            return ALOAD;
        }
    }

    public static void returnDefault(MethodVisitor mv, String medthodDesc) {
        Type returnType = Type.getReturnType(medthodDesc);

        if (returnType.equals(Type.VOID_TYPE)) {
            mv.visitInsn(RETURN);
        } else if (returnType.equals(Type.DOUBLE_TYPE)) {
            mv.visitInsn(DCONST_0);
            mv.visitInsn(DRETURN);
        } else if (returnType.equals(Type.FLOAT_TYPE)) {
            mv.visitInsn(FCONST_0);
            mv.visitInsn(FRETURN);
        } else if (returnType.equals(Type.LONG_TYPE)) {
            mv.visitInsn(LCONST_0);
            mv.visitInsn(LRETURN);
        } else if (returnType.equals(Type.CHAR_TYPE)
                || returnType.equals(Type.BOOLEAN_TYPE)
                || returnType.equals(Type.BYTE_TYPE)
                || returnType.equals(Type.INT_TYPE)) {
            mv.visitInsn(ICONST_0);
            mv.visitInsn(IRETURN);
        } else {
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
        }
    }


    public static void callPublicInstanceMethod(MethodVisitor mv, String className, String methodName, String methodDesc, boolean isInterface) {
        callInstanceMethod(mv, className, methodName, methodDesc, isInterface, true);
    }

    public static void callInstanceMethod(MethodVisitor mv, String className, String methodName, String methodDesc, boolean isInterface, boolean isPrivate) {
        callMethod(mv, className, methodName, methodDesc, false, isInterface, isPrivate);
    }

    public static void callStaticMethod(MethodVisitor mv, String className, String methodName, String methodDesc) {
        callMethod(mv, className, methodName, methodDesc, true, false, false);
    }

    public static void callSuperMethod(MethodVisitor mv, String className, String methodName, String methodDesc, boolean isStatic, boolean isInterface) {
        callMethod(mv, className, methodName, methodDesc, isStatic, isInterface, true);
    }

    public static void callMethod(MethodVisitor mv, String className, String methodName, String methodDesc, boolean isStatic, boolean isInterface, boolean isPrivate) {
        int executeCode = INVOKEVIRTUAL;
        if (isStatic) {
            executeCode = INVOKESTATIC;
        } else if (isInterface) {
            executeCode = INVOKEINTERFACE;
        } else if (isPrivate) {
            executeCode = INVOKESPECIAL;
        }
        mv.visitMethodInsn(executeCode, className, methodName, methodDesc, isInterface);
    }

    public static void newObj(MethodVisitor mv, String className, String methodDesc) {
        mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", methodDesc, false);
    }

    public static String getClassName(String classPath) {
        return classPath.replace("/", ".");
    }

    public static String getClassPath(String className) {
        return className.replace(".", "/");
    }

    public static String getClassPath(Class clazz) {
        return getClassPath(clazz.getName());
    }

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isChildOf(String name, String targetSuperName) {

        try {
            Class sourceClass = Class.forName(getClassName(name));
            Class targetClass = Class.forName(getClassName(targetSuperName));

            return targetClass.isAssignableFrom(sourceClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return false;


//        if (getClassPath(Object.class).equals(superName)) {
//            return getClassPath(Object.class).equals(targetSuperName);
//        }
//        if (targetSuperName.equals(superName)) {
//            return true;
//        }
//
//        try {
//            ClassReader cr = new ClassReader(superName);
//            ClassNode cn = new ClassNode(ClassProcessor.api);
//            cr.accept(cn, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);
//            return isChildOf(cn.superName, targetSuperName);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return false;
    }


    public static boolean matchMethod(String name, String desc, String targetName, Type... targetParamType) {
        if (!name.equals(targetName)) {
            return false;
        }

        Type[] argumentTypes = Type.getArgumentTypes(desc);
        if ((targetParamType != null ? targetParamType.length : 0) != argumentTypes.length) {
            return false;
        }

        for (int i = 0; i < argumentTypes.length; i++) {
            Type type = argumentTypes[i];
            if (!targetParamType[i].equals(type)) {
                return false;
            }
        }

        return true;
    }

    public static String getMethodDesc(Class returnClass, Class... paramsClass) {
        StringBuilder medthodDesc = new StringBuilder("(");
        if (paramsClass != null) {
            for (Class paramClass : paramsClass) {
                medthodDesc.append(Type.getDescriptor(paramClass));
            }
        }
        String returnDesc = Type.getDescriptor(returnClass);
        medthodDesc.append(")").append(returnDesc);
        return medthodDesc.toString();
    }

    public static String getMethodDesc(Type returnClass, Type... paramsClass) {
        return getMethodDesc(returnClass, Arrays.asList(paramsClass));
    }

    public static String getMethodDesc(Type returnClass, List<Type> paramsClass) {
        StringBuilder medthodDesc = new StringBuilder("(");
        if (paramsClass != null) {
            for (Type paramClass : paramsClass) {
                medthodDesc.append(paramClass.getDescriptor());
            }
        }
        String returnDesc = returnClass.getDescriptor();
        medthodDesc.append(")").append(returnDesc);
        return medthodDesc.toString();
    }


    public static Type[] getTypes(Class... classes) {
        if (classes == null || classes.length == 0) {
            return new Type[0];
        }

        Type[] types = new Type[classes.length];
        for (int i = 0; i < classes.length; i++) {
            types[i] = Type.getType(classes[i]);
        }
        return types;
    }

    public static int removeMark(int value, int mark) {
        return value & ~mark;
    }

    public static int addMark(int value, int mark) {
        return value | mark;
    }

    public static int replaceMarkIfHas(int value, int oldMark, int newMark) {
        if (!hasMark(value, oldMark)) {
            return value;
        }
        int result = value;
        result = removeMark(result, oldMark);
        result = addMark(result, newMark);
        return result;
    }

    public static boolean hasMark(int value, int mark) {
        return (value & mark) == mark;
    }


    public static String[] getExceptionArr(List<String> list) {
        String[] exceptions = null;
        if (list != null) {
            exceptions = new String[list.size()];
            list.toArray(exceptions);
        }
        return exceptions;
    }

    public static void writeByteToFile(byte[] bytes, String targetFileName) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(targetFileName);
            fos.write(bytes);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            CodeWeaveUtils.close(fos);
        }
    }

    public static String getDesc(String className) {
        return Type.getObjectType(getClassPath(className)).getDescriptor();
    }
}
