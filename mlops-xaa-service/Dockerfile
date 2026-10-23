FROM eclipse-temurin:17-jdk-slim
WORKDIR /app
COPY target/mlops-xaa-service-1.3.0.jar app.jar
EXPOSE 8186
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Duser.timezone=Asia/Shanghai -jar app.jar"]
