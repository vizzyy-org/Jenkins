#! groovy
import groovy.json.JsonSlurper
JsonSlurper slurper = new JsonSlurper()


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

                    // Create a security group
                    String ret = sh(script: """aws ec2 create-security-group --group-name SSHgroup --description "Security group defining SSH ingress source IPs" """, returnStdout: true)
                    echo "$ret"
                    Map parsedJson = slurper.parseText(ret)
                    def securityGroup = parsedJson.GroupId




                    sh "aws ec2 delete-security-group --group-name SSHgroup"

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
