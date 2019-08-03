def comment(ISSUE_NUMBER, message, repo){
    withCredentials([string(credentialsId: 'jenkins-user-api-token', variable: 'TOKEN')]) {
        httpRequest acceptType: 'APPLICATION_JSON',
                contentType: 'APPLICATION_JSON',
                httpMode: 'POST',
                customHeaders: [[name: 'Authorization', value: "token $TOKEN"]],
                requestBody: message,
                url: "https://api.github.com/repos/vizzyy-org/$repo/issues/$ISSUE_NUMBER/comments"
    }
}

def merge(ISSUE_NUMBER, message, repo){
    withCredentials([string(credentialsId: 'jenkins-user-api-token', variable: 'TOKEN')]) {
        httpRequest acceptType: 'APPLICATION_JSON',
                contentType: 'APPLICATION_JSON',
                httpMode: 'PUT',
                customHeaders: [[name: 'Authorization', value: "token $TOKEN"]],
                requestBody: message,
                url: "https://api.github.com/repos/vizzyy-org/$repo/pulls/$ISSUE_NUMBER/merge"
    }
}

def checkoutBranch(ISSUE_NUMBER, repo){
    if(ISSUE_NUMBER) {
        checkout([
                $class           : 'GitSCM', branches: [[name: "pr/$ISSUE_NUMBER"]],
                userRemoteConfigs: [[url          : "git@github.com:"+repo+".git",
                                     refspec      : '+refs/pull/*/head:refs/remotes/origin/pr/*',
                                     credentialsId: 'd9ece77a-be20-4450-93dc-d86862497dfc']]
        ])
    } else {
        checkout([
                $class           : 'GitSCM', branches: [[name: "*/master"]],
                userRemoteConfigs: [[url          : "git@github.com:"+repo+".git",
                                     credentialsId: 'd9ece77a-be20-4450-93dc-d86862497dfc']]
        ])
    }
}