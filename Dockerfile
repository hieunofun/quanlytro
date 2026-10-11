FROM node:22-alpine AS web
WORKDIR /web
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /project
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src/ ./src/
COPY --from=web /src/main/resources/static ./src/main/resources/static
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S hostel && adduser -S hostel -G hostel
RUN mkdir -p /app/uploads && chown hostel:hostel /app/uploads
COPY --from=backend /project/target/hostel-management-0.0.1-SNAPSHOT.jar app.jar
USER hostel
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Ho_Chi_Minh"
ENTRYPOINT ["java", "-jar", "app.jar"]
