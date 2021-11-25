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
import java.util.function.Supplier;
import java.util.regex.Pattern;

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
    private static final int CURRENT_ASM_OPCODE = org.objectweb.asm.Opcodes.ASM9;
    private static final Pattern FORMAL_CONTENTS_PATTERN = Pattern.compile(".*?L(.*?)[;<]");

    private SortedSet<String> classes = new TreeSet<>();

    /**
     * Extract type fully qualified type name from a type or descriptor. This
     * function is not used on signatures which may contain formal parameters.
     * @implNote This function was originally meant to extract types from
     * type descriptors. It is however also used on raw types obtained by calls to
     * `Type.getInternalName()`, this causes no trouble so far.
     * @param value Raw type descriptor or just the type name.
     * @return The simple string representing the fully-qualified name of the class
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
     * Function for extracting the inner elements of formal parameters, i. e.
     * those contained in <> parentheses. This function does not do full signature
     * parsing, just a simple search. The types not caught by the regular expression
     * should be already caught by other visitors.
     * For reference about signatures, see:
     * https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html#jvms-4.3
     */
    private static void extractSignature(String signature, Collection<String> result) {
        var matcher = FORMAL_CONTENTS_PATTERN.matcher(signature);
        while (matcher.find()) {
            var found = matcher.group(1);
            if (found != null) {
                result.add(found.replace('/', '.'));
            }
        }
    }

    private SortedSet<String> extractTypenamesFrom(byte[] classFile) {
        new ClassReader(classFile).accept(new ExtrClassVisitor(), 0);
        return classes;
    }

    private SortedSet<String> extractNestedClassesFrom(byte[] classFile,
            ClassesProvider classesProvider) {
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
            classFiles.addAll(classesProvider.getClass(addedClasses.toArray(new ClassIdentifier[0])));

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
     * Extracts the name of the outer class of the provided class.
     * @implNote We first obtain the name of the class itself and then deduce
     * when to set the name of the outer class. The method "visitInnerClass" is
     * invoked on both this class as well as its own nested members. Otherwise
     * "visitOuterClass" takes care of the case when the class is inside a method.
     * @param classFile The file to extract the name from.
     * @return The name of the super class.
     */
    public static Optional<String> extractOuterClass(byte[] classFile) {
        var result = new String[2];
        new ClassReader(classFile).accept(new ClassVisitor(CURRENT_ASM_OPCODE) {
            @Override
            public void visit(int version, int access, String name,
                    String signature, String superName, String[] interfaces) {
                result[1] = name;
            }

            @Override
            public void visitInnerClass(String name, String outerName, String innerName, int access) {
                if (result[1].equals(name)) {
                    if (outerName != null) {
                        result[0] = outerName.replace('/', '.');
                    }
                }
            }

            @Override
            public void visitOuterClass(String owner, String name, String descriptor) {
                result[0] = owner.replace('/', '.');
            }
        }, 0);
        return Optional.ofNullable(result[0]);
    }

    Supplier<Integer> i = () -> {
        class Lol {};
        return 5;
    };

    /**
     * Recursively extracts all the nested class names from the initial outer
     * class possibly by pulling more class files from the class provider.
     * @param classFile The file to extract names from.
     * @param classesProvider The provider of nested classes' bytecode.
     * @return The set of all nested fully qualified class names excluding the initial outer class.
     */
    public static SortedSet<String> extractNestedClasses(byte[] classFile, ClassesProvider classesProvider) {
        return new BytecodeExtractor().extractNestedClassesFrom(classFile, classesProvider);
    }

    /**
     * Walk up to outermost class and return all its transitively nested classes.
     * @param classFile The file to extract names from.
     * @param classesProvider The provider of nested classes' bytecode.
     * @return The set of all fully qualified class names of the nest to which this class belongs.
     */
    public static SortedSet<String> extractFullClassGroup(byte[] classFile, ClassesProvider classesProvider) {
        Optional<String> outermostClass;
        while ((outermostClass = extractOuterClass(classFile)).isPresent()) {
            classFile = classesProvider.getClass(new ClassIdentifier(outermostClass.get()))
                    .iterator().next().getFile();
        }
        var result = extractNestedClasses(classFile, classesProvider);
        new ClassReader(classFile).accept(new ClassVisitor(CURRENT_ASM_OPCODE) {
            @Override
            public void visit(int version, int access, String name,
                    String signature, String superName, String[] interfaces) {
                result.add(name.replace('/', '.'));
            }
        }, 0);
        return result;
    }

    private class ExtrAnnotationVisitor extends AnnotationVisitor {
        public ExtrAnnotationVisitor() {
            super(CURRENT_ASM_OPCODE);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
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
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
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
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            classes.add(normalize(descriptor));
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
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
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
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
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
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
            if (signature != null) {
                extractSignature(signature, classes);
            }
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
        public void visitOuterClass(String owner, String name, String descriptor) {
            classes.add(normalize(owner));
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            classes.add(normalize(name));
            if (outerName != null) {
                classes.add(normalize(outerName));
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
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
