# ---- Stage 1: build the jar ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Copy only what Maven needs to download dependencies first, so this layer is cached
# until the pom changes.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src src
# Tests and the coverage gate run in "./mvnw verify" (see README); the image build only packages.
RUN ./mvnw -B -q package -DskipTests

# ---- Stage 2: small runtime image ----
FROM eclipse-temurin:25-jre
WORKDIR /app

# Do not run as root
RUN useradd --system --uid 10001 --no-create-home app
USER app

COPY --from=build /build/target/pokemon-rankings-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
