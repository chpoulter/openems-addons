pipeline {

    agent {
        label 'dind-builder'
    }

    triggers {
        cron('H H(1-6) * * *')
        pollSCM('')
    }

    tools {
        maven 'maven-3.9.x'
        jdk 'Java 21'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Clean') {
            steps {
                echo 'Cleaning up the target directory...'
                cleanWs patterns: [[pattern: 'target/**', type: 'INCLUDE']]

                withMaven() {
                    sh 'mvn clean'
                }
            }
        }

        stage('Build') {
            steps {
                withMaven() {
                    sh 'mvn clean compile -DskipTests'
                }
            }
        }

        stage('Test') {
            steps {
                withMaven() {
                    sh 'mvn test'
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
 
        stage('License') {
            steps {
                withMaven() {
                    sh 'mvn -P license package validate generate-resources'
                }
            }
        }

        stage('Package & Deploy') {
            steps {
                withMaven(mavenSettingsConfig: 'repository-release') {
                    sh 'mvn deploy -DskipTests'
                }
            }
        }
    }
}
