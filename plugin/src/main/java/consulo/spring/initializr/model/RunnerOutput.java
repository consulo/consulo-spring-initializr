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
package consulo.spring.initializr.model;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The single JSON line printed by the runner, see {@code InitializrCommandLineRunner}.
 *
 * @param status     {@code ok} or {@code error}
 * @param message    set on error
 * @param artifactId set on success, after cleanup
 */
public record RunnerOutput(@Nullable String status,
                           @Nullable String message,
                           @Nullable String artifactId,
                           @Nullable List<String> warnings) {
}
