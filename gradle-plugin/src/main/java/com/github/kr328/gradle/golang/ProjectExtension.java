package com.github.kr328.gradle.golang;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ProjectExtension extends VariantExtension {
    private String moduleDirectory;
    private String libraryName;
    private String packageName;
}
