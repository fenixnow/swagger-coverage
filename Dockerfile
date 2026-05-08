FROM gradle:8.14-jdk8 AS builder

WORKDIR /build
COPY . .
RUN gradle :swagger-coverage-commandline:installDist -x test

FROM eclipse-temurin:8-jre

RUN groupadd -r swagger && useradd -r -g swagger swagger

COPY --from=builder /build/swagger-coverage-commandline/build/install/swagger-coverage-commandline /app

RUN chown -R swagger:swagger /app
USER swagger

WORKDIR /data
ENV PATH="/app/bin:$PATH"

ENTRYPOINT ["swagger-coverage-commandline"]
CMD ["--help"]