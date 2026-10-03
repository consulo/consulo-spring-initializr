import org.jspecify.annotations.NullMarked;

/**
 * Loaded only when the Maven plugin is enabled, see META-INF/plugin-requires.xml.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
@NullMarked
module consulo.spring.initializr.maven.impl {
    requires consulo.annotation;
    requires consulo.module.api;
    requires consulo.project.api;
    requires consulo.ui.api;
    requires consulo.virtual.file.system.api;

    requires consulo.spring.initializr.api;
    requires org.jetbrains.idea.maven;
}
