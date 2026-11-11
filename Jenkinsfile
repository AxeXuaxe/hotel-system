pipeline {
    agent any

    triggers {
        pollSCM('* * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                bat 'mvn -B compile'
            }
        }

        stage('Static Analysis') {
            steps {
                bat 'mvn -B checkstyle:check'
            }
        }

        stage('Test') {
            steps {
                bat 'mvn -B test'
            }
        }

        stage('Package') {
            steps {
                bat 'mvn -B package -DskipTests'
            }
        }

        stage('Check JAR') {
            steps {
                bat 'if exist target\\hotel-system-1.0-SNAPSHOT.jar (echo JAR check: SUCCESS & dir target\\hotel-system-1.0-SNAPSHOT.jar) else (echo JAR check: FAILURE - file not found & exit /b 1)'
            }
        }
    }
}
