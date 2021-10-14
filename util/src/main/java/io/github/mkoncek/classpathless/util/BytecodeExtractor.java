/*-
 * Copyright (c) 2021 Marián Konček
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.mkoncek.classpathless.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;

/**
 * A utility class to extract useful information fomr class files like typenames
 * methods, fields.
 */
public class BytecodeExtractor {
    static private final int CURRENT_ASM_OPCODE = org.objectweb.asm.Opcodes.ASM9;

    private SortedSet<String> classes = new TreeSet<>();

    /**
     * Extract type fully qualified type name from a type or descriptor.
     * @param value
     * @return
     */
    private static String normalize(String value) {
        assert(!value.startsWith("("));
        int begin = 0;
        while (begin < value.length() && value.charAt(begin) == '[') {
            ++begin;
        }
        if (value.charAt(begin) == 'L' && value.charAt(value.length() - 1) == ';') {
            value = value.substring(0, value.length() - 1);
            ++begin;
        }
        return value.substring(begin).replace('/', '.');
    }

    /**
     * Signatures may contain type parameters, for example:
     * "Ljava/util/function/Consumer<LType;>;", this function extracts both
     * outer and inner types.
     */
    private static void extractSignature(String signature, Collection<String> result) {
        signature = normalize(signature);
        var begin = signature.indexOf('<');
        if (begin != -1) {
            assert(signature.charAt(signature.length() - 1) == '>');
            extractSignature(signature.substring(begin + 1, signature.length() - 1), result);
        } else {
            begin = signature.length();
        }

        result.add(signature.substring(0, begin));
    }

    private SortedSet<String> extractTypenamesFrom(byte[] classFile) {
        new ClassReader(classFile).accept(new ExtrClassVisitor(), 0);
        return classes;
    }

    private SortedSet<String> extractNestedClassesFrom(byte[] classFile, ClassesProvider classprovider) {
        var classFiles = new ArrayList<IdentifiedBytecode>();
        var addedClasses = new ArrayList<ClassIdentifier>();

        // Extract the name of outer class
        new ClassReader(classFile).accept(new ClassVisitor(CURRENT_ASM_OPCODE) {
            @Override
            public void visit(int version, int access, String name,
                    String signature, String superName, String[] interfaces) {
                addedClasses.add(new ClassIdentifier(name.replace('/', '.')));
            }
        }, 0);

        var initialClass = addedClasses.get(0);
        addedClasses.clear();

        var visitor = new ClassVisitor(CURRENT_ASM_OPCODE) {
            @Override
            public void visitInnerClass(String name, String outerName,
                    String innerName, int access) {
                var normalized = normalize(name);
                if (normalized.startsWith(initialClass.getFullName())
                        && normalized.length() > initialClass.getFullName().length()
                        && !classes.contains(normalized)) {
                    addedClasses.add(new ClassIdentifier(normalized));
                }
            }
        };

        new ClassReader(classFile).accept(visitor, 0);

        while (!addedClasses.isEmpty()) {
            classFiles.addAll(classprovider.getClass(addedClasses.toArray(new ClassIdentifier[0])));

            for (var added : addedClasses) {
                classes.add(added.getFullName());
            }
            addedClasses.clear();

            for (var file : classFiles) {
                new ClassReader(file.getFile()).accept(visitor, 0);
            }
            classFiles.clear();
        }

        return classes;
    }

    /**
     * Extracts all type names present in the .class file.
     * @param classFile The file to extract names from.
     * @return The set of fully qualified type names present in the class file.
     */
    public static SortedSet<String> extractTypenames(byte[] classFile) {
        return new BytecodeExtractor().extractTypenamesFrom(classFile);
    }

    /**
     * Extracts all the field names of the provided class excluding inherited fields.
     * @param classFile The file to extract names from.
     * @return The collection of field names.
     */
    public static Collection<String> extractFields(byte[] classFile) {
        var result = new ArrayList<String>();
        new ClassReader(classFile).accept(new ClassVisitor(CURRENT_ASM_OPCODE) {
            @Override
            public FieldVisitor visitField(int access, String name,
                    String descriptor, String signature, Object value) {
                result.add(name);
                return null;
            }
        }, 0);
        return result;
    }

    /**
     * Extracts all method names of given class. This will not include methods
     * of inner classes nor inherited methods (unless they are overriden).
     * @param classFile The file to extract names from.
     * @return The collection of method names.
     */
    public static Collection<String> extractMethods(byte[] classFile) {
        var result = new ArrayList<String>();
        new ClassReader(classFile).accept(new ClassVisitor(CURRENT_ASM_OPCODE) {
            @Override
            public MethodVisitor visitMethod(int access, String name,
                    String descriptor, String signature, String[] exceptions) {
                result.add(name);
                return null;
            }
        }, 0);
        return result;
    }

    /**
     * Extracts the names of all directly implemented interfaces, i. e. not transitively.
     * @param classFile The file to extract names from.
     * @return The collection of implemented interfaces.
     */
    public static Collection<String> extractInterfaces(byte[] classFile) {
        var result = new ArrayList<String>();
        new ClassReader(classFile).accept(new ClassVisitor(CURRENT_ASM_OPCODE) {
            @Override
            public void visit(int version, int access, String name,
                    String signature, String superName, String[] interfaces) {
                for (var iName : interfaces) {
                    result.add(iName.replace('/', '.'));
                }
            }
        }, 0);
        return result;
    }

    /**
     * Extracts the name of the super class of the provided class. Classes which
     * to not inherit this will be equal to "java.lang.Object".
     * @param classFile The file to extract the name from.
     * @return The name of the super class.
     */
    public static Optional<String> extractSuperClass(byte[] classFile) {
        var result = new String[1];
        new ClassReader(classFile).accept(new ClassVisitor(CURRENT_ASM_OPCODE) {
            @Override
            public void visit(int version, int access, String name,
                    String signature, String superName, String[] interfaces) {
                if (superName != null) {
                    result[0] = superName.replace('/', '.');
                }
            }
        }, 0);
        return Optional.ofNullable(result[0]);
    }

    /**
     * Recursively extracts all the nested class names from the initial outer
     * class possibly by pulling more class files from the class provider.
     * @param classFile The file to extract names from.
     * @param classprovider The provider of nested classes' bytecode.
     * @return The set of all nested fully qualified class names excluding the initial outer class.
     */
    public static SortedSet<String> extractNestedClasses(byte[] classFile, ClassesProvider classprovider) {
        return new BytecodeExtractor().extractNestedClassesFrom(classFile, classprovider);
    }

    private class ExtrAnnotationVisitor extends AnnotationVisitor {
        public ExtrAnnotationVisitor() {
            super(CURRENT_ASM_OPCODE);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name,
                String descriptor) {
            classes.add(normalize(descriptor));
            return this;
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            classes.add(normalize(descriptor));
        }
    }

    private class ExtrMethodVisitor extends MethodVisitor {
        public ExtrMethodVisitor() {
            super(CURRENT_ASM_OPCODE);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor,
                boolean visible) {
            classes.add(normalize(descriptor));
            return new ExtrAnnotationVisitor();
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter,
                String descriptor, boolean visible) {
            classes.add(normalize(descriptor));
            return new ExtrAnnotationVisitor();
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef,
                TypePath typePath, Label[] start, Label[] end, int[] index,
                String descriptor, boolean visible) {
            classes.add(normalize(descriptor));
            return new ExtrAnnotationVisitor();
        }



        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef,
                TypePath typePath, String descriptor, boolean visible) {
            classes.add(normalize(descriptor));
            return new ExtrAnnotationVisitor();
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(int typeRef,
                TypePath typePath, String descriptor, boolean visible) {
            classes.add(normalize(descriptor));
            return new ExtrAnnotationVisitor();
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(int typeRef,
                TypePath typePath, String descriptor, boolean visible) {
            classes.add(normalize(descriptor));
            return new ExtrAnnotationVisitor();
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            classes.add(normalize(type));
        }

        @Override
        public void visitLocalVariable(String name, String descriptor,
                String signature, Label start, Label end, int index) {
            classes.add(normalize(descriptor));
            if (signature != null) {
                extractSignature(signature, classes);
            }
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor,
                int numDimensions) {
            classes.add(normalize(descriptor));
        }

        @Override
        public void visitFieldInsn(int opcode, String owner,
                String name, String descriptor) {
            classes.add(normalize(owner));
        }

        @Override
        public void visitMethodInsn(int opcode, String owner,
                String name, String descriptor, boolean isInterface) {
            classes.add(normalize(owner));
            classes.add(normalize(Type.getType(descriptor).getReturnType().getInternalName()));
            for (var t : Type.getType(descriptor).getArgumentTypes()) {
                classes.add(normalize(t.getInternalName()));
            }
        }

        @Override
        public void visitInvokeDynamicInsn(String name,
                String descriptor, Handle bootstrapMethodHandle,
                Object... bootstrapMethodArguments) {
            classes.add(normalize(Type.getType(descriptor).getReturnType().getInternalName()));
            for (var t : Type.getType(descriptor).getArgumentTypes()) {
                classes.add(normalize(t.getInternalName()));
            }
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end,
                Label handler, String type) {
            if (type != null) {
                classes.add(normalize(type));
            }
        }
    }

    private class ExtrFieldVisitor extends FieldVisitor {
        public ExtrFieldVisitor() {
            super(CURRENT_ASM_OPCODE);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor,
                boolean visible) {
            classes.add(normalize(descriptor));
            return new ExtrAnnotationVisitor();
        }
    }

    private class ExtrClassVisitor extends ClassVisitor {
        public ExtrClassVisitor() {
            super(CURRENT_ASM_OPCODE);
        }

        @Override
        public void visit(int version, int access, String name,
                String signature, String superName, String[] interfaces) {
            classes.add(name.replace('/', '.'));
            if (superName != null) {
                classes.add(superName.replace('/', '.'));
            }
            for (var iName : interfaces) {
                classes.add(iName.replace('/', '.'));
            }
        }

        @Override
        public FieldVisitor visitField(int access, String name,
                String descriptor, String signature, Object value) {
            classes.add(normalize(descriptor));
            if (signature != null) {
                extractSignature(signature, classes);
            }
            return new ExtrFieldVisitor();
        }

        @Override
        public MethodVisitor visitMethod(int access, String name,
                String descriptor, String signature, String[] exceptions) {
            classes.add(normalize(Type.getType(descriptor).getReturnType().getInternalName()));
            for (var t : Type.getType(descriptor).getArgumentTypes()) {
                classes.add(normalize(t.getInternalName()));
            }
            if (signature != null) {
                extractSignature(signature, classes);
            }
            if (exceptions != null) {
                for (var ex : exceptions) {
                    classes.add(ex.replace('/', '.'));
                }
            }
            return new ExtrMethodVisitor();
        }

        @Override
        public void visitOuterClass(String owner, String name,
                String descriptor) {
            classes.add(normalize(owner));
        }

        @Override
        public void visitInnerClass(String name, String outerName,
                String innerName, int access) {
            classes.add(normalize(name));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor,
                boolean visible) {
            classes.add(normalize(descriptor));
            return new ExtrAnnotationVisitor();
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef,
                TypePath typePath, String descriptor, boolean visible) {
            classes.add(normalize(descriptor));
            return new ExtrAnnotationVisitor();
        }
    }
}
