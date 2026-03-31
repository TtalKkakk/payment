pipeline {
  agent any

  environment {
    // ---- 아래 값들은 Jenkins에서 환경에 맞게 수정/대체하세요. ----
    // EC2 배포 경로 (EC2에 미리 만들고 docker-compose.ec2.yml + .env를 올려두는 방식)
    EC2_HOST = "13.209.84.21"
    // 배포용 SSH Credential의 Username(예: ubuntu)과 동일하게 맞추세요.
    EC2_USER = "ubuntu"
    EC2_PATH = "/home/ubuntu/pg-deploy"

    // Jenkins Credentials ID — Kind: "SSH Username with private key"
    EC2_SSH_CREDENTIALS_ID = "pg-server-jenkins-id"

    // Jenkins Credentials ID — Kind: "Secret file" (.env 파일)
    ENV_FILE_CREDENTIALS_ID = "pg-ec2-env"
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

    stage("Deploy to EC2 (upload jar + docker build + compose)") {
      steps {
        withCredentials([
          sshUserPrivateKey(
            credentialsId: "${env.EC2_SSH_CREDENTIALS_ID}",
            keyFileVariable: "SSH_KEY",
            usernameVariable: "SSH_USER"
          ),
          file(
            credentialsId: "${env.ENV_FILE_CREDENTIALS_ID}",
            variable: "ENV_FILE"
          )
        ]) {
          sh """
            set -e
            chmod 600 "\$SSH_KEY"

            ssh -i "\$SSH_KEY" -o StrictHostKeyChecking=no \$SSH_USER@${env.EC2_HOST} '
              mkdir -p ${env.EC2_PATH}
            '

            # EC2에 이미 docker-compose.ec2.yml + Dockerfile 을 올려두었다고 가정.
            # Jenkins에서 만든 JAR만 올리고, docker build는 EC2에서 수행.
            JAR_FILE=\$(ls -1 build/libs/*.jar | head -n 1)
            scp -i "\$SSH_KEY" -o StrictHostKeyChecking=no "\$JAR_FILE" \$SSH_USER@${env.EC2_HOST}:${env.EC2_PATH}/app.jar
            scp -i "\$SSH_KEY" -o StrictHostKeyChecking=no "\$ENV_FILE" \$SSH_USER@${env.EC2_HOST}:${env.EC2_PATH}/.env

            ssh -i "\$SSH_KEY" -o StrictHostKeyChecking=no \$SSH_USER@${env.EC2_HOST} '
              set -e
              cd ${env.EC2_PATH}
              docker build --platform=linux/amd64 -t pg-app:latest .
              docker compose -f docker-compose.ec2.yml up -d --no-deps pg
            '
          """
        }
      }
    }
  }
}

