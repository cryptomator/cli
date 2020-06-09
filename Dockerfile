FROM alpine:3.12.0

ARG CRYPTOMATOR_CLI_VERSION=0.4.0

RUN adduser -D cryptomator && \
    apk add --no-cache openjdk11-jre-headless && \
    wget https://github.com/cryptomator/cli/releases/download/$CRYPTOMATOR_CLI_VERSION/cryptomator-cli-$CRYPTOMATOR_CLI_VERSION.jar -O /usr/bin/cryptomator.jar

USER cryptomator

VOLUME ["/vaults"]

ENTRYPOINT ["java", "-jar", "/usr/bin/cryptomator.jar"]
