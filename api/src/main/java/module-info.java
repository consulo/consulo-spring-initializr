import org.jspecify.annotations.NullMarked;

/**
 * Extension points implemented by the optional build system integrations (Maven, Gradle).
 *
 * @author VISTALL
 * @since 2026-10-03
 */
@NullMarked
module consulo.spring.initializr.api {
    requires consulo.component.api;
    requires consulo.project.api;
    requires consulo.module.api;
    requires consulo.virtual.file.system.api;
    requires consulo.ui.api;

    exports consulo.spring.initializr.api;
}
