# classpathless-compiler

Classpathless compiler (CPLC) is a compiler wrapper used for compiling java
sources with customizable class providers. This tool works differently from the
traditional java compiler in that it doesn't use provided classpath but instead
pulls dependencies using an API.

## CLI tool

This tool behaves similar to `javac` but it uses its own API to resolve imports.

Command line options:

* `-h` - help
* `-cp`, `-classpath` - Specify classpath
* `-d` - Output directory where to put the `.class` files

## Logging properties

In order to use them, define the following system properties.

* `io.github.mkoncek.cplc.logging=[filename]` - sets the logging output
  * if not provided, logging output is discarded
  * if empty, logs into `stderr`

* `io.github.mkoncek.cplc.loglevel=[[off] | severe | warning | info | config | fine | finer | finest | all]` -
sets the level of logging, default is `off`

* `io.github.mkoncek.cplc.tracing` - enables very detailed logging of each
function call, requires `logging` and `loglevel` to be set

* `io.github.mkoncek.cplc.log-to-provider` - makes CPLC pass all the logging
into the provider as well
