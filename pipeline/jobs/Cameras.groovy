#! groovy

pipeline {
    agent any
    stages {
        stage("Build Docker Container"){
            steps{
                script{
                    sh ('''
                        cd /home/barney/docker/cameras
                        docker build -t=cameras
                    ''')
                }
            }
        }
        stage("Deploy Docker Container"){
            steps{
                script{
                    echo "Test 1"
                }
            }
        }
    }
}