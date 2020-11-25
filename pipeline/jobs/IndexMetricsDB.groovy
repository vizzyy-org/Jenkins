#! groovy

currentBuild.displayName = "DB-Indexing Pipeline [$currentBuild.number]"

pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
    }
    triggers{ cron('0 5 * * *') } // Every day at 5AM
    stages {
        stage("Index Database") {
            steps {
                script {
                    echo "Indexing server-metrics table..."

                    withCredentials([string(credentialsId: 'db-root-pw', variable: 'PW')]) {
                        String index_query =  "create index idx_server_metrics_hostname_timestamp_metric_value on server_metrics (hostname, timestamp, metric, value);"
                        String shell_command = """sudo mysql -u root -p$PW -D graphing_data -e "$index_query" >> ./output.txt"""

                        try {
                            sh(shell_command)
                            echo "Indexing completed!"
                        } catch (Exception e){
                            error("Failed to create new index!")
                        }
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
