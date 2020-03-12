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
        booleanParam(name: 'DELETE_ITEM', defaultValue: false, description: 'Delete this item?')
    }
    stages {
        stage("Execute") {
            steps {
                script {
                    try {
                        if(BUCKET_PATH != "" && NEW_VALUE != "" && ITEM_NAME != ""){
                            sh  """
                                    echo $NEW_VALUE > $ITEM_NAME;
                                    aws s3 cp $ITEM_NAME s3://$BUCKET_PATH/$ITEM_NAME
                                    rm $ITEM_NAME
                                """
                        } else if (BUCKET_PATH != "" && ITEM_NAME != "" && DELETE_ITEM == "true") {
                            sh  """
                                    aws s3 rm s3://$BUCKET_PATH/$ITEM_NAME
                                """
                        } else if (BUCKET_PATH != "" && ITEM_NAME != "") {
                            sh  """
                                    aws s3 cp s3://$BUCKET_PATH/$ITEM_NAME ./$ITEM_NAME
                                    cat $ITEM_NAME
                                    rm $ITEM_NAME
                                """
                        } else {
                            sh "aws s3 ls $BUCKET_PATH/"
                        }
                    } catch (Exception ignored){
                        sh """
                                echo "Oops, didn't work"
                            """
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
