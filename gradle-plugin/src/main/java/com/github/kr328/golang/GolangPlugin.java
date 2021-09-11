package com.github.kr328.golang;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.api.BaseVariant;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GolangPlugin implements Plugin<Project> {
    public static String taskNameOf(BaseVariant variant, String abi) {
        return String.format("externalGolangBuild%s%s", capitalize(variant.getName()), capitalize(abi));
    }

    private static String capitalize(String str) {
        return Arrays.stream(str.split("[-_]"))
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining());
    }

    public static File outputDirOf(Project project, BaseVariant variant, String abi) {
        String variantName = null;
        if (variant != null) {
            variantName = variant.getName();
        }

        return new File(Stream.of(project.getBuildDir().getAbsolutePath(), "outputs", "golang", variantName, abi)
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(File.separator)));
    }

    @Override
    public void apply(@Nonnull Project target) {
        target.getExtensions().create("golang", GolangExtension.class);

        target.afterEvaluate((project) -> {
            BaseExtension base = target.getExtensions().getByType(BaseExtension.class);

            if (base instanceof AppExtension) {
                ((AppExtension) base).getApplicationVariants()
                        .forEach(v -> decorateVariant(target, base, v));
            } else if (base instanceof LibraryExtension) {
                ((LibraryExtension) base).getLibraryVariants()
                        .forEach(v -> decorateVariant(target, base, v));
            } else {
                throw new IllegalArgumentException("Unsupported android plugin: " + base);
            }
        });
    }

    private void decorateVariant(Project target, BaseExtension base, BaseVariant variant) {
        GolangExtension extension = target.getExtensions().getByType(GolangExtension.class);
        HashSet<String> abis = new HashSet<>();

        abis.addAll(base.getDefaultConfig().getExternalNativeBuild().getCmake().getAbiFilters());
        abis.addAll(base.getDefaultConfig().getExternalNativeBuild().getNdkBuild().getAbiFilters());

        abis.forEach(abi -> {
            GolangSourceSet sourceSet = extension.getSourceSets().findByName(variant.getName());
            if (sourceSet == null) {
                sourceSet = extension.getSourceSets().findByName(variant.getFlavorName());
                if (sourceSet == null) {
                    sourceSet = extension.getSourceSets().findByName("main");
                }
            }
            if (sourceSet == null) {
                return;
            }

            File output = outputDirOf(target, variant, abi);
            File source = sourceSet.getSrcDir().get().getAsFile();
            List<String> tags = sourceSet.getTags().getOrElse(Collections.emptyList());
            String moduleFile = sourceSet.getModuleFile().getOrElse("");

            GolangBuildTask task = target.getTasks().create(taskNameOf(variant, abi), GolangBuildTask.class)
                    .applyFor(base, variant, abi, source, output, sourceSet.getFileName().get(), tags, moduleFile);

            variant.getPreBuildProvider().get().dependsOn(task);

            //noinspection ResultOfMethodCallIgnored
            output.mkdirs();
        });

        base.getSourceSets().getByName(variant.getName()).getJniLibs().srcDir(outputDirOf(target, variant, null));
    }
}
