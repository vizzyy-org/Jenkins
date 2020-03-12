#! groovy

currentBuild.displayName = "Secrets Pipeline [ " + currentBuild.number + " ]"

pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
    }
    parameters {
        string(name: 'BUCKET_PATH', defaultValue: 'vizzyy/credentials', description: 'S3 bucket path to pull from.')
        string(name: 'ITEM_NAME', defaultValue: '', description: 'S3 item name.')
        string(name: 'NEW_VALUE', defaultValue: '', description: 'New secret value.')
    }
    stages {
        stage("Execute") {
            steps {
                script {
                    if(NEW_VALUE != "" && ITEM_NAME != ""){
                        sh  """
                                echo $NEW_VALUE > $ITEM_NAME;
                                aws s3 cp $ITEM_NAME s3://$BUCKET_PATH/$ITEM_NAME
                                rm $ITEM_NAME
                            """
                    } else if (ITEM_NAME != "") {
                        sh  """
                                aws s3 cp s3://$BUCKET_PATH/$ITEM_NAME ./$ITEM_NAME
                                cat $ITEM_NAME
                                rm $ITEM_NAME
                            """
                    } else {
                        sh "aws s3 ls $BUCKET_PATH/"
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
