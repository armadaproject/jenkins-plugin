pipeline {
  agent {
    armada {
      yaml '''
        apiVersion: v1
        kind: Pod
        spec:
          containers:
          - name: maven
            image: maven:3.9.9-eclipse-temurin-17
            command:
            - cat
            tty: true
            resources:
                requests:
                  memory: "256Mi"
                  cpu: "500m"
                limits:
                  memory: "256Mi"
                  cpu: "500m"
          - name: busybox
            image: busybox
            command:
            - cat
            tty: true
            resources:
                requests:
                  memory: "256Mi"
                  cpu: "500m"
                limits:
                  memory: "256Mi"
                  cpu: "500m"
        '''
    }
  }
  stages {
    stage('Run maven') {
      steps {
        armadaContainer('maven') {
          sh 'mvn -version'
        }
        armadaContainer('busybox') {
          sh '/bin/busybox'
        }
      }
    }
  }
}