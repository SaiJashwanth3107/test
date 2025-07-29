pipeline {
    agent any
    tools {
        maven 'Maven'
        jdk 'JDK' // Configured for Java 21
    }
    environment {
        APP_NAME = "first"
        QA_PORT = "8082"
        PREPROD_PORT = "8083"
        LOG_DIR = "${WORKSPACE}/logs"
        QA_URL = "http://localhost:${QA_PORT}"
    }
    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/PS-Thoufiq/spring-sample-using-docker.git', branch: 'main'
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
        stage('Deploy to QA') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                echo "Deploying to QA environment on port ${QA_PORT}"
                script {
                    // Kill any existing process on QA port
                    try {
                        bat """
                            for /f \"tokens=5\" %%i in ('netstat -aon ^| findstr :${QA_PORT}') do taskkill /F /PID %%i
                        """
                    } catch (Exception e) {
                        echo "No process found on port ${QA_PORT}"
                    }
                    
                    // Start QA instance with redirected output
                    bat """
                        set JAVA_CMD=java -jar target/${APP_NAME}-0.0.1-SNAPSHOT.jar --spring.profiles.active=qa --server.port=${QA_PORT}
                        echo Starting QA instance: %JAVA_CMD%
                        start \"QA_Instance_${BUILD_ID}\" /B cmd /c \"%JAVA_CMD% > ${LOG_DIR}\\qa.log 2>&1\"
                    """
                    
                    // Wait for application to start
                    sleep(time: 60, unit: "SECONDS")
                    
                    // Verify custom health check with retries
                    script {
                        def maxRetries = 3
                        def retryDelay = 10
                        def success = false
                        for (int i = 0; i < maxRetries; i++) {
                            try {
                                bat """
                                    curl -f http://localhost:${QA_PORT}/students/health | findstr \"\\\"status\\\":\\\"UP\\\"\" | findstr \"\\\"stage\\\":\\\"qa\\\"\" || exit 1
                                """
                                success = true
                                break
                            } catch (Exception e) {
                                echo "Health check failed, retrying in ${retryDelay} seconds..."
                                sleep(time: retryDelay, unit: "SECONDS")
                            }
                        }
                        if (!success) {
                            error "QA health check failed after ${maxRetries} retries"
                        }
                    }
                    
                    // Log QA status
                    echo "QA is running on http://localhost:${QA_PORT}/students/health"
                    
                    // Verify process is running
                    bat """
                        netstat -aon | findstr :${QA_PORT} || exit 1
                    """
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
                        if exist target\\failsafe-reports (
                            findstr /m /c:"FAILURE" target\\failsafe-reports\\*.txt && exit 1 || exit 0
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
        stage('Deploy to Pre-Prod') {
            steps {
                echo "Deploying to Pre-Prod on port ${PREPROD_PORT}"
                script {
                    // Kill any existing process on Pre-Prod port
                    try {
                        bat """
                            for /f \"tokens=5\" %%i in ('netstat -aon ^| findstr :${PREPROD_PORT}') do taskkill /F /PID %%i
                        """
                    } catch (Exception e) {
                        echo "No process found on port ${PREPROD_PORT}"
                    }
                    
                    // Start Pre-Prod instance with redirected output
                    bat """
                        set JAVA_CMD=java -jar target/${APP_NAME}-0.0.1-SNAPSHOT.jar --spring.profiles.active=preprod --server.port=${PREPROD_PORT}
                        echo Starting Pre-Prod instance: %JAVA_CMD%
                        start \"PreProd_Instance_${BUILD_ID}\" /B cmd /c \"%JAVA_CMD% > ${LOG_DIR}\\preprod.log 2>&1\"
                    """
                    
                    // Wait for application to start
                    sleep(time: 60, unit: "SECONDS")
                    
                    // Verify custom health check with retries
                    script {
                        def maxRetries = 3
                        def retryDelay = 10
                        def success = false
                        for (int i = 0; i < maxRetries; i++) {
                            try {
                                bat """
                                    curl -f http://localhost:${PREPROD_PORT}/students/health | findstr \"\\\"status\\\":\\\"UP\\\"\" | findstr \"\\\"stage\\\":\\\"preprod\\\"\" || exit 1
                                """
                                success = true
                                break
                            } catch (Exception e) {
                                echo "Health check failed, retrying in ${retryDelay} seconds..."
                                sleep(time: retryDelay, unit: "SECONDS")
                            }
                        }
                        if (!success) {
                            error "Pre-Prod health check failed after ${maxRetries} retries"
                        }
                    }
                    
                    // Log Pre-Prod status
                    echo "Pre-Prod is running on http://localhost:${PREPROD_PORT}/students/health"
                    
                    // Verify process is running
                    bat """
                        netstat -aon | findstr :${PREPROD_PORT} || exit 1
                    """
                }
            }
        }
    }
    post {
        success {
            echo 'Pipeline completed successfully! Both instances are running:'
            echo "QA: http://localhost:${QA_PORT}/students/health (logs: ${LOG_DIR}\\qa.log)"
            echo "Pre-Prod: http://localhost:${PREPROD_PORT}/students/health (logs: ${LOG_DIR}\\preprod.log)"
            echo "To stop these instances, run:"
            echo " taskkill /FI \"WINDOWTITLE eq QA_Instance_${BUILD_ID}\" /T /F"
            echo " taskkill /FI \"WINDOWTITLE eq PreProd_Instance_${BUILD_ID}\" /T /F"
        }
        failure {
            echo 'Pipeline failed. Check logs for details: ${LOG_DIR}\\qa.log and ${LOG_DIR}\\preprod.log'
            // Clean up any running instances
            bat "taskkill /FI \"WINDOWTITLE eq QA_Instance_*\" /T /F || exit 0"
            bat "taskkill /FI \"WINDOWTITLE eq PreProd_Instance_*\" /T /F || exit 0"
        }
    }
}
