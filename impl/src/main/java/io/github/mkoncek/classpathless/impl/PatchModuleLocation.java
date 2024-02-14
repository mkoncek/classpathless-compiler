package io.github.mkoncek.classpathless.impl;

import javax.tools.JavaFileManager.Location;

public class PatchModuleLocation implements Location {
    private String moduleName;
    private String name;

    public PatchModuleLocation(String moduleName, String name) {
        this.moduleName = moduleName;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isOutputLocation() {
        return false;
    }

    public String getModuleName() {
        return moduleName;
    }
}
