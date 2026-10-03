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
package consulo.spring.initializr;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import consulo.container.plugin.PluginManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.util.CapturingProcessUtil;
import consulo.process.util.ProcessOutput;
import consulo.spring.initializr.localize.SpringInitializrLocalize;
import consulo.spring.initializr.model.RunnerOutput;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the bundled runner - a Spring Boot application around Spring Initializr - in its own JVM,
 * so Spring never meets the IDE classpath.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public final class SpringInitializrProjectGenerator {
    private static final Logger LOG = Logger.getInstance(SpringInitializrProjectGenerator.class);

    // copied into the plugin directory by the <copies> of plugin/pom.xml
    private static final String RUNNER_JAR = "runner.jar";
    private static final int TIMEOUT_SECONDS = 300;

    /**
     * @param error      null on success
     * @param artifactId name of the generated root project
     * @param warnings   e.g. dependencies skipped as incompatible
     */
    public record Result(@Nullable LocalizeValue error, String artifactId, List<String> warnings) {
        static Result failure(LocalizeValue error) {
            return new Result(error, "", List.of());
        }
    }

    private SpringInitializrProjectGenerator() {
    }

    /**
     * Blocks until the runner exits.
     */
    public static Result generate(SpringInitializrGenerateRequest request, Path outputDir) {
        List<String> command = new ArrayList<>();
        command.add(findJavaExecutable());
        command.add("-jar");
        command.add(new File(PluginManager.getPluginPath(SpringInitializrProjectGenerator.class), RUNNER_JAR).getPath());
        command.add("--generate");
        command.add("--json=" + SpringInitializrMetadataLoader.getMetadataFile().getPath());
        command.add("--output-dir=" + outputDir);
        command.addAll(request.toRunnerArguments());

        ProcessOutput output;
        try {
            output = CapturingProcessUtil.execAndGetOutput(new GeneralCommandLine(command).withCharset(StandardCharsets.UTF_8),
                TIMEOUT_SECONDS * 1000);
        }
        catch (ExecutionException e) {
            LOG.warn("Failed to start Spring Initializr runner", e);
            return Result.failure(LocalizeValue.of(StringUtil.notNullize(e.getMessage(), e.getClass().getName())));
        }

        if (output.isTimeout()) {
            return Result.failure(SpringInitializrLocalize.generationTimeout(TIMEOUT_SECONDS));
        }

        RunnerOutput runnerOutput = parse(output.getStdoutLines(true));
        if (runnerOutput == null) {
            LOG.warn("Spring Initializr runner exited with code " + output.getExitCode() + "\n" + output.getStderr());
            List<String> errorLines = output.getStderrLines(true);
            String lastErrorLine = errorLines.isEmpty() ? "" : errorLines.get(errorLines.size() - 1);
            return Result.failure(SpringInitializrLocalize.generationExitCode(output.getExitCode(), lastErrorLine));
        }

        if (!"ok".equals(runnerOutput.status())) {
            String message = runnerOutput.message();
            // the notification title already tells that the generation failed
            return Result.failure(message == null ? LocalizeValue.empty() : LocalizeValue.of(message));
        }

        String artifactId = runnerOutput.artifactId();
        List<String> warnings = runnerOutput.warnings();
        return new Result(null, artifactId == null ? request.artifactId() : artifactId, warnings == null ? List.of() : warnings);
    }

    private static @Nullable RunnerOutput parse(List<String> stdoutLines) {
        if (stdoutLines.isEmpty()) {
            return null;
        }

        try {
            return new Gson().fromJson(stdoutLines.get(stdoutLines.size() - 1), RunnerOutput.class);
        }
        catch (JsonParseException e) {
            return null;
        }
    }

    private static String findJavaExecutable() {
        Platform platform = Platform.current();
        // the runner needs Java 17+, the IDE runtime is newer
        String javaHome = platform.jvm().getRuntimeProperty("java.home");
        if (javaHome == null) {
            return "java";
        }
        return Path.of(javaHome, "bin", platform.os().isWindows() ? "java.exe" : "java").toString();
    }
}
