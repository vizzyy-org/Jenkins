#! groovy
import groovy.json.JsonSlurper
JsonSlurper slurper = new JsonSlurper()

def IP_ADDRESS;

currentBuild.displayName = "DDNS Pipeline [ " + currentBuild.number + " ]"

pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
    }
    stages {
        stage("Grab IP") {
            steps {
                script {

                    withCredentials([string(credentialsId: 'ddns', variable: 'DDNS')]) {
                        String ret = sh(script: "nslookup $DDNS | grep Address", returnStdout: true)
                        echo "$ret"

                        IP_ADDRESS = ret.split('Address:')[2].trim()

                        echo "DDNS value: $IP_ADDRESS"
                    }


                }
            }
        }

        stage("Configure") {
            steps {
                script {

                    // Delete existing group
                    sh "aws ec2 delete-security-group --group-name SSHgroup"

                    // Create new security group
                    String ret = sh(script: """aws ec2 create-security-group --group-name SSHgroup --description "Security group defining SSH ingress source IPs" """, returnStdout: true)
                    echo "$ret"
                    Map parsedJson = slurper.parseText(ret)
                    def securityGroup = parsedJson.GroupId



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
