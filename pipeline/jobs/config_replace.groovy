#! groovy

@Library('main') _

currentBuild.displayName = "Config Replace Pipeline [$currentBuild.number]"

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
                withCredentials([
                        string(credentialsId: 'grafana_db_user_pw', variable: 'grafana_db_user_pw'),
                        string(credentialsId: 'DDNS', variable: 'DDNS'),
                ]) {

                    String cmd = """cat ~/metrics/config* | grep ssl_ca"""
                    def ssl_line = sh(script: "ssh  -o ConnectTimeout=3 $host '$cmd'", returnStdout: true).trim()
                    cmd = """cat ~/metrics/config* | grep HOSTNAME"""
                    def hostname_line = sh(script: "ssh  -o ConnectTimeout=3 $host '$cmd'", returnStdout: true).trim()
                    cmd = """cat ~/metrics/config* | grep DISK_DRIVES"""
                    def drives_line = sh(script: "ssh  -o ConnectTimeout=3 $host '$cmd'", returnStdout: true).trim()
                    def db_host = hostname_line.contains("dinkleberg") ? "localhost" : hostname_line.contains("t4g") ? "$DDNS" : "dinkleberg.local"

                    def configFile = """
from mysql.connector.constants import ClientFlag

SSL_CONFIG = {
    "user": "grafana_db_user",
    "password": "$grafana_db_user_pw",
    "host": "$db_host",
    "port": 9004,
    "database": "graphing_data",
    "client_flags": [ClientFlag.SSL],
    $ssl_line
}
queue_name = "DatabaseMessageQueue"
$hostname_line
$drives_line
""".replaceAll("'", '"')

                    configFile = configFile.replaceAll('"', '\\\\"')

                    echo "$configFile"

                    def today = new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())

                    cmd = """cp ~/metrics/config.py ~/metrics/config.py.bak$today; echo "$configFile" > ~/metrics/config.py"""

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