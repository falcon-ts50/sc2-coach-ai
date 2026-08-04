FROM node:24-alpine AS frontend-build
WORKDIR /src/frontend
COPY frontend/package.json ./
RUN npm install
COPY frontend ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-25 AS java-build
WORKDIR /src
COPY java ./java
COPY --from=frontend-build /src/frontend/dist ./java/portal/src/main/resources/static
RUN cd java && mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-pip \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt analyze.py ./
COPY --from=java-build /src/java/portal/target/portal-*.jar ./portal.jar
RUN pip3 install --break-system-packages --no-cache-dir -r requirements.txt

ENV SC2_COACH_PYTHON=python3
ENV SC2_COACH_DECODER_SCRIPT=/app/analyze.py
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/portal.jar"]
