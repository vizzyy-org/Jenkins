#! groovy

@Library('main') _

currentBuild.displayName = "Metrics Pipeline [$currentBuild.number]"

pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
    }
    stages {
        stage('Dynamic Stages') {
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
    def tasks = [:]
    def HOSTS = config.pull("iam_limited_hosts")

    for (f in HOSTS) {
        def host = "${f}"
        tasks["$host"] = {
            hostStatus = null
            stage("$host") {
                withCredentials([
                        string(credentialsId: 'metrics_id', variable: 'ID'),
                        string(credentialsId: 'metrics_key', variable: 'KEY')
                ]) {
                    String cmd = """crontab -l | grep python | head -n 1 | grep -oE " /.*/python3\\.?[1-9]?" """
                    def python_path = sh(script: "ssh  -o ConnectTimeout=3 $host '$cmd'", returnStdout: true).trim()
                    cmd = "$python_path -m pip install boto3"
                    echo "$cmd"
                    def hostStatus = sh(script: "ssh  -o ConnectTimeout=3 $host '$cmd'", returnStatus: true)
                    if (hostStatus == 255) {
                        catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                            sh "exit 1"
                        }
                    }
                }
            }
        }
    }

    parallel tasks
}