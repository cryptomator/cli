FROM alpine:3.12.0

ARG CRYPTOMATOR_CLI_VERSION=0.4.0

RUN adduser -D cryptomator && \
    apk add --no-cache curl openjdk11-jre-headless && \
    curl -sSL https://github.com/cryptomator/cli/releases/download/$CRYPTOMATOR_CLI_VERSION/cryptomator-cli-$CRYPTOMATOR_CLI_VERSION.jar > /usr/bin/cryptomator.jar && \
    apk del curl

USER cryptomator

VOLUME ["/vaults"]

ENTRYPOINT ["java", "-jar", "/usr/bin/cryptomator.jar"]
