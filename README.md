# classpathless-compiler

Classpathless compiler (CPLC) is a powerfull compiler wrapper above javac API used for compiling java
sources with customizable class providers. This tool works differently from the traditional java compiler
in that it doesn't use provided classpath but instead pulls dependencies using an API:
You can implement your own:
```
public interface ClassesProvider {
    Collection<IdentifiedBytecode> getClass(ClassIdentifier... names);
    List<String> getClassPathListing();
}
```
and then call [our](https://github.com/mkoncek/classpathless-compiler/blob/master/impl/src/main/java/io/github/mkoncek/classpathless/impl/CompilerJavac.java) implemenation of:
```
public interface ClasspathlessCompiler {
   Collection<IdentifiedBytecode> compileClass(ClassesProvider classesProvider,
            Optional<MessagesListener> messagesListener, IdentifiedSource... javaSourceFiles);
   ....
}
```
And you will recieve compiled classes.

Before compilation itself, the default implementation in this project, is scanning all **necessary** (only necessary) dependecies from your
`ClassesProvider` to ensure, all rcognized depndencies willbe there once javac needs them.

If you need form some reason to work with `.java` filles and compile them on the fly CPLC is the way to go, instead of torturing yuourself with javac api.

## Building

Simply run `mvn install`.

## --patch-module

Since JDK9, you can build modules intead of classical jars. The modules ensure harder encapsualtion, and are restricting **each package** to be meber of **exactly one module**.
JDK itself is honouring this, and is split to several modules. So to hack JDK itself, or to compile agaisnt any generic module application, you need to tell javac whch classes belongs to which modules.

To work with this, javac introduced --patch-module:
```
javac -X 2>&1 | grep patch -A 2
  --patch-module <module>=<file>(:<file>)*
        Override or augment a module with classes and resources
        in JAR files or directories
```
For a CPLC, the files and dirs have no meaning. CPLC is accepting all javac flags (eg --source/--target...), but it had to amend the `--patch-module`.
If you need to compile class in package belonging to any module, you have to set `--patch-module` flag. However CPLC will remove it and use on its own.
In addition, we had to change the syntax, to fit to FQN logic which CPLC depends on instead of files and dirs. Syntax for `--patch-module` when passed to CPLC is:

```
javac -X 2>&1 | grep patch -A 2
  --patch-module <module>=<fqnOfPkg>(:<fqnOfPkg>)*
        Override or augment a module with classes and resources
        which are from given package, provided by fully qualified way
```
Eg: to compile `java.lang.Runnable` you need to add `--patch-module java.base=java.lang` to additional CPLC compiler flags
## CLI tool

This project contains default implementation of `ClassesProvider` which behaves similar to `javac` but inisde it uses our APIs and [our](https://github.com/mkoncek/classpathless-compiler/blob/master/impl/src/main/java/io/github/mkoncek/classpathless/impl/CompilerJavac.java) implemenation of `ClasspathlessCompiler`.

Command line options:

* `-h` - help
* `-cp`, `-classpath` - Specify classpath
* `-d` - Output directory where to put the `.class` files

## Logging properties

In order to use them, define the following system properties.

* `io.github.mkoncek.cplc.logging=[filename]` - sets the logging output
  * if the property is not defined, logging output is discarded
  * if the value is empty, logs into the standard error output

* `io.github.mkoncek.cplc.loglevel=[[off] | severe | warning | info | config | fine | finer | finest | all]` -
sets the level of logging, default is `off`

* `io.github.mkoncek.cplc.tracing` - enables very detailed logging of each
function call, requires `logging` and `loglevel` to be set

* `io.github.mkoncek.cplc.log-to-provider` - makes CPLC pass all the logging
into the provider as well
