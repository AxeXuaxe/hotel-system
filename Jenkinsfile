pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                bat 'mvn compile'
            }
        }

        stage('Test') {
            steps {
                bat 'mvn test'
            }
        }

        stage('Package') {
            steps {
                bat 'mvn package -DskipTests'
            }
        }

        stage('Check JAR') {
            steps {
                bat 'if exist target\\hotel-system-1.0-SNAPSHOT.jar (echo JAR check: SUCCESS & dir target\\hotel-system-1.0-SNAPSHOT.jar) else (echo JAR check: FAILURE - file not found & exit /b 1)'
            }
        }
    }
}
