#! groovy

currentBuild.displayName = "Metrics Pipeline [$currentBuild.number]"

HOSTS = [
        'pi@lights.local',
        'pi@summer.local',
        'pi@vox.local',
        'pi@battery.local',
        'pi@octopi.local',
        'pi@winter1.local',
        'barney@dinkleberg.local',
        'pi@herbivore.local',
        'pi@carnivore.local',
        'pi@four.local'
        ]

pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
    }
    stages {
        stage('Dynamic Stages') {
            agent any
            steps {
                script {
                    for(int i=0; i < HOSTS.size(); i++) {
                        hostStatus = null
                        host = HOSTS[i]
                        stage("Ping $host"){
                            echo "$host"
                            echo "Element: $i"
                            hostStatus = sh(script: "ssh -q $host exit", returnStatus: true)
                            echo hostStatus
                        }
                        stage("Ping Host"){
                            when {
                                expression {
                                    return hostStatus == 0
                                }
                            }
                            echo "Host UP!"
                            sh "ssh $host 'echo hello; exit'"
                        }
                    }
                }
            }
        }

    }
    post {
        success {
            script {
                sh "echo SUCCESS"
            }
        }
        failure {
            script {
                sh "echo FAILURE"
            }
        }
    }
}
