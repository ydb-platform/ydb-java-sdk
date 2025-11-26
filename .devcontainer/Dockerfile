FROM mcr.microsoft.com/devcontainers/java:1-21-bullseye

ENV MAVEN_CONFIG=/var/maven/.m2

# [Optional] Uncomment if you want to install more tools
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends maven \
    && mkdir -p /var/maven/.m2 \
    && chown -R vscode:vscode /var/maven/.m2
