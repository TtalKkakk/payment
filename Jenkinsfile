pipeline {
  agent any

  environment {
    // ---- 아래 값들은 Jenkins에서 환경에 맞게 수정/대체하세요. ----
    // EC2 배포 경로 (EC2에 미리 만들고 docker-compose.ec2.yml + .env를 올려두는 방식)
    EC2_HOST = "3.35.206.118"
    // 배포용 SSH Credential의 Username(예: ubuntu)과 동일하게 맞추세요.
    EC2_USER = "ubuntu"
    EC2_PATH = "/home/ubuntu/pg-deploy"

    // Jenkins Credentials ID — Kind: "SSH Username with private key"
    EC2_SSH_CREDENTIALS_ID = "pg-server-jenkins-id"
  }

  stages {
    stage("Checkout") {
      steps {
        checkout scm
      }
    }

    stage("Build jar") {
      steps {
        sh "./gradlew clean bootJar -x test"
      }
    }

    stage("Build docker image") {
      steps {
        // 단순 배포용: 항상 동일한 로컬 태그(pg-app:latest)를 만들어 save/load로 EC2에 전달합니다.
        sh "docker build --platform=linux/amd64 -t pg-app:latest ."
      }
    }

    stage("Save docker image") {
      steps {
        sh "rm -f pg-app.tar && docker save -o pg-app.tar pg-app:latest"
      }
    }

    // sshagent 는 "SSH Agent" 플러그인이 필요합니다. 없으면 withCredentials(sshUserPrivateKey)로 대체합니다.
    stage("Deploy to EC2 (save/load + docker compose)") {
      steps {
        withCredentials([
          sshUserPrivateKey(
            credentialsId: "${env.EC2_SSH_CREDENTIALS_ID}",
            keyFileVariable: "SSH_KEY",
            usernameVariable: "SSH_USER"
          )
        ]) {
          sh """
            set -e
            chmod 600 "\$SSH_KEY"

            ssh -i "\$SSH_KEY" -o StrictHostKeyChecking=no \$SSH_USER@${env.EC2_HOST} '
              mkdir -p ${env.EC2_PATH}
            '

            # EC2에 이미 docker-compose.ec2.yml + .env 를 올려두었다고 가정.
            scp -i "\$SSH_KEY" -o StrictHostKeyChecking=no pg-app.tar \$SSH_USER@${env.EC2_HOST}:${env.EC2_PATH}/pg-app.tar

            ssh -i "\$SSH_KEY" -o StrictHostKeyChecking=no \$SSH_USER@${env.EC2_HOST} '
              cd ${env.EC2_PATH} && \\
              docker load -i pg-app.tar && \\
              docker compose -f docker-compose.ec2.yml up -d --no-deps pg
            '
          """
        }
      }
    }
  }
}

