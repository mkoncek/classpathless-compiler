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
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.stream.Stream;

/**
 * @author Marián Konček
 */
public class DelegatingClassLoader extends ClassLoader {
    LoggingSwitch loggingSwitch;

    public DelegatingClassLoader(ClassLoader delegate, LoggingSwitch loggingSwitch) {
        super("Delegating" + delegate.getName(), delegate);
        this.loggingSwitch = loggingSwitch;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        loggingSwitch.trace(this, "loadClass", name);
        var result = super.loadClass(name);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public URL getResource(String name) {
        loggingSwitch.trace(this, "getResource", name);
        var result = super.getResource(name);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        loggingSwitch.trace(this, "getResources", name);
        var result = super.getResources(name);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public Stream<URL> resources(String name) {
        loggingSwitch.trace(this, "resources", name);
        var result = super.resources(name);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        loggingSwitch.trace(this, "getResourceAsStream", name);
        var result = super.getResourceAsStream(name);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    public void setDefaultAssertionStatus(boolean enabled) {
        loggingSwitch.trace(this, "setDefaultAssertionStatus", enabled);
        super.setDefaultAssertionStatus(enabled);
    }

    @Override
    public void setPackageAssertionStatus(String packageName, boolean enabled) {
        loggingSwitch.trace(this, "setPackageAssertionStatus", packageName, enabled);
        super.setPackageAssertionStatus(packageName, enabled);
    }

    @Override
    public void setClassAssertionStatus(String className, boolean enabled) {
        loggingSwitch.trace(this, "setClassAssertionStatus", className, enabled);
        super.setClassAssertionStatus(className, enabled);
    }

    @Override
    public void clearAssertionStatus() {
        loggingSwitch.trace(this, "clearAssertionStatus");
        super.clearAssertionStatus();
    }

    @Override
    protected Package definePackage(String name, String specTitle, String specVersion, String specVendor,
            String implTitle, String implVersion, String implVendor, URL sealBase) {
        loggingSwitch.trace(this, "definePackage", name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
        var result = super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    protected Class<?> findClass(String moduleName, String name) {
        loggingSwitch.trace(this, "findClass", moduleName, name);
        var result = super.findClass(moduleName, name);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        loggingSwitch.trace(this, "findClass", name);
        var result = super.findClass(name);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    protected String findLibrary(String libname) {
        loggingSwitch.trace(this, "findLibrary", libname);
        var result = super.findLibrary(libname);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    protected URL findResource(String moduleName, String name) throws IOException {
        loggingSwitch.trace(this, "findResource", moduleName, name);
        var result = super.findResource(moduleName, name);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    protected URL findResource(String name) {
        loggingSwitch.trace(this, "findResource", name);
        var result = super.findResource(name);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        loggingSwitch.trace(this, "findResources", name);
        var result = super.findResources(name);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    protected Object getClassLoadingLock(String className) {
        loggingSwitch.trace(this, "getClassLoadingLock", className);
        var result = super.getClassLoadingLock(className);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    protected Package getPackage(String name) {
        loggingSwitch.trace(this, "getPackage", name);
        var result = super.getPackage(name);
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    protected Package[] getPackages() {
        loggingSwitch.trace(this, "getPackages");
        var result = super.getPackages();
        loggingSwitch.trace(result);
        return result;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        loggingSwitch.trace(this, "loadClass", name, resolve);
        var result = super.loadClass(name);
        loggingSwitch.trace(result);
        return result;
    }
}
