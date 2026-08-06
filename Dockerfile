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
ARG APP_VERSION=0.8.8-SNAPSHOT
ARG BUILD_NUMBER=local
ARG BUILD_TIME=unknown
ARG GIT_COMMIT=unknown
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-pip \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --home-dir /nonexistent --shell /usr/sbin/nologin sc2coach

COPY requirements.txt ./*.py ./
COPY --from=java-build /src/java/portal/target/portal-*.jar ./portal.jar
RUN pip3 install --break-system-packages --no-cache-dir -r requirements.txt
RUN python3 -c "import analyze"

ENV SC2_COACH_PYTHON=python3
ENV SC2_COACH_DECODER_SCRIPT=/app/analyze.py
ENV APP_VERSION=${APP_VERSION}
ENV BUILD_NUMBER=${BUILD_NUMBER}
ENV BUILD_TIME=${BUILD_TIME}
ENV GIT_COMMIT=${GIT_COMMIT}
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70"
ENV HOME=/tmp
ENV MPLCONFIGDIR=/tmp/matplotlib
EXPOSE 8080
USER sc2coach
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/portal.jar"]
