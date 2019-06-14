#! groovy

currentBuild.displayName = "Cameras Pipeline [ " + currentBuild.number + " ]"

pipeline {
    agent any
    stages {
        stage("Acknowledge") {
            steps {
                script {
                    if(env.Build == "true") {
                        // POST /repos/:owner/:repo/issues/:issue_number/comments
                        withCredentials([string(credentialsId: 'jenkins-user-api-token', variable: 'TOKEN')]) {
                            def message = """{"body": "Jenkins triggered $currentBuild.displayName."}"""
                            httpRequest acceptType: 'APPLICATION_JSON',
                                    contentType: 'APPLICATION_JSON',
                                    httpMode: 'POST',
                                    customHeaders: [[name: 'Authorization', value: "token $TOKEN"]],
                                    requestBody: message,
                                    url: "https://api.github.com/repos/vizzyy-org/cameras/issues/$ISSUE_NUMBER/comments"
                        }
                    }
                }
            }
        }
        stage("Build Docker Container") {
            steps {
                script {
                    if(env.Build == "true") {
                        checkout([
                                $class           : 'GitSCM', branches: [[name: "pr/$ISSUE_NUMBER"]],
                                userRemoteConfigs: [[url          : 'git@github.com:vizzyy-org/cameras.git',
                                                     refspec      : '+refs/pull/*/head:refs/remotes/origin/pr/*',
                                                     credentialsId: 'd9ece77a-be20-4450-93dc-d86862497dfc']]
                        ])
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
                        def running
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
                // PUT /repos/:owner/:repo/pulls/:pull_number/merge
                if(env.Build == "true") {
                    withCredentials([string(credentialsId: 'jenkins-user-api-token', variable: 'TOKEN')]) {
                        def message = """{"commit_title": "Jenkins merged $currentBuild.displayName.","merge_method": "merge"}"""
                        httpRequest acceptType: 'APPLICATION_JSON',
                                contentType: 'APPLICATION_JSON',
                                httpMode: 'PUT',
                                customHeaders: [[name: 'Authorization', value: "token $TOKEN"]],
                                requestBody: message,
                                url: "https://api.github.com/repos/vizzyy-org/cameras/pulls/$ISSUE_NUMBER/merge"
                    }

                    withCredentials([string(credentialsId: 'jenkins-user-api-token', variable: 'TOKEN')]) {
                        def message = """{"body": "Jenkins successfully deployed $currentBuild.displayName."}"""
                        httpRequest acceptType: 'APPLICATION_JSON',
                                contentType: 'APPLICATION_JSON',
                                httpMode: 'POST',
                                customHeaders: [[name: 'Authorization', value: "token $TOKEN"]],
                                requestBody: message,
                                url: "https://api.github.com/repos/vizzyy-org/cameras/issues/$ISSUE_NUMBER/comments"
                    }
                }
            }
        }
        failure {
            script {
                if(env.Build == "true") {
                    withCredentials([string(credentialsId: 'jenkins-user-api-token', variable: 'TOKEN')]) {
                        def message = """{"body": "Jenkins failed during $currentBuild.displayName!"}"""
                        httpRequest acceptType: 'APPLICATION_JSON',
                                contentType: 'APPLICATION_JSON',
                                httpMode: 'POST',
                                customHeaders: [[name: 'Authorization', value: "token $TOKEN"]],
                                requestBody: message,
                                url: "https://api.github.com/repos/vizzyy-org/cameras/issues/$ISSUE_NUMBER/comments"
                    }
                }
            }
        }
    }
}