FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY backend/src/BeeSafeServer.java backend/src/BeeSafeServer.java
RUN mkdir -p backend/bin && javac -encoding UTF-8 -d backend/bin backend/src/BeeSafeServer.java

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/backend/bin /app/backend/bin
COPY web /app/web
RUN mkdir -p /app/datos
ENV BEESAFE_HOST=0.0.0.0
ENV BEESAFE_PORT=8765
EXPOSE 8765
CMD ["java", "-cp", "backend/bin", "BeeSafeServer"]
