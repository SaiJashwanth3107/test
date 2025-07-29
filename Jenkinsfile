pipeline {
    agent any
    tools {
        maven 'Maven'
        jdk 'JDK' // Configured for Java 21
    }
    environment {
        APP_NAME = "first"
        DOCKER_REGISTRY = "your-docker-registry" // e.g., "docker.io/yourusername"
        DOCKER_IMAGE = "${DOCKER_REGISTRY}/${APP_NAME}"
        DOCKER_TAG = "${env.BUILD_ID}"
        QA_PORT = "8082"
        PREPROD_PORT = "8083"
        LOG_DIR = "${WORKSPACE}/logs"
        QA_URL = "http://localhost:${QA_PORT}"
    }
    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/Thoufiq26/spring.git', branch: 'main'
            }
        }
        stage('Create Log Directory') {
            steps {
                bat 'mkdir "%LOG_DIR%" || exit 0'
            }
        }
        stage('Build') {
            steps {
                bat 'mvnw.cmd clean package'
            }
        }
        stage('Unit Tests') {
            steps {
                bat 'mvnw.cmd test'
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    // Create Dockerfile if it doesn't exist
                    if (!fileExists('Dockerfile')) {
                        writeFile file: 'Dockerfile', text: """
                            FROM eclipse-temurin:21-jdk-jammy
                            WORKDIR /app
                            COPY target/${APP_NAME}-0.0.1-SNAPSHOT.jar app.jar
                            ENTRYPOINT ["java", "-jar", "app.jar"]
                        """
                    }
                    
                    // Build Docker image
                    bat "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                }
            }
        }
        stage('Deploy to QA (Docker)') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                echo "Deploying to QA environment on port ${QA_PORT}"
                script {
                    // Stop and remove any existing container
                    bat "docker stop ${APP_NAME}-qa || exit 0"
                    bat "docker rm ${APP_NAME}-qa || exit 0"
                    
                    // Run QA container
                    bat """
                        docker run -d \
                          --name ${APP_NAME}-qa \
                          -p ${QA_PORT}:${QA_PORT} \
                          -e SPRING_PROFILES_ACTIVE=qa \
                          -e SERVER_PORT=${QA_PORT} \
                          ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """
                    
                    // Wait for application to start
                    sleep(time: 60, unit: "SECONDS")
                    
                    // Verify custom health check
                    bat """
                        curl -f http://localhost:${QA_PORT}/students/health | findstr \"\\\"status\\\":\\\"UP\\\"\" | findstr \"\\\"stage\\\":\\\"qa\\\"\" || exit 1
                    """
                    
                    // Log QA status
                    echo "QA is running on http://localhost:${QA_PORT}/students/health"
                }
            }
        }
        stage('Integration Tests') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                script {
                    // Run integration tests against the QA environment
                    bat """
                        mvnw.cmd verify -Dspring.profiles.active=qa -Dservice.url=${QA_URL}
                    """
                    
                    // Verify integration tests passed
                    bat """
                        if exist target\\surefire-reports (
                            findstr /m /c:"FAILURE" target\\surefire-reports\\*.txt && exit 1 || exit 0
                        )
                    """
                }
            }
        }
        stage('Approval for Pre-Prod') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                input message: 'Integration tests passed. Approve deployment to Pre-Prod?', ok: 'Deploy'
            }
        }
        stage('Deploy to Pre-Prod (Docker)') {
            steps {
                echo "Deploying to Pre-Prod on port ${PREPROD_PORT}"
                script {
                    // Stop and remove any existing container
                    bat "docker stop ${APP_NAME}-preprod || exit 0"
                    bat "docker rm ${APP_NAME}-preprod || exit 0"
                    
                    // Run Pre-Prod container
                    bat """
                        docker run -d \
                          --name ${APP_NAME}-preprod \
                          -p ${PREPROD_PORT}:${PREPROD_PORT} \
                          -e SPRING_PROFILES_ACTIVE=preprod \
                          -e SERVER_PORT=${PREPROD_PORT} \
                          ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """
                    
                    // Wait for application to start
                    sleep(time: 60, unit: "SECONDS")
                    
                    // Verify custom health check
                    bat """
                        curl -f http://localhost:${PREPROD_PORT}/students/health | findstr \"\\\"status\\\":\\\"UP\\\"\" | findstr \"\\\"stage\\\":\\\"preprod\\\"\" || exit 1
                    """
                    
                    // Log Pre-Prod status
                    echo "Pre-Prod is running on http://localhost:${PREPROD_PORT}/students/health"
                }
            }
        }
        stage('Push to Docker Registry') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                script {
                    // Login to Docker registry (credentials should be configured in Jenkins)
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        bat "docker login -u %DOCKER_USER% -p %DOCKER_PASS%"
                    }
                    
                    // Push the Docker image
                    bat "docker push ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    
                    // Optionally tag as latest and push
                    bat "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
                    bat "docker push ${DOCKER_IMAGE}:latest"
                }
            }
        }
    }
    post {
        success {
            echo 'Pipeline completed successfully! Both containers are running:'
            echo "QA: http://localhost:${QA_PORT}/students/health"
            echo "Pre-Prod: http://localhost:${PREPROD_PORT}/students/health"
            echo "Docker image pushed to: ${DOCKER_IMAGE}:${DOCKER_TAG}"
            echo "To stop these containers, run:"
            echo " docker stop ${APP_NAME}-qa ${APP_NAME}-preprod"
        }
        failure {
            echo 'Pipeline failed. Check logs for details.'
            // Clean up any running containers
            bat "docker stop ${APP_NAME}-qa || exit 0"
            bat "docker rm ${APP_NAME}-qa || exit 0"
            bat "docker stop ${APP_NAME}-preprod || exit 0"
            bat "docker rm ${APP_NAME}-preprod || exit 0"
        }
    }
}
