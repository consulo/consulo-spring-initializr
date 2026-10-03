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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.spring.initializr.metadata.InitializrProperties;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonInitializrProperties extends InitializrProperties {
    private static final ObjectMapper MAPPER = createMapper();

    private static ObjectMapper createMapper() {
        return JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // capabilities are final fields exposed by getters only
                .enable(MapperFeature.USE_GETTERS_AS_SETTERS)
                .enable(MapperFeature.INFER_PROPERTY_MUTATORS)
                .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                .annotationIntrospector(new JacksonAnnotationIntrospector() {
                    @Override
                    public boolean hasIgnoreMarker(MapperConfig<?> config, AnnotatedMember m) {
                        // InitializrProperties hides its capabilities from JSON, but the metadata file is made of them
                        return false;
                    }
                })
                .build();
    }

    public static InitializrProperties fromJson(Path jsonPath) {
        return MAPPER.readValue(jsonPath.toFile(), JsonInitializrProperties.class);
    }
}
