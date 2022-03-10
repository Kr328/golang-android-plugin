package com.github.kr328.gradle.golang;

import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.api.variant.DslExtension;
import com.android.build.api.variant.ExternalNativeBuild;
import com.android.build.api.variant.Variant;
import com.android.build.gradle.BaseExtension;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class ProjectPlugin implements Plugin<Project> {
    private static String capitalize(String str) {
        return Arrays.stream(str.split("[-_]"))
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining());
    }

    private static File getOutputDir(Project project, String name) {
        return Paths.get(project.getBuildDir().getAbsolutePath(), "outputs", "golang", name)
                .toAbsolutePath().toFile();
    }

    private static void decorateVariant(Project project, Set<String> abiFilters, String name, BuildConfig config) {
        final File ndkDirectory = project.getExtensions().getByType(BaseExtension.class).getNdkDirectory();

        for (final String abi : abiFilters) {
            final TaskProvider<BuildTask> buildTask = project.getTasks().register(
                    String.format("externalGolangBuild%s[%s]", capitalize(name), abi),
                    BuildTask.class,
                    (task) -> {
                        task.getNdkDirectory().set(ndkDirectory);

                        if (config.getModuleDirectory() == null) {
                            task.getModuleDir().set(project.file(Paths.get("src", "main", "golang").toString()));
                        } else {
                            task.getModuleDir().set(project.file(config.getModuleDirectory()));
                        }

                        if (config.getPackageName() == null) {
                            task.getPackageName().set("main");
                        } else {
                            task.getPackageName().set(config.getPackageName());
                        }

                        task.getDestinationDir().set(getOutputDir(project, name));

                        if (config.getLibraryName() == null) {
                            task.getLibraryName().set("gojni");
                        } else {
                            task.getLibraryName().set(config.getLibraryName());
                        }

                        task.getBuildTags().set(config.getBuildTags());
                        task.getSdkVersion().set(config.getSdkVersion());
                        task.getABI().set(abi);
                        task.getDebuggable().set(config.isDebuggable());
                    }
            );

            final String externalNativeBuild = "externalNativeBuild" + capitalize(name);

            project.getTasks().getByName(externalNativeBuild).dependsOn(buildTask);

            project.getTasks().forEach(task -> {
                if (task.getName().startsWith("buildCMake")) {
                    task.mustRunAfter(buildTask);
                }
            });
        }
    }

    @Override
    public void apply(@Nonnull Project target) {
        if (!target.getPlugins().hasPlugin("com.android.base")) {
            throw new GradleException("Android plugin not applied");
        }

        final ProjectExtension global = target.getExtensions().create("golang", ProjectExtension.class);
        final AndroidComponentsExtension<?, ?, ?> components = target.getExtensions().getByType(AndroidComponentsExtension.class);

        components.registerExtension(
                new DslExtension.Builder("golang")
                        .extendBuildTypeWith(VariantExtension.class)
                        .extendProductFlavorWith(VariantExtension.class)
                        .build(),
                (config) -> {
                    final Variant variant = config.getVariant();
                    final ExternalNativeBuild externalNativeBuild = variant.getExternalNativeBuild();

                    if (externalNativeBuild == null) {
                        throw new GradleException("External NDK build is required");
                    }

                    final VariantExtension buildType = config.buildTypeExtension(VariantExtension.class);
                    final List<VariantExtension> productFlavor = config.productFlavorsExtensions(VariantExtension.class);

                    final Set<String> abiFilters = externalNativeBuild.getAbiFilters().get();
                    final String moduleDir = global.getModuleDirectory();
                    final String libraryName = global.getLibraryName();
                    final String packageName = global.getPackageName();
                    final boolean isDebuggable = "debug".equals(variant.getBuildType());
                    final Set<String> buildTags = Stream.concat(
                            buildType.getBuildTags().stream(),
                            productFlavor.stream().flatMap(p -> p.getBuildTags().stream())
                    ).collect(Collectors.toSet());

                    return new BuildConfig(
                            moduleDir,
                            libraryName,
                            packageName, abiFilters,
                            buildTags,
                            config.getVariant().getMinSdkVersion().getApiLevel(),
                            isDebuggable
                    );
                }
        );

        components.finalizeDsl((dsl) -> {
            dsl.getSourceSets().all((sourceSet) ->
                    sourceSet.getJniLibs().srcDir(getOutputDir(target, sourceSet.getName()))
            );
        });

        components.onVariants(components.selector().all(), (variant) -> {
            final BuildConfig config = variant.getExtension(BuildConfig.class);
            if (config == null) {
                return;
            }

            target.afterEvaluate((_project) ->
                    decorateVariant(
                            target,
                            Objects.requireNonNull(variant.getExternalNativeBuild()).getAbiFilters().get(),
                            variant.getName(),
                            config
                    )
            );
        });
    }
}
