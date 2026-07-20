pipeline {
  agent any

  stages {
    stage('Environment Check') {
      steps {
        sh '''
          echo "===== JDK & Maven ====="
          java --version
          mvn --version
        '''
      }
    }

    stage('Checkout') {
      steps {
        git(url: 'https://gitee.com/synap-xnet/xnet-mlops.git', credentialsId: 'gitee-synap-xnet-id', branch: 'v0.0.1', changelog: true, poll: false)
      }
    }

    stage('Build') {
      steps {
        sh 'mvn clean install -U -DskipTests'
      }
    }

    stage('Docker Build') {
      parallel {
        stage('Build login') {
          steps {
            sh 'docker build -f mlops-login/Dockerfile -t 127.0.0.1/xnet-mlops/xnet-mlops-login:${BUILD_NUMBER} mlops-login/'
          }
        }
        stage('Build dpp') {
          steps {
            sh 'docker build -f mlops-dpp-service/Dockerfile -t 127.0.0.1/xnet-mlops/xnet-mlops-dpp-service:${BUILD_NUMBER} mlops-dpp-service/'
          }
        }
        stage('Build mtp') {
          steps {
            sh 'docker build -f mlops-mtp-service/Dockerfile -t 127.0.0.1/xnet-mlops/xnet-mlops-mtp-service:${BUILD_NUMBER} mlops-mtp-service/'
          }
        }
        stage('Build smp') {
          steps {
            sh 'docker build -f mlops-smp-service/Dockerfile -t 127.0.0.1/xnet-mlops/xnet-mlops-smp-service:${BUILD_NUMBER} mlops-smp-service/'
          }
        }
      }
    }

    stage('Docker Push') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'docker-registry-creds', passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
          sh '''
            echo "$DOCKER_PASS" | docker login 127.0.0.1 -u "$DOCKER_USER" --password-stdin

            docker push 127.0.0.1/xnet-mlops/xnet-mlops-login:${BUILD_NUMBER}
            docker push 127.0.0.1/xnet-mlops/xnet-mlops-dpp-service:${BUILD_NUMBER}
            docker push 127.0.0.1/xnet-mlops/xnet-mlops-mtp-service:${BUILD_NUMBER}
            docker push 127.0.0.1/xnet-mlops/xnet-mlops-smp-service:${BUILD_NUMBER}
          '''
        }
      }
    }

    stage('Deploy login') {
      steps {
        withCredentials([kubeconfigContent(credentialsId: 'kube-secret', variable: 'KUBE_CONFIG')]) {
          sh '''
            mkdir -p ~/.kube
            echo "$KUBE_CONFIG" > ~/.kube/config
            chmod 600 ~/.kube/config
            sed -i "s|image: 127.0.0.1/xnet-mlops/xnet-mlops-login:.*|image: 127.0.0.1/xnet-mlops/xnet-mlops-login:${BUILD_NUMBER}|" mlops-login/deploy.yaml
            kubectl apply -f mlops-login/deploy.yaml -n xnet-mlops
            kubectl rollout restart deployment xnet-mlops-login-deployment -n xnet-mlops
          '''
        }
      }
    }

    stage('Deploy dpp') {
      steps {
        withCredentials([kubeconfigContent(credentialsId: 'kube-secret', variable: 'KUBE_CONFIG')]) {
          sh '''
            mkdir -p ~/.kube
            echo "$KUBE_CONFIG" > ~/.kube/config
            chmod 600 ~/.kube/config
            sed -i "s|image: 127.0.0.1/xnet-mlops/xnet-mlops-dpp-service:.*|image: 127.0.0.1/xnet-mlops/xnet-mlops-dpp-service:${BUILD_NUMBER}|" mlops-dpp-service/deploy.yaml
            kubectl apply -f mlops-dpp-service/deploy.yaml -n xnet-mlops
            kubectl rollout restart deployment xnet-mlops-dpp-service-deployment -n xnet-mlops
          '''
        }
      }
    }

    stage('Deploy mtp') {
      steps {
        withCredentials([kubeconfigContent(credentialsId: 'kube-secret', variable: 'KUBE_CONFIG')]) {
          sh '''
            mkdir -p ~/.kube
            echo "$KUBE_CONFIG" > ~/.kube/config
            chmod 600 ~/.kube/config
            sed -i "s|image: 127.0.0.1/xnet-mlops/xnet-mlops-mtp-service:.*|image: 127.0.0.1/xnet-mlops/xnet-mlops-mtp-service:${BUILD_NUMBER}|" mlops-mtp-service/deploy.yaml
            kubectl apply -f mlops-mtp-service/deploy.yaml -n xnet-mlops
            kubectl rollout restart deployment xnet-mlops-mtp-service-deployment -n xnet-mlops
          '''
        }
      }
    }

    stage('Deploy smp') {
      steps {
        withCredentials([kubeconfigContent(credentialsId: 'kube-secret', variable: 'KUBE_CONFIG')]) {
          sh '''
            mkdir -p ~/.kube
            echo "$KUBE_CONFIG" > ~/.kube/config
            chmod 600 ~/.kube/config
            sed -i "s|image: 127.0.0.1/xnet-mlops/xnet-mlops-smp-service:.*|image: 127.0.0.1/xnet-mlops/xnet-mlops-smp-service:${BUILD_NUMBER}|" mlops-smp-service/deploy.yaml
            kubectl apply -f mlops-smp-service/deploy.yaml -n xnet-mlops
            kubectl rollout restart deployment xnet-mlops-smp-service-deployment -n xnet-mlops
          '''
        }
      }
    }
  }
}
