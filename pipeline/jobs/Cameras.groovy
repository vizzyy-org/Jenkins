#! groovy

currentBuild.displayName = "Cameras Pipeline [ " + currentBuild.number + " ]"

pipeline {
    agent any
    stages {
        stage("Stop Container"){
            steps{
                if(env.DEPLOY) {
                    sh('''
                        sudo docker stop cameras
                        sudo docker rm cameras    
                    ''')
                }
            }
        }
        stage("Build Docker Container"){
            steps{
                checkout([
                        $class: 'GitSCM', branches: [[name: '*/master']],
                        userRemoteConfigs: [[url: 'git@github.com:Vizzyy/cameras.git',credentialsId:'d9ece77a-be20-4450-93dc-d86862497dfc']]
                ])
                sh ('''
                    cd /home/barney/docker/cameras
                    sudo docker build -t=cameras .
                ''')
            }
        }
        stage("Deploy Docker Container"){
            steps{
                if(env.DEPLOY) {
                    sh('sudo docker run -d -p 80:6000 --name cameras cameras')
                }
            }
        }
    }
}