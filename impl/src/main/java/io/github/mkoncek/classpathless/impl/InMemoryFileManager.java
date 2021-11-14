/*-
 * Copyright (c) 2020 Marián Konček
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
package io.github.mkoncek.classpathless.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.ClasspathlessCompiler;

/**
 * @author Marián Konček
 */
public class InMemoryFileManager implements JavaFileManager {
    private JavaFileManager delegate;
    private ClasspathlessCompiler.Arguments arguments = null;

    private ClassesProvider classesProvider = null;
    private SortedSet<String> availableClasses = null;
    private LoggingSwitch loggingSwitch = null;

    private ArrayList<InMemoryJavaClassFileObject> classOutputs = new ArrayList<>();

    // The Location name for system classes in Java < 9
    private static final String HOST_SYSTEM_CLASS_PATH = "PLATFORM_CLASS_PATH";
    // The Location name for system classes in Java >= 9
    private static final String HOST_SYSTEM_MODULES = "SYSTEM_MODULES[java.base]";

    public InMemoryFileManager(JavaFileManager delegate) {
        super();
        this.delegate = delegate;
    }

    void setLoggingSwitch(LoggingSwitch loggingSwitch) {
        this.loggingSwitch = loggingSwitch;
    }

    void setClassesProvider(ClassesProvider classesProvider) {
        this.classesProvider = classesProvider;
    }

    void setAvailableClasses(SortedSet<String> availableClasses) {
        this.availableClasses = availableClasses;
    }

    void setArguments(ClasspathlessCompiler.Arguments arguments) {
        this.arguments = arguments;
    }

    void clearAndGetOutput(Collection<JavaFileObject> classOutput) {
        loggingSwitch.trace(this, "clearAndGetOutput", classOutput);
        classOutput.addAll(classOutputs);
        classOutputs.clear();
        availableClasses.clear();
    }

    @Override
    public Location getLocationForModule(Location location, String moduleName)
            throws IOException {
        loggingSwitch.trace(this, "getLocationForModule", location, moduleName);
        var result = delegate.getLocationForModule(location, moduleName);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public Location getLocationForModule(Location location, JavaFileObject fo)
            throws IOException {
        loggingSwitch.trace(this, "getLocationForModule", location, fo);
        var result = delegate.getLocationForModule(location, fo);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public <S> ServiceLoader<S> getServiceLoader(Location location, Class<S> service)
            throws IOException {
        loggingSwitch.trace(this, "getServiceLoader", location, service);
        var result = delegate.getServiceLoader(location, service);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public String inferModuleName(Location location) throws IOException {
        loggingSwitch.trace(this, "inferModuleName", location);
        String result;
        result = delegate.inferModuleName(location);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public Iterable<Set<Location>> listLocationsForModules(Location location)
            throws IOException {
        loggingSwitch.trace(this, "listLocationsForModules", location);
        Iterable<Set<Location>> result;
        result = delegate.listLocationsForModules(location);

        if (location.equals(StandardLocation.SYSTEM_MODULES) && !arguments.useHostSystemClasses()) {
            // Only expose the one module which contains java.lang package.
            // If the compiler doesn't get it, it will fail and not look for
            // java.lang further for example on classpath.
            // Even though we expose the host module, the invocation of list
            // will return a list of system classes provided by the provider.
            for (var set : result) {
                for (var loc : set) {
                    if (loc.getName().equals(HOST_SYSTEM_MODULES)) {
                        result = Arrays.asList(Set.of(loc));
                        break;
                    }
                }
            }
        }

        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public boolean contains(Location location, FileObject fo) throws IOException {
        loggingSwitch.trace(this, "contains", location, fo);
        var result = delegate.contains(location, fo);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public void close() throws IOException {
        loggingSwitch.trace(this, "close");
        delegate.close();
    }

    @Override
    public void flush() throws IOException {
        loggingSwitch.trace(this, "flush");
        delegate.flush();
    }

    @Override
    public int isSupportedOption(String option) {
        loggingSwitch.trace(this, "isSupportedOption", option);
        var result = delegate.isSupportedOption(option);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public boolean isSameFile(FileObject a, FileObject b) {
        loggingSwitch.trace(this, "isSameFile", a, b);
        var result = delegate.isSameFile(a, b);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        loggingSwitch.trace(this, "getClassLoader", location);
        var result = delegate.getClassLoader(location);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public FileObject getFileForInput(Location location, String packageName,
            String relativeName) throws IOException {
        loggingSwitch.trace(this, "getFileForInput", location, packageName, relativeName);
        var result = delegate.getFileForInput(location, packageName, relativeName);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName,
            String relativeName, FileObject sibling) throws IOException {
        loggingSwitch.trace(this, "getFileForOutput", packageName, relativeName, sibling);
        var result = delegate.getFileForOutput(location, packageName, relativeName, sibling);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location,
            String className, Kind kind) throws IOException {
        loggingSwitch.trace(this, "getJavaFileForInput", location, className, kind);
        var result = delegate.getJavaFileForInput(location, className, kind);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location,
            String className, Kind kind, FileObject sibling) throws IOException {
        loggingSwitch.trace(this, "getJavaFileForOutput", location, className, kind, sibling);
        if (kind.equals(Kind.CLASS) && location.equals(StandardLocation.CLASS_OUTPUT)) {
            // We do not construct with ClassesProvider because the write will
            // happen by the caller
            var result = new InMemoryJavaClassFileObject(className, null);
            loggingSwitch.trace(result);
            classOutputs.add(result);
            return result;
        } else {
            var result = delegate.getJavaFileForOutput(location, className, kind, sibling);
            loggingSwitch.trace(result);
            return result;
        }
    }

    @Override
    public boolean hasLocation(Location location) {
        loggingSwitch.trace(this, "hasLocation", location);
        boolean result;
        result = delegate.hasLocation(location);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        loggingSwitch.trace(this, "inferBinaryName", location, file);
        if (file instanceof InMemoryJavaClassFileObject) {
            var realFile = (InMemoryJavaClassFileObject) file;
            var result = realFile.getClassIdentifier().getFullName();
            loggingSwitch.trace(result);
            return result;
        } else {
            var result = delegate.inferBinaryName(location, file);
            loggingSwitch.trace(result);
            return result;
        }
    }

    /**
     * A utility method which loads all available classes as a collection of
     * file objects for a given package name.
     */
    private Collection<InMemoryJavaClassFileObject> loadClasses(
            String packageName, boolean recurse) throws IOException {
        var result = new ArrayList<InMemoryJavaClassFileObject>();

        for (var availableClassName : availableClasses.tailSet(packageName)) {
            if (!availableClassName.startsWith(packageName)) {
                break;
            }

            if (packageName.isEmpty()) {
                if (availableClassName.contains(".") && !recurse) {
                    loggingSwitch.logln(Level.FINE, "Skipping over class from a package from ClassProvider: \"{0}\"", availableClassName);
                    continue;
                }
            } else if (availableClassName.length() > packageName.length() + 1) {
                if (availableClassName.substring(packageName.length() + 1).contains(".") && !recurse) {
                    loggingSwitch.logln(Level.FINE, "Skipping over class from a subpackage from ClassProvider: \"{0}\"", availableClassName);
                    continue;
                }
            }

            loggingSwitch.logln(Level.FINE, "Loading class from ClassProvider: \"{0}\"", availableClassName);

            result.add(new InMemoryJavaClassFileObject(availableClassName, classesProvider, loggingSwitch));
        }

        return result;
    }

    /**
     * A utility which extracts the fully qualified names of given file objects
     * assuming they are the host system classes returned by the StandardJavaFileManager.
     */
    private Collection<String> hostClassesNames(Iterable<JavaFileObject> jfobjects) {
        var result = new ArrayList<String>();
        for (var jfobject : jfobjects) {
            String name = jfobject.getName();
            // Ignore strange non-class files and module-info.class
            if (name.startsWith("/modules/") && name.endsWith(".class") && !name.contains("-")) {
                name = name.substring(9, name.length() - 6);
                // Ignore "java.base" until the next slash
                int begin = name.indexOf('/') + 1;
                name = name.substring(begin).replace('/', '.');
                result.add(name);
            } else {
                loggingSwitch.logln(Level.FINE, "Skipping over file object: \"{0}\"", name);
            }
        }
        return result;
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName,
            Set<Kind> kinds, boolean recurse) throws IOException {
        loggingSwitch.trace(this, "list", location, packageName, kinds, recurse);

        if (!arguments.useHostSystemClasses()) {
            if (location.getName().equals(HOST_SYSTEM_CLASS_PATH)) {
                // Add all the host visible names to our set of available classes
                // but do not return it, the compiler will later ask for the same
                // package name with the location CLASS_PATH, then we return all.
                for (String name : hostClassesNames(delegate.list(location, packageName, kinds, recurse))) {
                    availableClasses.add(name);
                }
                var result = Collections.<JavaFileObject>emptyList();
                loggingSwitch.trace(result);
                return result;
            } else if (location.getName().equals(HOST_SYSTEM_MODULES)) {
                // In this case we need to return the whole set of classes
                // because the compiler will not ask for host classes with
                // Location == CLASS_PATH, due to different nature of modules
                var result = new TreeSet<JavaFileObject>((var lhs, var rhs) -> {
                    return ((InMemoryJavaClassFileObject)(lhs)).getClassIdentifier().compareTo(((InMemoryJavaClassFileObject)(rhs)).getClassIdentifier());
                });
                result.addAll(loadClasses(packageName, recurse));
                for (String name : hostClassesNames(delegate.list(location, packageName, kinds, recurse))) {
                    result.add(new InMemoryJavaClassFileObject(name, classesProvider, loggingSwitch));
                }
                loggingSwitch.trace(result);
                return result;
            }
        }

        if ((!arguments.useHostSystemClasses() && location.getName().equals(HOST_SYSTEM_CLASS_PATH))
                || location.equals(StandardLocation.CLASS_PATH)) {
            var result = new ArrayList<JavaFileObject>();
            result.addAll(loadClasses(packageName, recurse));
            loggingSwitch.trace(result);
            return result;
        } else {
            var result = delegate.list(location, packageName, kinds, recurse);
            loggingSwitch.trace(result);
            return result;
        }
    }

    @Override
    public boolean handleOption(String current, Iterator<String> remaining) {
        loggingSwitch.trace(this, "handleOption", current, remaining);
        var result = delegate.handleOption(current, remaining);
        loggingSwitch.trace(result);
        return result;
    }
}
