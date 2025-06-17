FROM mcr.microsoft.com/devcontainers/java:1-21-bullseye

# [Optional] Uncomment if you want to install more tools
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends maven

# [Optional] Uncomment if you want to install yc cli
RUN curl -fsSL https://storage.yandexcloud.net/yandexcloud-yc/install.sh | bash

# [Optional] Uncomment if you want to install ydb cli
RUN curl -fsSL https://raw.githubusercontent.com/ydb-platform/ydb/refs/heads/main/ydb/apps/ydb/install/install.sh | bash
