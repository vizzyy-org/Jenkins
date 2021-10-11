#! groovy

@Library('main') _

currentBuild.displayName = "Remote Command Pipeline [$currentBuild.number]"

pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
    }
    parameters {
        text(name: 'TARGET_HOSTS', defaultValue: 'pi@lights.local,pi@summer.local,pi@vox.local,pi@winter1.local,pi@herbivore.local,pi@carnivore.local,pi@four.local,pi@sensor.local,pi@tree.local', description: '')
        text(name: 'REMOTE_COMMAND', defaultValue: 'sudo apt update; sudo apt -y upgrade; sudo shutdown -r +1', description: '')
    }
    stages {
        stage('Dynamic Stages') {
            steps {
                script {
                    doDynamicParallelSteps(env.TARGET_HOSTS, env.REMOTE_COMMAND)
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

def doDynamicParallelSteps(hosts, command){
    def tasks = [:]
    def HOSTS = hosts
    HOSTS = HOSTS.split(',')
    echo "$HOSTS"

    for (f in HOSTS) {
        def host = "${f}"
        tasks["$host"] = {
            hostStatus = null
            stage("$host") {
                String cmd = """
                        $command
                    """
                def hostStatus = sh(script: "timeout 5 ssh -o ConnectTimeout=3 $host '$cmd'", returnStatus: true)
                if (hostStatus == 255) {
                    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                        sh "exit 1"
                    }
                }
            }
        }
    }

    parallel tasks
}
