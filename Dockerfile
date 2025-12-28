FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/aegisops-agent-1.0.0.jar app.jar

RUN addgroup -g 1000 aegisops && \
    adduser -D -u 1000 -G aegisops aegisops

USER aegisops

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]