#! groovy

currentBuild.displayName = "Mothership Pipeline [ " + currentBuild.number + " ]"

try {
    if (ISSUE_NUMBER)
        echo "Building from pull request..."
} catch (Exception e) {
    ISSUE_NUMBER = false
    echo "Building from jenkins job..."
}

pipeline {
    agent any
    stages {
        stage("Acknowledge") {
            steps {
                script {
                    if(env.Build == "true" && ISSUE_NUMBER) {
                        prTools.comment(ISSUE_NUMBER, """{"body": "Jenkins triggered $currentBuild.displayName"}""", "mothership")
                    }
                }
            }
        }

        stage("Build Docker Container") {
            steps {
                script {
                    if(env.Build == "true") {
                        prTools.checkoutBranch(ISSUE_NUMBER, "vizzyy-org/mothership")

                        sh('''
                            $(aws ecr get-login --no-include-email --region us-east-1)
                            docker build -t login:latest .
                            docker tag login:latest 476889715112.dkr.ecr.us-east-1.amazonaws.com/login:latest
                            docker push 476889715112.dkr.ecr.us-east-1.amazonaws.com/login:latest
                        ''')
                    }
                }
            }
        }

        stage("Deploy Docker Container") {
            steps {
                script {
                    if (env.Deploy == "true") {

                        sh('''
                            aws codepipeline start-pipeline-execution --region us-east-1 --name login-pipeline
                        ''')

                    }
                }
            }
        }
    }
    post {
        success {
            script {
                if(env.Build == "true" && ISSUE_NUMBER) {
                    prTools.merge(ISSUE_NUMBER, """{"commit_title": "Jenkins merged $currentBuild.displayName","merge_method": "merge"}""", "mothership")
                    prTools.comment(ISSUE_NUMBER, """{"body": "Jenkins successfully deployed $currentBuild.displayName"}""" , "mothership")
                }
            }
        }
        failure {
            script {
                if(env.Build == "true" && ISSUE_NUMBER) {
                    prTools.comment(ISSUE_NUMBER, """{"body": "Jenkins failed during $currentBuild.displayName"}""", "mothership")
                }
            }
        }
    }
}
