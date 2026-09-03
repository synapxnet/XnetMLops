FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-pip python3-venv \
    && python3 -m venv /opt/recommendation-venv \
    && /opt/recommendation-venv/bin/pip install --no-cache-dir --upgrade pip \
    && /opt/recommendation-venv/bin/pip install --no-cache-dir numpy==2.2.3 pandas==2.2.3 \
    && /opt/recommendation-venv/bin/pip install --no-cache-dir torch==2.11.0 --index-url https://download.pytorch.org/whl/cpu \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY target/mlops-mtp-service-1.0.0-SNAPSHOT.jar app.jar
COPY recommendation-training /opt/xnetmlops/recommendation-training
EXPOSE 8183
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV RECOMMENDATION_TRAINING_PYTHON=/opt/recommendation-venv/bin/python
ENV RECOMMENDATION_TRAINING_SCRIPT=/opt/xnetmlops/recommendation-training/train_dcn.py
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Duser.timezone=Asia/Shanghai -jar app.jar"]
