plugins {
    `java-library`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.slf4j:slf4j-api:2.0.9")
    compileOnly("it.unimi.dsi:fastutil:8.5.12")
    compileOnly("com.google.code.gson:gson:2.10.1")

    testImplementation("org.slf4j:slf4j-api:2.0.9")
    testImplementation("it.unimi.dsi:fastutil:8.5.12")
    testImplementation("com.google.code.gson:gson:2.10.1")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
