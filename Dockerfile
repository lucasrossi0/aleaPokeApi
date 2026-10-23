FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system --uid 10001 --no-create-home app
USER app

COPY --from=build /build/target/pokemon-rankings-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
