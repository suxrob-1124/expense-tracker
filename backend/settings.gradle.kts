pluginManagement {
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "org.springframework.boot" ->
                    useModule("org.springframework.boot:spring-boot-gradle-plugin:${requested.version}")
                "io.spring.dependency-management" ->
                    useModule("io.spring.gradle:dependency-management-plugin:${requested.version}")
            }
        }
    }
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "expense-tracker-backend"
