package com.elavon.migrator.mcp.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SpecLocator {
    private SpecLocator() {}

    public static Path resolveElavonSpec() {
        // 1) Env/prop override
        String env = System.getProperty("TARGET_OPENAPI", System.getenv("TARGET_OPENAPI"));
        if (env != null && !env.isBlank()) {
            Path p = Paths.get(env);
            if (Files.exists(p)) return p;
        }
        // 2) Default workspace location
        Path fs = Paths.get("tools/mcp-payments-migrator/specs/elavon.json");
        if (Files.exists(fs)) return fs.toAbsolutePath();
        // 3) Classpath fallback copied to temp file
        return extractClasspath("/specs/elavon.json", "elavon.json");
    }

    public static Path resolveConvergeSpec() {
        String env = System.getProperty("SOURCE_OPENAPI", System.getenv("SOURCE_OPENAPI"));
        if (env != null && !env.isBlank()) {
            Path p = Paths.get(env);
            if (Files.exists(p)) return p;
        }
        Path fs = Paths.get("tools/mcp-payments-migrator/specs/converge.json");
        if (Files.exists(fs)) return fs.toAbsolutePath();
        return extractClasspath("/specs/converge.json", "converge.json");
    }

    private static Path extractClasspath(String resourcePath, String fileName) {
        try (InputStream in = SpecLocator.class.getResourceAsStream(resourcePath)) {
            if (in == null) return Paths.get(fileName); // last resort
            Path tmpDir = Paths.get("tools/mcp-payments-migrator/target/specs-cache");
            Files.createDirectories(tmpDir);
            Path dest = tmpDir.resolve(fileName);
            Files.copy(in, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return dest;
        } catch (IOException e) {
            return Paths.get(fileName);
        }
    }
}


