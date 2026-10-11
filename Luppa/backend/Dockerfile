FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN addgroup --system luppa && adduser --system --ingroup luppa luppa

ENV SPRING_PROFILES_ACTIVE=prod
ENV IMAGE_STORAGE_DIR=/app/uploads/events
ENV PROFILE_IMAGE_STORAGE_DIR=/app/uploads/profile

COPY --from=build /workspace/target/luppa-api-*.jar /app/luppa-api.jar

RUN mkdir -p /app/uploads/events /app/uploads/profile \
    && chown -R luppa:luppa /app

USER luppa

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/luppa-api.jar"]
