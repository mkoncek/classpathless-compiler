package io.github.mkoncek.classpathless;

import org.junit.jupiter.api.Test;

public class ToolTest {
    @Test
    public void testSimple() throws Exception {
        Tool.main(new String[] {"src/test/resources/tool/Main.java"});
    }

    @Test
    public void testPatchModuleSimple() throws Exception {
        Tool.main(new String[] {"--patch-module", "java.base=java.lang", "src/test/resources/tool/Runnable.java"});
    }

    @Test
    public void testPatchModuleLogging() throws Exception {
        Tool.main(new String[] {"--patch-module", "java.logging=java.util.logging", "src/test/resources/tool/Filter.java"});
    }

    @Test
    public void testPatchModuleMultipleLeft() throws Exception {
        Tool.main(new String[] {"--patch-module", "java.base=java.lang", "--patch-module", "java.logging=java.util.logging", "src/test/resources/tool/Runnable.java"});
    }

    @Test
    public void testPatchModuleMultipleRight() throws Exception {
        Tool.main(new String[] {"--patch-module", "java.base=java.lang", "--patch-module", "java.logging=java.util.logging", "src/test/resources/tool/Filter.java"});
    }
}
