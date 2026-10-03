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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What the wizard asks the runner to generate, see {@code InitializrCommandLineRunner} for the options.
 *
 * @param packageName   empty - derived from group and artifact by the runner
 * @param type          Initializr project type, e.g. {@code maven-project}
 * @param buildSystemId {@code build} tag of the type: {@code maven} or {@code gradle}
 * @author VISTALL
 * @since 2026-10-03
 */
public record SpringInitializrGenerateRequest(String name,
                                              String groupId,
                                              String artifactId,
                                              String packageName,
                                              String bootVersion,
                                              String javaVersion,
                                              String language,
                                              String type,
                                              @Nullable String buildSystemId,
                                              String packaging,
                                              List<String> dependencyIds) {
    public List<String> toRunnerArguments() {
        List<String> arguments = new ArrayList<>();
        arguments.add("--name=" + name);
        arguments.add("--group-id=" + groupId);
        arguments.add("--artifact-id=" + artifactId);
        if (!packageName.isBlank()) {
            arguments.add("--package-name=" + packageName);
        }
        arguments.add("--boot-version=" + bootVersion);
        arguments.add("--java-version=" + javaVersion);
        arguments.add("--language=" + language);
        arguments.add("--type=" + type);
        arguments.add("--packaging=" + packaging);
        if (!dependencyIds.isEmpty()) {
            arguments.add("--dependencies=" + String.join(",", dependencyIds));
        }
        return arguments;
    }
}
