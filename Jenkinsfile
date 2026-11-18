// BookNGo CI/CD pipeline for Jenkins (mirrors .github/workflows/ci.yml).

pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'jdk17'
    }

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    triggers {

        pollSCM('H/5 * * * *')
    }

    environment {
        IMAGE_VERSION = "1.0.${BUILD_NUMBER}"
        // passed to the root Dockerfile as SERVICE / PORT build args
        SERVICES      = 'auth-service:8081 catalog-service:8082 booking-service:8083 notification-service:8084 compliance-service:8085'

        // throw-away containers for the tests, on their own Docker network
        CI_NETWORK    = "bookngo-ci-${BUILD_NUMBER}"
        CI_POSTGRES   = "bookngo-ci-postgres-${BUILD_NUMBER}"
        CI_KAFKA      = "bookngo-ci-kafka-${BUILD_NUMBER}"



        DB_URL                  = "jdbc:postgresql://bookngo-ci-postgres-${BUILD_NUMBER}:5432/bookngo"
        DB_USERNAME             = 'postgres'
        DB_PASSWORD             = 'postgres'
        JWT_SECRET              = 'dGVzdC1qd3Qtc2VjcmV0LWZvci1jaS1vbmx5LTAxMjM0NTY3ODk='
        INTERNAL_API_KEY        = 'ci-internal-key'
        KAFKA_BOOTSTRAP_SERVERS = "bookngo-ci-kafka-${BUILD_NUMBER}:9092"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
            }
        }

        stage('Build') {
            steps {
                sh 'java -version && mvn -v'
                sh 'mvn -B package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh '''
                    docker network create "$CI_NETWORK"
                    docker network connect "$CI_NETWORK" "$(hostname)"

                    docker run -d --name "$CI_POSTGRES" --network "$CI_NETWORK" \
                        -e POSTGRES_DB=bookngo -e POSTGRES_USER="$DB_USERNAME" -e POSTGRES_PASSWORD="$DB_PASSWORD" \
                        postgres:16.4

                    docker run -d --name "$CI_KAFKA" --network "$CI_NETWORK" \
                        -e KAFKA_NODE_ID=1 \
                        -e KAFKA_PROCESS_ROLES=broker,controller \
                        -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
                        -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://$CI_KAFKA:9092 \
                        -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
                        -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
                        -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
                        -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
                        -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
                        -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
                        -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 \
                        apache/kafka:3.9.1

                    echo "Waiting for Postgres..."
                    for i in $(seq 1 30); do
                        docker exec "$CI_POSTGRES" pg_isready -U "$DB_USERNAME" -d bookngo && break
                        [ "$i" = 30 ] && { echo "Postgres did not start"; docker logs "$CI_POSTGRES"; exit 1; }
                        sleep 2
                    done

                    echo "Waiting for Kafka..."
                    for i in $(seq 1 30); do
                        docker exec "$CI_KAFKA" /opt/kafka/bin/kafka-broker-api-versions.sh \
                            --bootstrap-server localhost:9092 > /dev/null 2>&1 && break
                        [ "$i" = 30 ] && { echo "Kafka did not start"; docker logs "$CI_KAFKA"; exit 1; }
                        sleep 2
                    done
                '''
                sh 'mvn -B test'
            }
            post {
                always {
                    junit allowEmptyResults: false, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    for entry in $SERVICES; do
                        service="${entry%%:*}"
                        port="${entry##*:}"
                        echo "=== Building bookngo-$service:$IMAGE_VERSION (port $port)"
                        docker build \
                            --build-arg SERVICE="$service" \
                            --build-arg PORT="$port" \
                            -t "bookngo-$service:$IMAGE_VERSION" \
                            .
                    done
                '''
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub',
                                                  usernameVariable: 'DOCKERHUB_USER',
                                                  passwordVariable: 'DOCKERHUB_TOKEN')]) {
                    sh '''
                        echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USER" --password-stdin

                        for entry in $SERVICES; do
                            service="${entry%%:*}"
                            repo="$DOCKERHUB_USER/bookngo-$service"
                            docker tag "bookngo-$service:$IMAGE_VERSION" "$repo:$IMAGE_VERSION"
                            docker tag "bookngo-$service:$IMAGE_VERSION" "$repo:latest"
                            docker push "$repo:$IMAGE_VERSION"
                            docker push "$repo:latest"
                        done
                    '''
                }
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }

        // ---------------------------------------------------------------------------------------
        // Deploy: enable once the EC2 host exists. Needs the "SSH Agent" plugin and two credentials:
        //   'ec2-ssh-key' (SSH Username with private key, user ec2-user) and 'ec2-host' (Secret text).
        // The EC2 host must have Docker + Docker Compose, a compose file that uses the pushed
        // <dockerhub-user>/bookngo-<service>:${IMAGE_TAG} images, and its own .env (never from git).
        // ---------------------------------------------------------------------------------------
        // stage('Deploy to EC2') {
        //     steps {
        //         withCredentials([string(credentialsId: 'ec2-host', variable: 'EC2_HOST')]) {
        //             sshagent(credentials: ['ec2-ssh-key']) {
        //                 sh '''
        //                     ssh -o StrictHostKeyChecking=accept-new "ec2-user@$EC2_HOST" \
        //                         "cd /opt/bookngo && IMAGE_TAG=$IMAGE_VERSION docker compose pull && IMAGE_TAG=$IMAGE_VERSION docker compose up -d"
        //                 '''
        //             }
        //         }
        //     }
        // }
    }

    post {
        always {
            sh '''
                docker rm -f "$CI_POSTGRES" "$CI_KAFKA" > /dev/null 2>&1 || true
                docker network disconnect "$CI_NETWORK" "$(hostname)" > /dev/null 2>&1 || true
                docker network rm "$CI_NETWORK" > /dev/null 2>&1 || true
            '''
        }
        success {
            echo "Pushed bookngo-<service>:${IMAGE_VERSION} and :latest for all services."
        }
        failure {
            echo 'Build failed - see the failing stage in Blue Ocean and the Test Results page.'
        }
    }
}
