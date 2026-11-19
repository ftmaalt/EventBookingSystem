# Jenkins CI/CD

How the BookNGo pipeline in [`Jenkinsfile`](../Jenkinsfile) works and how to run it on a local Jenkins in Docker.

Related: [docs/deployment.md](../docs/deployment.md)

## 1. Overview

The pipeline builds and tests all five services, then builds one Docker image per service and pushes it to Docker Hub.

Stages, in order:

| # | Stage | What it does |
|---|---|---|
| 1 | Checkout | `checkout scm`, prints the last commit |
| 2 | Build | `mvn -B package -DskipTests` for all modules |
| 3 | Test | Starts throwaway Postgres and Kafka containers, runs `mvn -B test`, publishes JUnit reports |
| 4 | Docker Build | Builds `bookngo-<service>:1.0.<BUILD_NUMBER>` from the root `Dockerfile` with `--build-arg SERVICE` and `--build-arg PORT` |
| 5 | Docker Push | Logs in with the `dockerhub` credential, tags and pushes each image |
| - | Deploy to EC2 | Written but commented out (see section 7) |

Services and ports (`SERVICES` in the Jenkinsfile): `auth-service:8081`, `catalog-service:8082`, `booking-service:8083`, `notification-service:8084`, `compliance-service:8085`.

Images are pushed as `<DOCKERHUB_USER>/bookngo-<service>`, where `<DOCKERHUB_USER>` is the username stored in the `dockerhub` credential. With the project account that is `ftmam/bookngo-<service>`, which is what `docker-compose.prod.yml` pulls.

Tags: `1.0.<BUILD_NUMBER>` (`IMAGE_VERSION`) and `latest`.

Trigger: `pollSCM('H/5 * * * *')` in the Jenkinsfile (about every 5 minutes); the job tracks branch `*/main`.

Other options set in the Jenkinsfile: no concurrent builds, 60-minute timeout, last 15 builds kept. After every run, the `post` block removes the test containers and network.

## 2. Jenkins image

[`jenkins/Dockerfile`](Dockerfile) starts from `jenkins/jenkins:lts-jdk17` and, as `root`, adds Docker's official Debian apt repository and installs:

- `ca-certificates`, `curl`
- `docker-ce-cli` (Docker client only, no daemon)
- `docker-buildx-plugin`

It then switches back to the `jenkins` user. The Docker daemon is the host's, reached through the mounted socket.

Image name used in this guide: `bookngo-jenkins:lts-jdk17`.

## 3. Run Jenkins locally with Docker

The Test, Docker Build and Docker Push stages call `docker`, so the container needs the host Docker socket. The `jenkins` user also needs permission to use it. First find the group ID that owns the socket:

```bash
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock alpine stat -c %g /var/run/docker.sock
```

Use that number as `<SOCKET_GID>` below. On Docker Desktop (Windows and macOS) it is usually `0`. In Git Bash on Windows, run `export MSYS_NO_PATHCONV=1` first so `/var/run/docker.sock` is not rewritten into a Windows path.

PowerShell (from the repository root):

```powershell
docker build -t bookngo-jenkins:lts-jdk17 ./jenkins
```

```powershell
docker run -d --name jenkins-local --restart unless-stopped -p 8088:8080 -v jenkinsvol-bookngo:/var/jenkins_home -v /var/run/docker.sock:/var/run/docker.sock --group-add <SOCKET_GID> bookngo-jenkins:lts-jdk17
```

bash (from the repository root):

```bash
docker build -t bookngo-jenkins:lts-jdk17 ./jenkins

docker run -d --name jenkins-local --restart unless-stopped \
  -p 8088:8080 \
  -v jenkinsvol-bookngo:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --group-add "$(docker run --rm -v /var/run/docker.sock:/var/run/docker.sock alpine stat -c %g /var/run/docker.sock)" \
  bookngo-jenkins:lts-jdk17
```

Do not pass `--hostname`: the Test stage runs `docker network connect "$CI_NETWORK" "$(hostname)"`, which only works while the hostname is the container ID (the default).

Open `http://localhost:8088`. The initial admin password is printed by:

```bash
docker exec jenkins-local cat /var/jenkins_home/secrets/initialAdminPassword
```

Paste it into the unlock page, then create your own admin user. All Jenkins data (jobs, credentials, build history) is kept in the `jenkinsvol-bookngo` volume, so the container can be removed and recreated without losing setup.

## 4. One-time Jenkins setup

### Plugins

Choose "Install suggested plugins" in the setup wizard. Make sure these are present, because the Jenkinsfile uses them:

| Plugin | Used for |
|---|---|
| Pipeline | `pipeline { }`, `timeout`, `disableConcurrentBuilds` |
| Git | `checkout scm` and SCM polling |
| Credentials Binding | `withCredentials(...)` |
| JUnit | `junit` test report step |
| SSH Agent | `sshagent(...)`, only for the Deploy to EC2 stage |
| Blue Ocean (optional) | Referenced in the failure message only |

### Tools

The Jenkinsfile asks for two tools by name. Configure them under **Manage Jenkins → Tools**:

- **JDK**, name `jdk17`. The base image already contains JDK 17 (`JAVA_HOME` `/opt/java/openjdk`), or use an automatic installer.
- **Maven**, name `Maven-3.9`. Use "Install automatically" with a 3.9.x version.

The names must match exactly.

### Pipeline job

1. **New Item** → name it (for example `bookngo`) → **Pipeline**.
2. **Pipeline** section: Definition **Pipeline script from SCM**, SCM **Git**, Repository URL = the GitHub repository URL. A public repository needs no credentials.
3. Branch Specifier: `*/main`. Script Path: `Jenkinsfile`.
4. **Build Triggers**: **Poll SCM**, schedule `H/5 * * * *`.
5. Save, then click **Build Now** once. Jenkins registers the `pollSCM` trigger from the Jenkinsfile on the first run.

## 5. Credentials

Add credentials under **Manage Jenkins → Credentials → System → Global credentials**. The IDs must match the Jenkinsfile.

| ID | Kind | Content | Used by |
|---|---|---|---|
| `dockerhub` | Username with password | Docker Hub username, and a Docker Hub **access token** as the password (Docker Hub → Account settings → Personal access tokens, Read & Write). Never the account password. | Docker Push |
| `ec2-ssh-key` | SSH Username with private key | Username `ec2-user` and the EC2 key pair's private key | Deploy to EC2 |
| `ec2-host` | Secret text | Public hostname or IP of the EC2 instance | Deploy to EC2 |

Credentials live only in Jenkins (inside the `jenkinsvol-bookngo` volume). They are never committed to the repository or written into the Jenkinsfile. The pipeline sends the token to `docker login` with `--password-stdin` and logs out after the push.

## 6. Test stage

The Test stage creates, per build:

- a Docker network `bookngo-ci-<BUILD_NUMBER>`
- `postgres:16.4` as `bookngo-ci-postgres-<BUILD_NUMBER>` (database `bookngo`)
- `apache/kafka:3.9.1` in KRaft mode as `bookngo-ci-kafka-<BUILD_NUMBER>`, listening on `9092`

It connects the Jenkins container to that network, waits up to about 60 seconds for each container (`pg_isready`, `kafka-broker-api-versions.sh`), then runs `mvn -B test`. The services reach the containers by name through the `DB_URL` and `KAFKA_BOOTSTRAP_SERVERS` values set in the Jenkinsfile `environment` block. Those values, including the database login and JWT secret, are throwaway values for CI only.

For this to work:

- Jenkins must run in a Docker container (as in section 3), and the build must run on that controller, because the stage connects its own container (`$(hostname)`) to the test network.
- The Docker socket must be mounted, and the `jenkins` user must be able to use it.
- The Docker host must be able to pull `postgres:16.4` and `apache/kafka:3.9.1`.
- No host ports are published, so the stage does not conflict with a local `docker compose` stack.

Test results are published with `junit` even when the stage fails. The containers and network are removed in the pipeline's `post { always { } }` block.

## 7. Deploy to EC2 stage

The stage is in the Jenkinsfile but commented out. To enable it:

1. Install the **SSH Agent** plugin.
2. Add the `ec2-ssh-key` and `ec2-host` credentials (section 5).
3. Prepare the server as in [docs/deployment.md](../docs/deployment.md) section 5. It needs Docker and the Compose plugin, `/opt/bookngo/docker-compose.yml` (a copy of `docker-compose.prod.yml`, which uses `ftmam/bookngo-<service>:${IMAGE_TAG}`), and its own `/opt/bookngo/.env`, never taken from Git.
4. Uncomment the `stage('Deploy to EC2') { ... }` block, commit, and push to `main`.

On the server, the stage runs over SSH as `ec2-user`:

```bash
cd /opt/bookngo && IMAGE_TAG=$IMAGE_VERSION docker compose pull && IMAGE_TAG=$IMAGE_VERSION docker compose up -d
```

`$IMAGE_VERSION` is the build's `1.0.<BUILD_NUMBER>`. The first connection accepts the host key automatically (`StrictHostKeyChecking=accept-new`).

## 8. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `permission denied while trying to connect to the Docker daemon socket` | The socket is not mounted, or the `jenkins` user is not in the socket's group | Recreate the container with `-v /var/run/docker.sock:/var/run/docker.sock` and `--group-add <SOCKET_GID>` (section 3). The volume keeps all Jenkins data. |
| `docker: not found` | Jenkins is running from the plain `jenkins/jenkins` image | Build and run `bookngo-jenkins:lts-jdk17` from `jenkins/Dockerfile` |
| Docker Push: `unauthorized: incorrect username or password` | Wrong `dockerhub` credential, account password used, or token expired or read-only | Create a new Docker Hub access token with Read & Write scope and update the `dockerhub` credential |
| `docker network connect ... No such container` | Jenkins is not in a container, or `--hostname` was overridden | Run Jenkins as in section 3 without `--hostname` |
| `Postgres did not start` / `Kafka did not start`, or tests fail with connection refused / `UnknownHostException` | Images could not be pulled, a container crashed, or the Jenkins container is not on the test network | Check the printed `docker logs` in the console. Run `docker ps -a` on the host. Remove stale `bookngo-ci-*` containers and networks left by a killed build (`docker rm -f`, `docker network rm`). |
| `Tool type "maven" does not have an install of "Maven-3.9" configured` | Tool names do not match | Configure the JDK `jdk17` and Maven `Maven-3.9` tools (section 4) |
| `no space left on device` | Every build leaves new `bookngo-<service>:1.0.<n>` images, tagged copies and build cache on the host | `docker image prune -a` and `docker builder prune` on the host. Check usage with `docker system df`. |

## 9. GitHub Actions

[`.github/workflows/ci.yml`](../.github/workflows/ci.yml) is an optional GitHub Actions equivalent, not required for Jenkins. On pushes and pull requests to `main` it runs `mvn -B verify` against a Postgres 16.4 service container. On pushes to `main` it then builds and pushes the same five images with tags `1.0.<run_number>` and `latest`. It uses repository secrets `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN`. Unlike the Jenkins Test stage, it does not start a Kafka container.
