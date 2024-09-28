pipeline {
    agent any
    environment {
        cred = credentials('aws-key')
        
    }
    options {
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '30', numToKeepStr: '5')
        timeout(30)
    }
    tools {
        maven 'Maven'
    }
    stages {
        stage('checkout stage') {
            steps {
                checkout scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/Vishal-NodeServer/major-jenkins-project.git']])
            }
        }
        stage('sonar test') {
            steps {
                script {
                    stage('SonarQube Analysis') {
                        def mvn = tool 'Maven';
                        withSonarQubeEnv(installationName: 'sonarqube-server') {
                            sh "${mvn}/bin/mvn clean verify sonar:sonar -Dsonar.projectKey=superproject -Dsonar.projectName='superproject'"
                        }
                    }
                }
            }
        }
        stage('maven build'){
                steps{
                    sh 'mvn package'
                }
            }
        stage('nexus test'){
            steps{
                 nexusArtifactUploader(
                        nexusVersion: 'nexus3',
                        protocol: 'http',
                        nexusUrl: '13.201.55.7:8081/',
                        groupId: 'addressbook',
                        version: '2.0-SNAPSHOT',
                        repository: 'maven-snapshots',
                        credentialsId: 'nexus',
                        artifacts: [
                               [artifactId: 'SuperProject',
                               classifier: '',
                               file: 'target/addressbook-2.0.war',
                               type: 'war']
                              ]
                   )
                
            }
        }
        stage('docker build'){
            steps{
                sh "docker build -t super-project ."
            }
        }
        stage('docker push'){
            steps{
                sh "aws ecr get-login-password --region ap-south-1 | docker login --username AWS --password-stdin 891377115612.dkr.ecr.ap-south-1.amazonaws.com"
                sh "docker tag super-project:latest 891377115612.dkr.ecr.ap-south-1.amazonaws.com/super-project:latest"
                sh "docker push 891377115612.dkr.ecr.ap-south-1.amazonaws.com/super-project:latest"
                sh "docker tag 891377115612.dkr.ecr.ap-south-1.amazonaws.com/super-project:latest 891377115612.dkr.ecr.ap-south-1.amazonaws.com/super-project:${BUILD_NUMBER}"
                sh "docker push 891377115612.dkr.ecr.ap-south-1.amazonaws.com/super-project:${BUILD_NUMBER}"
                
            }
        }
        stage('k8s deployment'){
            steps{
                sh "aws eks update-kubeconfig --region ap-south-1 --name super-project"
                sh "kubectl apply -f Application.yaml"
            }
        }
        
        
        }
        post {
  always {
    echo "job completed"
  }
  success {
    echo "success"
  }
  failure {
    echo "failed"
  }
}
}