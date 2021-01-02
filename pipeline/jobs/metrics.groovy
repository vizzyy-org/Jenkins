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
    def HOSTS = config.pull("hosts")

    for (f in HOSTS) {
        def host = "${f}"
        tasks["$host"] = {
            hostStatus = null
            stage("$host") {
                String cmd = """
                            cd ~/metrics
                            git stash
                            git fetch --all
                            git pull origin daemon
                        """
                if ($host == "2011mbp"){
                    cmd += """
                            sudo launchctl stop com.metrics.app
                            sudo launchctl start com.metrics.app
                            sudo launchctl list | grep com.metrics.app
                        """
                } else {
                    cmd += """
                            sudo systemctl restart metrics
                            sudo systemctl status metrics
                        """
                }
                def hostStatus = sh(script: "ssh  -o ConnectTimeout=3 $host '$cmd'", returnStatus: true)
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