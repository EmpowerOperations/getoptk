plugins {
    kotlin("jvm") version "1.3.40"
}

group = "com.empowerops"
version = "0.3"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("com.google.guava:guava:19.0")
    implementation("org.antlr:ST4:4.0.8")

    testImplementation("junit:junit:4.11")
    testImplementation("org.assertj:assertj-core:3.5.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.3.40")
}

repositories {
    mavenCentral()
}
