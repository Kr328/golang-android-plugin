package com.github.kr328.gradle.golang;

import com.android.build.api.variant.VariantExtension;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.Set;

@Data
@AllArgsConstructor
@NonNull
public class BuildConfig implements VariantExtension {
    private final String moduleDirectory;
    private final String libraryName;
    private final String packageName;
    private final Set<String> abiFilters;
    private final Set<String> buildTags;
    private final int sdkVersion;
    private final boolean debuggable;
}
