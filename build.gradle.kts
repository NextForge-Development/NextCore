plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
    id("com.gradleup.shadow") version "8.3.8"
}

group = "dev.mzcy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    paperweight.paperDevBundle("1.19.1-R0.1-SNAPSHOT")
    implementation("gg.nextforge:nextlicenses-client:1.1-SNAPSHOT")

    // JDBC Backends
    implementation("mysql:mysql-connector-j:8.4.0")
    implementation("com.h2database:h2:2.2.224")

    // MongoDB
    implementation("org.mongodb:mongodb-driver-sync:5.1.0")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}