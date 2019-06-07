#! groovy

currentBuild.displayName = "Cameras Pipeline [ " + currentBuild.number + " ]"

pipeline {
    agent any
    stages {
        stage("Stop Container"){
            steps{
                sh('''
                    sudo docker stop cameras
                    sudo docker rm cameras    
                ''')
            }
        }
        stage("Build Docker Container"){
            steps{
                sh ('''
                    cd /home/barney/docker/cameras
                    sudo docker build -t=cameras .
                ''')
            }
        }
        stage("Deploy Docker Container"){
            steps{
                sh('sudo docker run -d -p 80:6000 --name cameras cameras')
            }
        }
    }
}