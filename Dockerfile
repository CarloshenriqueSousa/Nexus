# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY src/ ./src/
COPY lib/ ./lib/

# Generate sources list and compile
RUN find src -name "*.java" > sources.txt
RUN mkdir bin
RUN javac -encoding UTF-8 -d bin --module-path lib --class-path lib/postgresql-42.7.3.jar @sources.txt

# Stage 2: Run
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /app/bin ./bin
COPY --from=builder /app/lib ./lib
COPY src/config/security.properties ./src/config/security.properties

# Expose server port
EXPOSE 8081

# Execute the Java Server
CMD ["java", "--module-path", "lib", "--class-path", "bin:lib/postgresql-42.7.3.jar", "Main.Main"]
