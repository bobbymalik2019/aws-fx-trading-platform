#!/bin/bash
set -euo pipefail

sudo dnf install -y \
  java-17-amazon-corretto-devel \
  maven \
  git \
  docker

sudo systemctl enable --now docker

java -version
mvn -version
docker --version