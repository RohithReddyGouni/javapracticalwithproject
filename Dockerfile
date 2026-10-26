From eclipse-temurin:17-jdk-jammy AS build

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

COPY src/ src/

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

RUN groupadd --gid 10001 app \
    && useradd --uid 10001 \
       --gid app \
       --no-create-home \
       --home-dir /app \
       --shell /usr/sbin/nologin app

COPY --from=build --chown=app:app \
    /build/target/*.jar /app/app.jar

USER app

EXPOSE 8080 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
