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

import consulo.module.creation.NewModuleWizardContextBase;
import consulo.spring.initializr.model.DependencyItem;
import consulo.spring.initializr.model.MetadataItem;
import consulo.spring.initializr.model.MetadataResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-04-14
 */
public class SpringInitializrNewModuleWizardContext extends NewModuleWizardContextBase {
    private @Nullable MetadataResult myMetadata;

    private @Nullable MetadataItem myBootVersion;
    private @Nullable MetadataItem myJavaVersion;
    private @Nullable MetadataItem myLanguage;
    private @Nullable MetadataItem myType;
    private @Nullable MetadataItem myPackaging;

    private String myGroupId = "com.example";
    private String myArtifactId = "";
    private String myPackageName = "";

    private final Set<DependencyItem> mySelectedDependencies = new LinkedHashSet<>();

    public SpringInitializrNewModuleWizardContext(boolean isNewProject) {
        super(isNewProject);
    }

    public @Nullable MetadataResult getMetadata() {
        return myMetadata;
    }

    public void setMetadata(MetadataResult metadata) {
        myMetadata = metadata;
    }

    public @Nullable MetadataItem getBootVersion() {
        return myBootVersion;
    }

    public void setBootVersion(@Nullable MetadataItem bootVersion) {
        myBootVersion = bootVersion;
    }

    public @Nullable MetadataItem getJavaVersion() {
        return myJavaVersion;
    }

    public void setJavaVersion(@Nullable MetadataItem javaVersion) {
        myJavaVersion = javaVersion;
    }

    public @Nullable MetadataItem getLanguage() {
        return myLanguage;
    }

    public void setLanguage(@Nullable MetadataItem language) {
        myLanguage = language;
    }

    public @Nullable MetadataItem getType() {
        return myType;
    }

    public void setType(@Nullable MetadataItem type) {
        myType = type;
    }

    public @Nullable MetadataItem getPackaging() {
        return myPackaging;
    }

    public void setPackaging(@Nullable MetadataItem packaging) {
        myPackaging = packaging;
    }

    public String getGroupId() {
        return myGroupId;
    }

    public void setGroupId(String groupId) {
        myGroupId = groupId;
    }

    /**
     * @return entered artifact, empty means the project name
     */
    public String getArtifactId() {
        return myArtifactId;
    }

    public void setArtifactId(String artifactId) {
        myArtifactId = artifactId;
    }

    /**
     * @return entered package, empty means derived from group and artifact
     */
    public String getPackageName() {
        return myPackageName;
    }

    public void setPackageName(String packageName) {
        myPackageName = packageName;
    }

    public Set<DependencyItem> getSelectedDependencies() {
        return mySelectedDependencies;
    }

    /**
     * @return null until the metadata is loaded and every option is chosen
     */
    public @Nullable SpringInitializrGenerateRequest createRequest() {
        MetadataItem bootVersion = myBootVersion;
        MetadataItem javaVersion = myJavaVersion;
        MetadataItem language = myLanguage;
        MetadataItem type = myType;
        MetadataItem packaging = myPackaging;
        if (bootVersion == null || javaVersion == null || language == null || type == null || packaging == null) {
            return null;
        }

        // the Boot version may have been changed after the dependencies were picked
        List<String> dependencyIds = new ArrayList<>();
        for (DependencyItem dependency : mySelectedDependencies) {
            if (dependency.isCompatibleWith(bootVersion.id())) {
                dependencyIds.add(dependency.id());
            }
        }

        String name = getName();
        return new SpringInitializrGenerateRequest(
            name,
            myGroupId,
            myArtifactId.isBlank() ? name : myArtifactId,
            myPackageName,
            bootVersion.id(),
            javaVersion.id(),
            language.id(),
            type.id(),
            type.getBuildSystemId(),
            packaging.id(),
            dependencyIds
        );
    }
}
