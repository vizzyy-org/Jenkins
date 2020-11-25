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
        stage("Drop Index") {
            steps {
                script {
                    echo "Droping existing index..."

                    withCredentials([string(credentialsId: 'db-root-pw', variable: 'PW')]) {
                        String index_query =  "drop index idx_server_metrics_hostname_timestamp_metric_value on server_metrics;"
                        GString shell_command = """sudo mysql -u root -p$PW -D graphing_data -e "$index_query" """

                        try {
                            sh(shell_command)
                            echo "Index dropped!"
                        } catch (Exception e){
//                            error("Failed to drop index!")
                            echo "Could not drop index. Does it exist?"
                        }
                    }
                }
            }
        }

        stage("Create Index") {
            steps {
                script {
                    echo "Indexing server-metrics table..."

                    withCredentials([string(credentialsId: 'db-root-pw', variable: 'PW')]) {
                        String index_query =  "create index idx_server_metrics_hostname_timestamp_metric_value on server_metrics (hostname, timestamp, metric, value);"
                        GString shell_command = """sudo mysql -u root -p$PW -D graphing_data -e "$index_query" """

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
