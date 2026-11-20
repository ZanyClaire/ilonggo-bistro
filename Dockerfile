# Multi-stage production build for Ilonggo Bistro
FROM node:24-alpine AS css-builder
WORKDIR /build
COPY package.json package-lock.json* ./
RUN npm ci || npm install
COPY src/main/resources ./src/main/resources
RUN npx @tailwindcss/cli -i src/main/resources/static/css/input.css -o src/main/resources/static/css/app.css --minify

FROM eclipse-temurin:25-jdk-alpine AS java-builder
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src ./src
COPY --from=css-builder /build/src/main/resources/static/css/app.css ./src/main/resources/static/css/app.css
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app && mkdir -p /app/uploads /app/data && chown -R app:app /app
USER app
COPY --from=java-builder /build/target/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
