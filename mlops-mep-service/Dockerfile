FROM eclipse-temurin:17-jdk-slim
WORKDIR /app
COPY target/mlops-mep-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8184
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Duser.timezone=Asia/Shanghai -jar app.jar"]
