#! groovy

currentBuild.displayName = "DDNS Pipeline [ " + currentBuild.number + " ]"

pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
    }
    stages {
        stage("Execute") {
            steps {
                script {

                    def ret = sh(script: """aws ec2 create-security-group --group-name SSHgroup --description "Security group defining SSH ingress source IPs" """, returnStdout: true)
                    echo "$ret"
                    echo "Returned $ret.GroupId"

//                    sh """
//
//
//                    """

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
