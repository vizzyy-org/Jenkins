#! groovy

currentBuild.displayName = "Cameras Pipeline [ " + currentBuild.number + " ]"

pipeline {
    agent any
    stages {
        stage("Build Docker Container"){
            steps{
                script{

                    sh ('''
                        echo $USER
                        cd /home/barney/docker/cameras
                        sudo docker build -t=cameras .
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