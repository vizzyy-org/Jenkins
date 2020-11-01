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
                    doDynamicParallelSteps()
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

def doDynamicParallelSteps(){
    tasks = [:]
    for(int i=0; i < HOSTS.size(); i++) {
        host = HOSTS[i]
        echo "Host $host in hosts"
        tasks["${host}"] = {
            hostStatus = null
            stage("$host") {
                echo "$host"
                String cmd = """
                            hostname
                            cd ~/metrics
                            git stash
                            git fetch --all
                            git pull origin master
                        """
                sh("ssh $host '$cmd'")
                hostStatus = sh(script: "ssh  -o ConnectTimeout=3 $host '$cmd'", returnStatus: true)
                echo "Returned status: $hostStatus"
            }
        }
    }
    parallel tasks
}