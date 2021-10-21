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
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedSet;

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

    private ClassesProvider classprovider = null;
    private SortedSet<String> availableClasses = null;
    private LoggingSwitch loggingSwitch = null;

    private ArrayList<InMemoryJavaClassFileObject> classOutputs = new ArrayList<>();

    public InMemoryFileManager(JavaFileManager delegate) {
        super();
        this.delegate = delegate;
    }

    void setLoggingSwitch(LoggingSwitch loggingSwitch) {
        this.loggingSwitch = loggingSwitch;
    }

    void setClassProvider(ClassesProvider classprovider) {
        this.classprovider = classprovider;
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
            for (var set : result) {
                for (var loc : set) {
                    if (loc.getName().equals("SYSTEM_MODULES[java.base]")) {
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
            /// We do not construct with ClassesProvider because the write will
            /// happen by the caller
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

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName,
            Set<Kind> kinds, boolean recurse) throws IOException {
        loggingSwitch.trace(this, "list", location, packageName, kinds, recurse);

        if (location.equals(StandardLocation.CLASS_PATH)
                || (!arguments.useHostSystemClasses()
                        && location.getName().equals("SYSTEM_MODULES[java.base]"))) {
            var result = new ArrayList<JavaFileObject>();

            if (!packageName.isEmpty()) {
                result.addAll(LoadClasses.loadClasses(availableClasses, packageName, recurse, classprovider, loggingSwitch));
            }

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
