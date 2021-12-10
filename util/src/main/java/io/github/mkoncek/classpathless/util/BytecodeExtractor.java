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
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;

/**
 * A utility class to extract useful information from class files, for example
 * type names, methods, fields.
 */
public class BytecodeExtractor {
    private static final int CURRENT_ASM_OPCODE = org.objectweb.asm.Opcodes.ASM9;
    private static final Pattern FORMAL_CONTENTS_PATTERN = Pattern.compile(".*?L(.+?)[;<]");

    private SortedSet<String> classes = new TreeSet<>();

    private static String dot(String value) {
        return value.replace('/', '.');
    }

    /**
     * Function for extracting the type names from descriptors.
     * @implNote Implementation is currently same as with signatures, but we
     * keep it as a separate function.
     * https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html#jvms-4.3
     */
    private static void extractDescriptor(String descriptor, Collection<String> result) {
        extractSignature(descriptor, result);
    }

    /**
     * Function for extracting the contents of formal parameters, i. e.
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
                result.add(dot(found));
            }
        }
    }

    private SortedSet<String> extractTypenamesFrom(byte[] classFile) {
        new ClassReader(classFile).accept(new ExtrClassVisitor(classes), 0);
        return classes;
    }

    private SortedSet<String> extractDirectNestedClassesFrom(byte[] classFile) {
        var className = new String[1];

        new ClassReader(classFile).accept(new ClassVisitor(CURRENT_ASM_OPCODE) {
            @Override
            public void visit(int version, int access, String name,
                    String signature, String superName, String[] interfaces) {
                className[0] = name;
            }

            @Override
            public void visitInnerClass(String name, String outerName,
                    String innerName, int access) {
                if (outerName == null || className[0].equals(outerName)) {
                    classes.add(dot(name));
                }
            }
        }, 0);

        return classes;
    }

    private SortedSet<String> extractNestedClassesFrom(byte[] classFile,
            ClassesProvider classesProvider) {
        for (var nestedName : extractDirectNestedClasses(classFile)) {
            if (classes.add(nestedName)) {
                for (var bytecode : classesProvider.getClass(new ClassIdentifier(nestedName))) {
                    extractNestedClassesFrom(bytecode.getFile(), classesProvider);
                }
            }
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
                    result.add(dot(iName));
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
                    result[0] = dot(superName);
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
                        result[0] = dot(outerName);
                    }
                }
            }

            @Override
            public void visitOuterClass(String owner, String name, String descriptor) {
                result[0] = dot(owner);
            }
        }, 0);
        return Optional.ofNullable(result[0]);
    }

    /**
     * Extracts all directly nested class names from the initial outer class.
     * @param classFile The file to extract names from.
     * @return The set of all directly nested fully qualified class names excluding the initial outer class.
     */
    public static SortedSet<String> extractDirectNestedClasses(byte[] classFile) {
        return new BytecodeExtractor().extractDirectNestedClassesFrom(classFile);
    }

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
                result.add(dot(name));
            }
        }, 0);
        return result;
    }

    /**
     * This method returns all the class names that are required for the
     * compilation of a source file corresponding to the bytecode of initialClass.
     * @param initialClass The bytecode the dependencies of which are requested.
     * @param classesProvider ClassesProvider of class dependencies.
     * @return A collection of all class names that are required for compilation.
     */
    public static Collection<String> extractDependencies(
            IdentifiedBytecode initialClass, ClassesProvider classesProvider) {
        final Consumer<String> empty = s -> {};
        return extractDependenciesImpl(initialClass, classesProvider, empty, empty, empty);
    }

    /**
     * This is an implementation method.
     * @param initialClass The bytecode the dependencies of which are requested.
     * @param classesProvider ClassesProvider of class dependencies.
     * @param first The consumer of a class name in case a class is added in the first phase.
     * @param second The consumer of a class name in case a class is added in the second phase.
     * @param third The consumer of a class name in case a class is added in the third phase.
     * @return A collection of all class names that are required for compilation.
     */
    static Collection<String> extractDependenciesImpl(
            IdentifiedBytecode initialClass, ClassesProvider classesProvider,
            Consumer<String> first, Consumer<String> second, Consumer<String> third) {
        var result = new TreeSet<String>();

        // First phase: the full group of the initial class
        for (var newClass : BytecodeExtractor.extractFullClassGroup(initialClass.getFile(), classesProvider)) {
            if (result.add(newClass)) {
                first.accept(newClass);
            }
        }

        var referencedClasses = new TreeSet<String>();

        // Second phase: directly referenced names
        for (var className : new ArrayList<>(result)) {
            for (var bytecode : classesProvider.getClass(new ClassIdentifier(className))) {
                for (var newClass : BytecodeExtractor.extractTypenames(bytecode.getFile())) {
                    if (result.add(newClass)) {
                        second.accept(newClass);
                    }
                    referencedClasses.add(newClass);
                }
            }
        }

        // Third phase: all outer classes of all referenced classes
        // Start from the longest names to avoid duplicating the traversals
        for (String className; (className = referencedClasses.pollLast()) != null;) {
            // Do not read the bytecode of java.lang.Object
            // This is a workaround to work with DCEVM 11
            if (className.equals("java.lang.Object")) {
                continue;
            }

            for (var bytecode : classesProvider.getClass(new ClassIdentifier(className))) {
                var outer = BytecodeExtractor.extractOuterClass(bytecode.getFile());
                if (outer.isPresent()) {
                    String outerName = outer.get();
                    if (result.add(outerName)) {
                        third.accept(outerName);
                    }
                    referencedClasses.add(outerName);
                }
            }
        }

        result.remove(initialClass.getClassIdentifier().getFullName());

        return result;
    }

    private static class ExtrAnnotationVisitor extends AnnotationVisitor {
        private SortedSet<String> classes;

        private ExtrAnnotationVisitor(SortedSet<String> classes) {
            super(CURRENT_ASM_OPCODE);
            this.classes = classes;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            extractDescriptor(descriptor, classes);
            return this;
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            extractDescriptor(descriptor, classes);
        }
    }

    private static class ExtrMethodVisitor extends MethodVisitor {
        private SortedSet<String> classes;

        private ExtrMethodVisitor(SortedSet<String> classes) {
            super(CURRENT_ASM_OPCODE);
            this.classes = classes;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            extractDescriptor(descriptor, classes);
            return new ExtrAnnotationVisitor(classes);
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter,
                String descriptor, boolean visible) {
            extractDescriptor(descriptor, classes);
            return new ExtrAnnotationVisitor(classes);
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef,
                TypePath typePath, Label[] start, Label[] end, int[] index,
                String descriptor, boolean visible) {
            extractDescriptor(descriptor, classes);
            return new ExtrAnnotationVisitor(classes);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef,
                TypePath typePath, String descriptor, boolean visible) {
            extractDescriptor(descriptor, classes);
            return new ExtrAnnotationVisitor(classes);
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(int typeRef,
                TypePath typePath, String descriptor, boolean visible) {
            extractDescriptor(descriptor, classes);
            return new ExtrAnnotationVisitor(classes);
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(int typeRef,
                TypePath typePath, String descriptor, boolean visible) {
            extractDescriptor(descriptor, classes);
            return new ExtrAnnotationVisitor(classes);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            classes.add(dot(type));
        }

        @Override
        public void visitLocalVariable(String name, String descriptor,
                String signature, Label start, Label end, int index) {
            extractDescriptor(descriptor, classes);
            if (signature != null) {
                extractSignature(signature, classes);
            }
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            extractDescriptor(descriptor, classes);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            classes.add(dot(owner));
            extractDescriptor(descriptor, classes);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner,
                String name, String descriptor, boolean isInterface) {
            classes.add(dot(owner));
            extractDescriptor(descriptor, classes);
        }

        @Override
        public void visitInvokeDynamicInsn(String name,
                String descriptor, Handle bootstrapMethodHandle,
                Object... bootstrapMethodArguments) {
            extractDescriptor(descriptor, classes);
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            if (type != null) {
                classes.add(dot(type));
            }
        }
    }

    private static class ExtrFieldVisitor extends FieldVisitor {
        private SortedSet<String> classes;

        private ExtrFieldVisitor(SortedSet<String> classes) {
            super(CURRENT_ASM_OPCODE);
            this.classes = classes;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            extractDescriptor(descriptor, classes);
            return new ExtrAnnotationVisitor(classes);
        }
    }

    private static class ExtrClassVisitor extends ClassVisitor {
        private SortedSet<String> classes;

        private ExtrClassVisitor(SortedSet<String> classes) {
            super(CURRENT_ASM_OPCODE);
            this.classes = classes;
        }

        @Override
        public void visit(int version, int access, String name,
                String signature, String superName, String[] interfaces) {
            classes.add(dot(name));
            if (signature != null) {
                extractSignature(signature, classes);
            }
            if (superName != null) {
                classes.add(dot(superName));
            }
            for (var intrfc : interfaces) {
                classes.add(dot(intrfc));
            }
        }

        @Override
        public FieldVisitor visitField(int access, String name,
                String descriptor, String signature, Object value) {
            extractDescriptor(descriptor, classes);
            if (signature != null) {
                extractSignature(signature, classes);
            }
            return new ExtrFieldVisitor(classes);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name,
                String descriptor, String signature, String[] exceptions) {
            extractDescriptor(descriptor, classes);
            if (signature != null) {
                extractSignature(signature, classes);
            }
            if (exceptions != null) {
                for (var ex : exceptions) {
                    classes.add(dot(ex));
                }
            }
            return new ExtrMethodVisitor(classes);
        }

        @Override
        public void visitOuterClass(String owner, String name, String descriptor) {
            classes.add(dot(owner));
            if (descriptor != null) {
                extractDescriptor(descriptor, classes);
            }
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            classes.add(dot(name));
            if (outerName != null) {
                classes.add(dot(outerName));
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            extractDescriptor(descriptor, classes);
            return new ExtrAnnotationVisitor(classes);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef,
                TypePath typePath, String descriptor, boolean visible) {
            extractDescriptor(descriptor, classes);
            return new ExtrAnnotationVisitor(classes);
        }
    }
}
