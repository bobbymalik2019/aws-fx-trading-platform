aws-fx-trading-platform/
│
├── README.md                    # Overall project architecture
│
├── fix-accepter-server/
│   ├── README.md                # Docker deployment guide
│   ├── Dockerfile
│   ├── pom.xml
│   ├── src/
│   └── .dockerignore
│
└── fix-initiator-client/
    ├── README.md                # Market Connectivity guide
    ├── pom.xml
    └── src/

Why this structure?
- Root README.md: Explains our overall AWS FX Trading Platform architecture, components, technologies, and deployment strategy.
- Acceptor README.md: Explains how to build, run, test, and troubleshoot the simulated FIX Liquidity Providers using Docker.
- Initiator README.md: Explains how to configure and deploy the Market Connectivity component.
GitHub automatically displays the README.md when you open the corresponding directory.    

# FIX LP Simulator — Docker Deployment Guide

## Overview

This guide explains how to build and deploy the FIX Liquidity Provider Simulator using Docker.

The application is part of the AWS FX Trading Platform project. It uses Java, Spring Boot, and QuickFIX/J to simulate liquidity providers such as HSBC and Bloomberg.

The deployment works on:

- Windows with Docker Desktop (Linux containers)
- Linux with Docker Engine
- Amazon Linux 2023 running on AWS EC2

## 1. Prerequisites

Ensure the following tools are installed:

| Software | Requirement |
|---|---|
| Docker | Installed and running |
| Git | Installed |
| Java | JDK 17, for local JAR builds |
| Apache Maven | For local JAR builds |

Java and Maven are not needed on the host if you already have the required application JAR or use a multi-stage Dockerfile that builds it.

Verify Docker:

```bash
docker --version
docker info
```

Verify Java and Maven if building the JAR locally:

```bash
java -version
mvn --version
```

On Linux, you may need to prefix Docker commands with `sudo` if your user does not have permission to access the Docker daemon.

## 2. Clone the repository

```bash
git clone https://github.com/bobbymalik2019/aws-fx-trading-platform.git
```

Navigate to the project:

```bash
cd aws-fx-trading-platform/fix-accepter-server
```

If the repository is already cloned:

```bash
git pull origin main
```

## 3. Build the Spring Boot application

From the `fix-accepter-server` directory:

```bash
mvn clean package -DskipTests
```

For systems with limited memory, optionally set Maven memory limits.

Linux:

```bash
MAVEN_OPTS="-Xmx384m" mvn clean package -DskipTests
```

Windows PowerShell:

```powershell
$env:MAVEN_OPTS="-Xmx384m"
mvn clean package -DskipTests
```

After a successful build, verify that the JAR exists:

```text
target/fix-accepter-server-0.0.1-SNAPSHOT.jar
```

The expected JAR name reflects the current project version and may change.

## 4. Build the Docker image

Ensure the project directory contains the application's `Dockerfile`.

Run:

```bash
docker build -t fx-lp-simulator:1.0 .
```

This builds a Docker image named `fx-lp-simulator` with tag `1.0`.

Verify:

```bash
docker images
```

**Note:** These instructions assume the Dockerfile packages or copies the Spring Boot application into a Linux-based container and that the application can load its FIX configuration files.

## 5. Create Docker volumes

Create persistent volumes for QuickFIX/J message storage and logs.

```bash
docker volume create fx-lp-store
docker volume create fx-lp-log
```

The volumes are mounted to:

| Volume | Container directory | Purpose |
|---|---|---|
| fx-lp-store | /app/store | FIX session message store |
| fx-lp-log | /app/log | FIX session logs |

These paths must match the QuickFIX/J configuration in the container.

Docker named volumes persist even when the container is removed.

## 6. Create the Docker network

```bash
docker network create fx-exchange-1
```

The network allows the FIX Acceptor and other Docker-based components to communicate using container names.

For example, another container attached to this network can connect to the FIX Acceptor using:

```text
Host: fx-lp-simulator
Port: 9876
```

Docker requires the network to exist before containers can be attached to it.

If the network has already been created, reuse it.

## 7. Start the FIX LP Simulator

Run the following command from PowerShell, Linux Bash, or another terminal supporting Docker CLI:

```bash
docker run -d --name fx-lp-simulator --network fx-exchange-1 -p 9876:9876 -v fx-lp-store:/app/store -v fx-lp-log:/app/log --memory=512m fx-lp-simulator:1.0
```

Parameters explained:

| Parameter           | Description                                |
|        ---          |                                         ---|
| -d                  | Runs the container in the background       |
| --name              | Assigns the container name                 |
| --network           | Attaches to the Docker network             |
| -p 9876:9876        | Maps host port 9876 to container port 9876 |
| -v                  | Mounts persistent volumes                  |
| --memory=512m       | Sets the container memory limit            |
| fx-lp-simulator:1.0 | Docker image and version                   |

The FIX Acceptor is configured to listen on TCP port `9876`, which can serve multiple configured FIX sessions.

If running on EC2, ensure the instance's security group restricts inbound access to TCP port 9876 to authorized FIX clients only.

## 8. Verify the deployment

Check whether the container is running:

```bash
docker ps --filter name=fx-lp-simulator
```

View the latest application logs:

```bash
docker logs --tail 50 fx-lp-simulator
```

Monitor logs continuously:

```bash
docker logs -f fx-lp-simulator
```

Inspect the Docker network:

```bash
docker network inspect fx-exchange-1
```

The application logs should show the FIX Acceptor starting and configured sessions being created. FIX logon events occur when an initiator connects.

## 9. Connect the FIX Initiator

Configure the Market Connectivity application's QuickFIX/J initiator settings to connect to the Acceptor.

| Deployment scenario | SocketConnectHost | SocketConnectPort |
|                  ---|                ---|                 ---|
| Initiator on the same host, outside Docker | 127.0.0.1 | 9876 |
| Initiator in Docker on `fx-exchange-1` | fx-lp-simulator | 9876 |
| Initiator on another machine | Reachable host IP/DNS | 9876 |

The initiator and acceptor must also have matching FIX session configuration, including SenderCompID and TargetCompID.

The simulator supports the configured HSBC and Bloomberg FIX sessions. Additional liquidity providers require corresponding session configuration.

## 10. Stop and restart the container

Stop:

```bash
docker stop fx-lp-simulator
```

Start:

```bash
docker start fx-lp-simulator
```

Restart:

```bash
docker restart fx-lp-simulator
```

## 11. Remove the container

First stop the container:

```bash
docker stop fx-lp-simulator
```

Then remove it:

```bash
docker rm fx-lp-simulator
```

This does not delete the Docker image or named volumes.

To remove the image:

```bash
docker rmi fx-lp-simulator:1.0
```

## 12. Optional cleanup of Docker resources

Remove the Docker network after all connected containers have been removed or disconnected:

```bash
docker network rm fx-exchange-1
```

Remove named volumes only if their persisted FIX session state and logs are no longer needed:

```bash
docker volume rm fx-lp-store
docker volume rm fx-lp-log
```

**Warning:** Removing these volumes permanently deletes their stored data.

## 13. Rebuild after changing application code

After pulling updated source code:

```bash
git pull origin main
mvn clean package -DskipTests
docker build -t fx-lp-simulator:1.0 .
docker stop fx-lp-simulator
docker rm fx-lp-simulator
```

Then repeat the `docker run` command in Section 7.

The existing named volumes will be reused.

## Troubleshooting

**Container exits immediately:** Check `docker logs fx-lp-simulator`.

**Port 9876 is already allocated:** Stop the conflicting application or use a different host port mapping.

**Docker permission denied on Linux:** Verify permissions to access the Docker daemon.

**Initiator cannot connect:** Check the configured FIX host, port, session identifiers, Docker network and host firewall.

**FIX session errors after restarting:** Inspect the QuickFIX/J session logs and stored session state before resetting any FIX sequence numbers.

---

**Project:** AWS FX Trading Platform

**Repository:** https://github.com/bobbymalik2019/aws-fx-trading-platform

**Component:** FIX Liquidity Provider Simulator / QuickFIX/J Acceptor




--------Incase you have a bare Linux machine for some other purpose you can re-purpose it for this deme.run
setup-linux.sh

-----then run 
finally-run.sh
