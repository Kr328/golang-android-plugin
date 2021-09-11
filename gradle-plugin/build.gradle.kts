plugins {
    java
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    compileOnly(deps.android.gradle)
    compileOnly(gradleApi())
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
    plugins {
        create("golang-android") {
            id = "golang-android"
            implementationClass = "com.github.kr328.golang.GolangPlugin"
        }
    }
}