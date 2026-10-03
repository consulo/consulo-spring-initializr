/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.spring.initializr.runner;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectGenerationTest {
    private static final Path TEST_METADATA = Path.of("src/test/resources/test-metadata.json");
    // the file shipped inside the plugin
    private static final Path BUNDLED_METADATA = Path.of("../plugin/src/main/dist/spring-initializr-metadata.json");

    @TempDir
    Path tempDir;

    private record Result(JsonObject json, int exitCode) {
        String status() {
            return json.get("status").getAsString();
        }
    }

    private Result generate(Path metadata, Path outputDir, String... options) {
        List<String> args = new ArrayList<>();
        args.add("--generate");
        args.add("--json=" + metadata.toAbsolutePath());
        args.add("--output-dir=" + outputDir);
        args.addAll(List.of(options));

        PrintStream originalOut = System.out;
        ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true));
        int exitCode;
        try {
            ConfigurableApplicationContext context = new SpringApplication(SpringInitializrRunnerApplication.class).run(args.toArray(String[]::new));
            exitCode = SpringApplication.exit(context);
        }
        finally {
            System.setOut(originalOut);
        }

        String output = capturedOut.toString().trim();
        assertFalse(output.contains("\n"), "stdout must carry the result JSON only, got: " + output);
        return new Result(new Gson().fromJson(output, JsonObject.class), exitCode);
    }

    private static JsonObject bundledMetadata() throws IOException {
        return new Gson().fromJson(Files.readString(BUNDLED_METADATA), JsonObject.class);
    }

    private static String defaultBootVersion(JsonObject metadata) {
        for (JsonElement element : metadata.getAsJsonArray("bootVersions")) {
            if (element.getAsJsonObject().get("default").getAsBoolean()) {
                return element.getAsJsonObject().get("id").getAsString();
            }
        }
        throw new AssertionError("No default boot version");
    }

    @Test
    void generateMavenProject() throws Exception {
        Path outputDir = tempDir.resolve("output");

        Result result = generate(TEST_METADATA, outputDir,
                "--group-id=com.test",
                "--artifact-id=myapp",
                "--boot-version=3.5.0",
                "--java-version=21",
                "--build-system=maven",
                "--packaging=jar",
                "--dependencies=web");

        assertEquals("ok", result.status(), result.json().toString());
        assertEquals(0, result.exitCode());

        assertTrue(Files.exists(outputDir.resolve("src/main/java/com/test/myapp/MyappApplication.java")), "Main class should exist");

        String pomContent = Files.readString(outputDir.resolve("pom.xml"));
        assertTrue(pomContent.contains("spring-boot-starter-web"), "pom.xml should contain web starter");
        assertTrue(pomContent.contains("<groupId>com.test</groupId>"), "pom.xml should contain groupId");
        assertTrue(pomContent.contains("<version>0.0.1-SNAPSHOT</version>"), "pom.xml should contain the project version");
    }

    @Test
    void generateGradleProject() throws Exception {
        Path outputDir = tempDir.resolve("output");

        Result result = generate(TEST_METADATA, outputDir,
                "--boot-version=3.5.0",
                "--java-version=17",
                "--build-system=gradle",
                "--dependencies=data-jpa,h2");

        assertEquals("ok", result.status(), result.json().toString());

        String buildGradle = Files.readString(outputDir.resolve("build.gradle"));
        assertTrue(buildGradle.contains("spring-boot-starter-data-jpa"), "Should contain data-jpa");
        assertTrue(buildGradle.contains("com.h2database:h2"), "Should contain h2");
    }

    @Test
    void bundledMetadataResolvesBoms() throws Exception {
        JsonObject metadata = bundledMetadata();
        Path outputDir = tempDir.resolve("output");

        Result result = generate(BUNDLED_METADATA, outputDir,
                "--type=maven-project",
                "--boot-version=" + defaultBootVersion(metadata),
                "--java-version=17",
                "--dependencies=web,modulith");

        assertEquals("ok", result.status(), result.json().toString());

        String pomContent = Files.readString(outputDir.resolve("pom.xml"));
        assertTrue(pomContent.contains("<version>" + defaultBootVersion(metadata) + "</version>"), "Boot parent version");
        assertTrue(pomContent.contains("spring-boot-starter-webmvc"), "web starter");
        assertTrue(pomContent.contains("spring-modulith-bom"), "modulith BOM should be imported");
    }

    @Test
    void bundledMetadataGeneratesKotlinGradleProject() throws Exception {
        JsonObject metadata = bundledMetadata();
        Path outputDir = tempDir.resolve("output");

        Result result = generate(BUNDLED_METADATA, outputDir,
                "--type=gradle-project-kotlin",
                "--language=kotlin",
                "--boot-version=" + defaultBootVersion(metadata),
                "--java-version=21");

        assertEquals("ok", result.status(), result.json().toString());
        assertTrue(Files.exists(outputDir.resolve("build.gradle.kts")), "Kotlin DSL build script");
        assertTrue(Files.exists(outputDir.resolve("src/main/kotlin/com/example/demo/DemoApplication.kt")), "Kotlin main class");
        String wrapper = Files.readString(outputDir.resolve("gradle/wrapper/gradle-wrapper.properties"));
        assertTrue(wrapper.contains("gradle-9."), "Spring Boot 4 needs Gradle 9: " + wrapper);
    }

    @Test
    void cleansPackageAndApplicationName() throws Exception {
        Path outputDir = tempDir.resolve("output");

        Result result = generate(BUNDLED_METADATA, outputDir,
                "--type=maven-project",
                "--artifact-id=my-app");

        assertEquals("ok", result.status(), result.json().toString());
        assertTrue(Files.exists(outputDir.resolve("src/main/java/com/example/my_app/MyAppApplication.java")),
                "Package and class names must be valid Java identifiers");
    }

    @Test
    void skipsIncompatibleDependency() throws Exception {
        JsonObject metadata = bundledMetadata();
        List<String> bootVersions = new ArrayList<>();
        metadata.getAsJsonArray("bootVersions").forEach(it -> bootVersions.add(it.getAsJsonObject().get("id").getAsString()));

        String dependencyId = null;
        String bootVersion = null;
        for (JsonElement group : metadata.getAsJsonArray("dependencies")) {
            for (JsonElement element : group.getAsJsonObject().getAsJsonArray("content")) {
                JsonObject dependency = element.getAsJsonObject();
                JsonArray compatible = dependency.getAsJsonArray("compatibleBootVersions");
                for (String version : bootVersions) {
                    if (dependencyId == null && dependency.has("compatibilityRange") && !compatible.toString().contains("\"" + version + "\"")) {
                        dependencyId = dependency.get("id").getAsString();
                        bootVersion = version;
                    }
                }
            }
        }
        assertNotNull(dependencyId, "Bundled metadata should have a dependency limited to some Boot versions");

        Result result = generate(BUNDLED_METADATA, tempDir.resolve("output"),
                "--type=maven-project",
                "--boot-version=" + bootVersion,
                "--dependencies=web," + dependencyId);

        assertEquals("ok", result.status(), result.json().toString());
        assertTrue(result.json().getAsJsonArray("warnings").toString().contains(dependencyId), result.json().toString());
    }

    @Test
    void reportsErrorAsJson() {
        Result result = generate(BUNDLED_METADATA, tempDir.resolve("output"), "--type=unknown-project");

        assertEquals("error", result.status());
        assertEquals(1, result.exitCode());
        assertTrue(result.json().get("message").getAsString().contains("unknown-project"), result.json().toString());
    }

    @Test
    void cleansMavenCoordinate() {
        assertEquals("my-app", InitializrCommandLineRunner.cleanMavenCoordinate("my app", "-"));
        assertEquals("com.example", InitializrCommandLineRunner.cleanMavenCoordinate("com.example", "."));
    }
}
