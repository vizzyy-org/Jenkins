#! groovy

currentBuild.displayName = "Secrets Pipeline [ " + currentBuild.number + " ]"

//try {
//    if (ISSUE_NUMBER)
//        echo "Building from pull request..."
//} catch (Exception e) {
//    ISSUE_NUMBER = false
//    echo "Building from jenkins job..."
//}

pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
    }
    parameters {
        string(name: 'BUCKET', defaultValue: 'vizzyy', description: 'S3 bucket to pull from.')
        string(name: 'ITEM_PATH', defaultValue: '/credentials', description: 'S3 item path.')
    }
    stages {
        stage("Acknowledge") {
            steps {
                script {
                    sh "aws s3 ls"
                }
            }
        }
    }
//    post {
//        success {
//            script {
//                if(env.Build == "true" && ISSUE_NUMBER) {
//                    prTools.merge(ISSUE_NUMBER, """{"commit_title": "Jenkins merged $currentBuild.displayName","merge_method": "merge"}""", "mothership")
//                    prTools.comment(ISSUE_NUMBER, """{"body": "Jenkins successfully deployed $currentBuild.displayName"}""" , "mothership")
//                }
//            }
//        }
//        failure {
//            script {
//                if(env.Build == "true" && ISSUE_NUMBER) {
//                    prTools.comment(ISSUE_NUMBER, """{"body": "Jenkins failed during $currentBuild.displayName"}""", "mothership")
//                }
//            }
//        }
//    }
}
