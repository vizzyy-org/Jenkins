#! groovy

def IP_ADDRESS

currentBuild.displayName = "DDNS Pipeline [ " + currentBuild.number + " ]"

pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
    }
    triggers{ cron('0 * * * *') }
    stages {
        stage("Grab IP") {
            steps {
                script {
                    echo "Grabbing ip"
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
                    withCredentials([string(credentialsId: 'mainEC2', variable: 'INSTANCE_ID'), string(credentialsId: 'WebServerSG', variable: 'WEB_SERVER_SG')]) {

                        // Detach from EC2
                        sh """aws ec2 modify-instance-attribute --instance-id $INSTANCE_ID --groups "$WEB_SERVER_SG" """

                        // Delete existing group
                        sh "aws ec2 delete-security-group --group-name SSHgroup"

                        // Create new security group
                        String ret = sh(script: """aws ec2 create-security-group --group-name SSHgroup --description "Security group defining SSH/DB ingress source IPs" """, returnStdout: true)
                        echo "$ret"
                        def parsedJson = readJSON text: ret
                        def securityGroup = parsedJson.GroupId

                        // Configure the singular IP from DDNS value
                        sh "aws ec2 authorize-security-group-ingress --group-name SSHgroup --protocol tcp --port 22 --cidr $IP_ADDRESS/32"
                        // DB
//                        sh "aws ec2 authorize-security-group-ingress --group-name SSHgroup --protocol tcp --port 3306 --cidr 0.0.0.0/0"

                        // Attach to EC2
                        sh """aws ec2 modify-instance-attribute --instance-id $INSTANCE_ID --groups "$WEB_SERVER_SG" "$securityGroup" """
                    }

                }
            }
        }
    }
    post {
        success {
            echo "SUCCESS"
        }
        failure {
            echo "FAILURE"
        }
    }
}
