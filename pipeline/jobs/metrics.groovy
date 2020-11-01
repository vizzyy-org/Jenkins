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
//    triggers{ cron('0 * * * *') }
    stages {
//        stage("Grab IP") {
//            steps {
//                script {
//
//                    withCredentials([string(credentialsId: 'ddns', variable: 'DDNS')]) {
//                        String ret = sh(script: "nslookup $DDNS | grep Address", returnStdout: true)
//                        echo "$ret"
//
//                        IP_ADDRESS = ret.split('Address:')[2].trim()
//
//                        echo "DDNS value: $IP_ADDRESS"
//                    }
//
//
//                }
//            }
//        }
//
//        stage("Configure") {
//            steps {
//                script {
//
//                    withCredentials([string(credentialsId: 'mainEC2', variable: 'INSTANCE_ID'), string(credentialsId: 'WebServerSG', variable: 'WEB_SERVER_SG')]) {
//
//                        // Detach from EC2
//                        sh """aws ec2 modify-instance-attribute --instance-id $INSTANCE_ID --groups "$WEB_SERVER_SG" """
//
//                        // Delete existing group
//                        sh "aws ec2 delete-security-group --group-name SSHgroup"
//
//                        // Create new security group
//                        String ret = sh(script: """aws ec2 create-security-group --group-name SSHgroup --description "Security group defining SSH/DB ingress source IPs" """, returnStdout: true)
//                        echo "$ret"
//                        Map parsedJson = slurper.parseText(ret) as Map
//                        def securityGroup = parsedJson.GroupId
//
//                        // Configure the singular IP from DDNS value
//                        sh "aws ec2 authorize-security-group-ingress --group-name SSHgroup --protocol tcp --port 22 --cidr $IP_ADDRESS/32"
//                        // DB
////                        sh "aws ec2 authorize-security-group-ingress --group-name SSHgroup --protocol tcp --port 3306 --cidr 0.0.0.0/0"
//
//                        // Attach to EC2
//                        sh """aws ec2 modify-instance-attribute --instance-id $INSTANCE_ID --groups "$WEB_SERVER_SG" "$securityGroup" """
//                    }
//
//                }
//            }
//        }
        stage('Dynamic Stages') {
            agent any
            steps {
                script {
                    for(int i=0; i < HOSTS.size(); i++) {
                        stage("Ping Host"){
                            host = HOSTS[i]
                            echo "$host"
                            echo "Element: $i"
                        }
                    }
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
