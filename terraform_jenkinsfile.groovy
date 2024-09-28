pipeline{
    agent any 
    environment{
        cred = credentials('aws-key')
    }
    stages{
        stage('checkout stage'){
             steps{
                checkout scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/Vishal-NodeServer/major-jenkins-project.git']])
             }
        }
        stage('terraform init'){
            steps{
                sh 'terraform init'
            }
        }
        stage('terraform plan'){
            steps{
                sh 'terraform plan'
            }
        }
        stage('terraform apply'){
            steps{
                sh 'terraform apply -auto-approve'
            }
        }
    }
}