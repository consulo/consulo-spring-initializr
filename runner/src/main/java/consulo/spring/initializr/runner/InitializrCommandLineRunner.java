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
import com.google.gson.GsonBuilder;
import io.spring.initializr.generator.buildsystem.BuildSystem;
import io.spring.initializr.generator.language.Language;
import io.spring.initializr.generator.packaging.Packaging;
import io.spring.initializr.generator.project.DefaultProjectAssetGenerator;
import io.spring.initializr.generator.project.MutableProjectDescription;
import io.spring.initializr.generator.project.ProjectGenerator;
import io.spring.initializr.generator.version.Version;
import io.spring.initializr.metadata.DefaultMetadataElement;
import io.spring.initializr.metadata.Dependency;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.metadata.Type;
import io.spring.initializr.metadata.support.MetadataBuildItemMapper;
import io.spring.initializr.metadata.support.MetadataBuildItemResolver;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Generates a project and prints a single JSON line to stdout:
 * {@code {"status":"ok","outputDir":...,"warnings":[...]}} or {@code {"status":"error","message":...}}.
 */
@Component
public class InitializrCommandLineRunner implements CommandLineRunner, ExitCodeGenerator {
    private static final String DEFAULT_GROUP_ID = "com.example";
    private static final String DEFAULT_ARTIFACT_ID = "demo";
    private static final String DEFAULT_VERSION = "0.0.1-SNAPSHOT";
    private static final String DEFAULT_DESCRIPTION = "Demo project for Spring Boot";

    private final ApplicationContext applicationContext;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private int exitCode;

    public InitializrCommandLineRunner(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            if (args.length == 0 || !"--generate".equals(args[0])) {
                throw new IllegalArgumentException("Usage: --generate --json=<path> --output-dir=<path> [options]");
            }

            List<String> warnings = new ArrayList<>();
            Generated generated = generate(parseArgs(args), warnings);

            result.put("status", "ok");
            result.put("outputDir", generated.projectDir().toString());
            result.put("artifactId", generated.artifactId());
            result.put("warnings", warnings);
        }
        catch (Exception e) {
            exitCode = 1;
            result.put("status", "error");
            result.put("message", describe(e));
        }
        System.out.println(gson.toJson(result));
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    /**
     * @param artifactId after cleanup - the name of the root project in the generated build
     */
    private record Generated(Path projectDir, String artifactId) {
    }

    private Generated generate(Map<String, String> params, List<String> warnings) throws IOException {
        Path outputDir = Path.of(required(params, "output-dir"));
        InitializrMetadata metadata = JsonMetadataLoader.load(Path.of(required(params, "json")));

        Version platformVersion = Version.parse(paramOrDefault(params, "boot-version", () -> metadata.getBootVersions().getDefault()));
        Language language = Language.forId(params.getOrDefault("language", "java"),
                paramOrDefault(params, "java-version", () -> metadata.getJavaVersions().getDefault()));

        MutableProjectDescription description = new MutableProjectDescription();
        description.setPlatformVersion(platformVersion);
        description.setLanguage(language);
        description.setBuildSystem(resolveBuildSystem(metadata, params));
        description.setPackaging(Packaging.forId(params.getOrDefault("packaging", "jar")));

        String groupId = cleanMavenCoordinate(params.getOrDefault("group-id", DEFAULT_GROUP_ID), ".");
        String artifactId = cleanMavenCoordinate(params.getOrDefault("artifact-id", DEFAULT_ARTIFACT_ID), "-");
        String name = params.getOrDefault("name", artifactId);
        description.setGroupId(groupId);
        description.setArtifactId(artifactId);
        description.setVersion(params.getOrDefault("version", DEFAULT_VERSION));
        description.setName(name);
        description.setDescription(params.getOrDefault("description", DEFAULT_DESCRIPTION));
        description.setPackageName(metadata.getConfiguration().cleanPackageName(
                params.getOrDefault("package-name", groupId + "." + artifactId), language, DEFAULT_GROUP_ID + "." + DEFAULT_ARTIFACT_ID));
        description.setApplicationName(metadata.getConfiguration().generateApplicationName(name));

        addDependencies(description, metadata, platformVersion, params.get("dependencies"), warnings);

        Files.createDirectories(outputDir);

        ProjectGenerator projectGenerator = new ProjectGenerator(context -> {
            context.setParent(applicationContext);
            context.registerBean(InitializrMetadata.class, () -> metadata);
            context.registerBean(MetadataBuildItemResolver.class, () -> new MetadataBuildItemResolver(metadata, platformVersion));
        });
        Path projectDir = projectGenerator.generate(description, new DefaultProjectAssetGenerator(d -> outputDir));
        return new Generated(projectDir, artifactId);
    }

    private static void addDependencies(MutableProjectDescription description,
                                        InitializrMetadata metadata,
                                        Version platformVersion,
                                        String dependencies,
                                        List<String> warnings) {
        if (dependencies == null || dependencies.isBlank()) {
            return;
        }

        for (String rawId : dependencies.split(",")) {
            String id = rawId.trim();
            if (id.isEmpty()) {
                continue;
            }

            Dependency dependency = metadata.getDependencies().get(id);
            if (dependency == null) {
                warnings.add("Unknown dependency '" + id + "', skipped");
                continue;
            }
            if (!dependency.match(platformVersion)) {
                warnings.add("Dependency '" + id + "' is not compatible with Spring Boot " + platformVersion + ", skipped");
                continue;
            }

            description.addDependency(id, MetadataBuildItemMapper.toDependency(dependency.resolve(platformVersion)));
        }
    }

    private static BuildSystem resolveBuildSystem(InitializrMetadata metadata, Map<String, String> params) {
        String typeId = params.get("type");
        if (typeId == null) {
            return BuildSystem.forIdAndDialect(params.getOrDefault("build-system", "maven"), params.get("build-dialect"));
        }

        Type type = metadata.getTypes().get(typeId);
        if (type == null) {
            throw new IllegalArgumentException("Unknown project type '" + typeId + "'");
        }
        return BuildSystem.forIdAndDialect(type.getTags().get("build"), type.getTags().get("dialect"));
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            int eq = arg.indexOf('=');
            if (arg.startsWith("--") && eq > 0) {
                params.put(arg.substring(2, eq), arg.substring(eq + 1));
            }
        }
        return params;
    }

    private static String required(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("--" + key + " is required");
        }
        return value;
    }

    private static String paramOrDefault(Map<String, String> params, String key, Supplier<DefaultMetadataElement> defaultElement) {
        String value = params.get(key);
        if (value != null) {
            return value;
        }

        DefaultMetadataElement element = defaultElement.get();
        if (element == null) {
            throw new IllegalArgumentException("--" + key + " is required, the metadata has no default for it");
        }
        return element.getId();
    }

    /**
     * Same rules as start.spring.io: drops characters Maven does not accept and joins the remaining words with the delimiter.
     */
    static String cleanMavenCoordinate(String coordinate, String delimiter) {
        String[] elements = coordinate.split("[^\\w\\-.]+");
        if (elements.length == 1) {
            return coordinate;
        }

        StringBuilder builder = new StringBuilder();
        for (String element : elements) {
            if (element.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != delimiter.charAt(0)) {
                builder.append(delimiter);
            }
            builder.append(element);
        }
        return builder.toString();
    }

    private static String describe(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message != null ? message : root.getClass().getName();
    }
}
