FROM amazoncorretto:21

WORKDIR /app

COPY build/install/server ./
RUN touch /.env

EXPOSE 8080:8080

ENTRYPOINT ["./bin/server"]