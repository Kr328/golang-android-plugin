package com.github.kr328.gradle.golang;

import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class BuildTask extends DefaultTask {
    private static Map<String, String> buildEnvironment(File ndkDirectory, String abi, int sdkVersion) {
        ArrayList<String> toolchainsRoot = new ArrayList<>();

        toolchainsRoot.add(ndkDirectory.getAbsolutePath());
        toolchainsRoot.add("toolchains");
        toolchainsRoot.add("llvm");
        toolchainsRoot.add("prebuilt");

        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            toolchainsRoot.add("windows-x86_64");
        } else if (Os.isFamily(Os.FAMILY_MAC)) {
            toolchainsRoot.add("darwin-x86_64");
        } else if (Os.isFamily(Os.FAMILY_UNIX)) {
            toolchainsRoot.add("linux-x86_64");
        } else {
            throw new IllegalArgumentException("Unsupported platform: " + System.getProperty("os.name"));
        }

        toolchainsRoot.add("bin");

        final String compilerPrefix;
        final String goArch;
        final String goArm;
        switch (abi) {
            case "arm64-v8a":
                compilerPrefix = "aarch64-linux-android";
                goArch = "arm64";
                goArm = "";
                break;
            case "armeabi-v7a":
                compilerPrefix = "armv7a-linux-androideabi";
                goArch = "arm";
                goArm = "7";
                break;
            case "x86":
                compilerPrefix = "i686-linux-android";
                goArch = "386";
                goArm = "";
                break;
            case "x86_64":
                compilerPrefix = "x86_64-linux-android";
                goArch = "amd64";
                goArm = "";
                break;
            default:
                throw new IllegalArgumentException("Unsupported abi: " + abi);
        }

        toolchainsRoot.add(compilerPrefix + sdkVersion + "-clang");

        final HashMap<String, String> environment = new HashMap<>();

        environment.put("CC", String.join(File.separator, toolchainsRoot));
        environment.put("GOOS", "android");
        environment.put("GOARCH", goArch);
        environment.put("GOARM", goArm);
        environment.put("CGO_ENABLED", "1");
        environment.put("CFLAGS", "-O3 -Werror");

        return environment;
    }

    @TaskAction
    public void build() throws IOException {
        Files.createDirectories(Paths.get(getModuleDir().getAsFile().get().getParentFile().getAbsolutePath()));

        getProject().exec((spec) -> {
            final File ndkDirectory = getNdkDirectory().get();
            final File moduleDir = getModuleDir().getAsFile().get();
            final String packageName = getPackageName().get();
            final File destinationDir = getDestinationDir().getAsFile().get();
            final String libraryName = getLibraryName().get();
            final Set<String> buildTags = getBuildTags().get();
            final String abi = getABI().get();
            final int sdkVersion = getSdkVersion().get();
            final boolean isDebuggable = getDebuggable().get();

            ArrayList<String> commands = new ArrayList<>();

            commands.add("go");
            commands.add("build");
            commands.add("-buildmode");
            commands.add("c-shared");
            commands.add("-trimpath");
            commands.add("-o");
            commands.add(Paths.get(destinationDir.getAbsolutePath(), abi, "lib" + libraryName + ".so").toAbsolutePath().toString());

            if (isDebuggable) {
                commands.add("-tags");
                commands.add("debug");
            } else {
                commands.add("-ldflags");
                commands.add("-s -w");
            }

            if (!buildTags.isEmpty()) {
                commands.add("-tags");
                commands.add(String.join(",", buildTags));
            }

            commands.add(packageName);

            spec.commandLine(commands);
            spec.workingDir(moduleDir);
            spec.environment(buildEnvironment(ndkDirectory, abi, sdkVersion));
        });
    }

    @Input
    public abstract Property<File> getNdkDirectory();

    @InputDirectory
    public abstract DirectoryProperty getModuleDir();

    @Input
    public abstract Property<String> getPackageName();

    @OutputDirectory
    public abstract DirectoryProperty getDestinationDir();

    @Input
    public abstract Property<String> getLibraryName();

    @Input
    public abstract SetProperty<String> getBuildTags();

    @Input
    public abstract Property<Integer> getSdkVersion();

    @Input
    public abstract Property<String> getABI();

    @Input
    public abstract Property<Boolean> getDebuggable();
}
