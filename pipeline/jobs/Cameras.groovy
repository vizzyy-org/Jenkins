#! groovy

currentBuild.displayName = "Cameras Pipeline [ " + currentBuild.number + " ]"

try {
    def temp = ISSUE_NUMBER
} catch (Exception e) {
    ISSUE_NUMBER = false
}

pipeline {
    agent any
    stages {
        stage("Acknowledge") {
            steps {
                script {
                    if(env.Build == "true" && ISSUE_NUMBER) {
                        prTools.comment(ISSUE_NUMBER, """{"body": "Jenkins triggered $currentBuild.displayName"}""")
                    }
                }
            }
        }
        stage("Build Docker Container") {
            steps {
                script {
                    if(env.Build == "true") {
                        prTools.checkoutBranch(ISSUE_NUMBER, "vizzyy-org/cameras")

                        withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: 'docker-login',
                                          usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                            sh('''
                                sudo docker build -t=cameras .
                                echo $PASSWORD | sudo docker login -u $USERNAME --password-stdin
                                sudo docker tag cameras:latest vizzyy/images:cameras
                                sudo docker push vizzyy/images:cameras
                            ''')
                        }
                    }
                }
            }
        }
        stage("Stop Container") {
            steps {
                script {
                    if (env.Deploy == "true") {
                        try {
                            sh('sudo docker stop cameras')
                        } catch (Exception e) {
                            echo "Could not stop container."
                        }
                        try {
                            sh('sudo docker rm cameras')
                        } catch (Exception e) {
                            echo "Could not remove container."
                        }
                    }
                }
            }
        }
        stage("Deploy Docker Container") {
            steps {
                script {
                    if (env.Deploy == "true") {
                        withCredentials([string(credentialsId: 'VOX_KEY', variable: 'vox_key'),
                                         string(credentialsId: 'OCULUS_KEY', variable: 'oculus_key')]) {
                            sh('''
                                sudo docker run -d \
                                -p 80:6000 \
                                --name cameras \
                                -e VOX_KEY=$vox_key \
                                -e OCULUS_KEY=$oculus_key \
                                vizzyy/images:cameras
                            ''')
                        }

                    }
                }
            }
        }
    }
    post {
        success {
            script {
                if(env.Build == "true" && ISSUE_NUMBER) {
                    prTools.merge(ISSUE_NUMBER, """{"commit_title": "Jenkins merged $currentBuild.displayName","merge_method": "merge"}""")
                    prTools.comment(ISSUE_NUMBER, """{"body": "Jenkins successfully deployed $currentBuild.displayName"}""")
                }
            }
        }
        failure {
            script {
                if(env.Build == "true" && ISSUE_NUMBER) {
                    prTools.comment(ISSUE_NUMBER, """{"body": "Jenkins failed during $currentBuild.displayName"}""")
                }
            }
        }
    }
}