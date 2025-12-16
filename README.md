# classpathless-compiler

Classpathless Compiler (CPLC) is a powerful compiler wrapper built on top of the javac API, designed for compiling Java sources with customizable class providers. Unlike the traditional Java compiler, CPLC does not use a provided classpath but instead pulls dependencies through an API.

A custom `ClassesProvider` can be implemented:

```java
public interface ClassesProvider {
    Collection<IdentifiedBytecode> getClass(ClassIdentifier... names);
    List<String> getClassPathListing();
}
```

The [provided implementation](https://github.com/mkoncek/classpathless-compiler/blob/master/impl/src/main/java/io/github/mkoncek/classpathless/impl/CompilerJavac.java) of `ClasspathlessCompiler` can then be invoked:

```java
public interface ClasspathlessCompiler {
    Collection<IdentifiedBytecode> compileClass(ClassesProvider classesProvider,
            Optional<MessagesListener> messagesListener, IdentifiedSource... javaSourceFiles);
    // ...
}
```

The compiled classes are returned as a result.

Before compilation, the default implementation scans all **necessary** (only necessary) dependencies from the provided `ClassesProvider` to ensure that all recognized dependencies will be available when javac requires them.

CPLC is particularly useful for scenarios requiring on-the-fly compilation of `.java` files, offering a more convenient alternative to working directly with the javac API.

## Building

Run `mvn install` to build the project.

The project is available in Maven repositories:

```xml
<properties>
    ...
    <cplc.version>2.3</cplc.version>
</properties>

<dependency>
    <groupId>io.github.mkoncek</groupId>
    <artifactId>classpathless-compiler-api</artifactId>
    <version>${cplc.version}</version>
</dependency>
<dependency>
    <groupId>io.github.mkoncek</groupId>
    <artifactId>classpathless-compiler-util</artifactId>
    <version>${cplc.version}</version>
</dependency>
<dependency>
    <groupId>io.github.mkoncek</groupId>
    <artifactId>classpathless-compiler</artifactId>
    <version>${cplc.version}</version>
</dependency>
```

## --patch-module

Since JDK 9, modules can be built instead of classical JARs. Modules ensure stronger encapsulation and restrict **each package** to be a member of **exactly one module**. The JDK itself honors this constraint and is split into several modules. To modify the JDK itself or to compile against any modular application, javac must be informed which classes belong to which modules.

To address this, javac introduced `--patch-module`:

```
javac -X 2>&1 | grep patch -A 2
  --patch-module <module>=<file>(:<file>)*
        Override or augment a module with classes and resources
        in JAR files or directories
```

For CPLC, files and directories have no meaning. CPLC accepts all javac flags (e.g., `--source`/`--target`), but the `--patch-module` flag has been amended. When compiling a class in a package belonging to any module, the `--patch-module` flag must be set. CPLC will intercept this flag and use it internally.

The syntax has been changed to fit the FQN logic that CPLC depends on instead of files and directories. The syntax for `--patch-module` when passed to CPLC is:

```
--patch-module <module>=<fqnOfPkg>(:<fqnOfPkg>)*
      Override or augment a module with classes and resources
      from the given package, specified by fully qualified name
```

For example, to compile `java.lang.Runnable`, the flag `--patch-module java.base=java.lang` must be added to the CPLC compiler flags.

## CLI Tool

This project contains a default implementation of `ClassesProvider` which behaves similarly to `javac` but internally uses the CPLC APIs and the [provided implementation](https://github.com/mkoncek/classpathless-compiler/blob/master/impl/src/main/java/io/github/mkoncek/classpathless/impl/CompilerJavac.java) of `ClasspathlessCompiler`.

### Command Line Options

* `-h` — Display help
* `-cp`, `-classpath` — Specify the classpath
* `-d` — Output directory for the generated `.class` files

## Logging Properties

The following system properties can be defined to configure logging:

* `io.github.mkoncek.cplc.logging=[filename]` — Sets the logging output destination
  * If the property is not defined, logging output is discarded
  * If the value is empty, logs are written to standard error

* `io.github.mkoncek.cplc.loglevel=[[off] | severe | warning | info | config | fine | finer | finest | all]` — Sets the logging level (default is `off`)

* `io.github.mkoncek.cplc.tracing` — Enables detailed logging of each function call; requires `logging` and `loglevel` to be set

* `io.github.mkoncek.cplc.log-to-provider` — Enables passing all logging output to the provider as well
