#! groovy

@Library('main') _

currentBuild.displayName = "Metrics Pipeline [$currentBuild.number]"

HOSTS = hosts.hosts.collect()
echo HOSTS[0].toString()

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

    for (f in HOSTS) {
        def host = "${f}"
        tasks["$host"] = {
            hostStatus = null
            stage("$host") {
                String cmd = """
                            cd ~/metrics
                            git stash
                            git fetch --all
                            git pull origin master
                        """
                hostStatus = sh(script: "ssh  -o ConnectTimeout=3 $host '$cmd'", returnStatus: true)
                if (hostStatus == 255){
                    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                        sh "exit 1"
                    }
                }
            }
        }
    }

    parallel tasks
}