pipeline {
  agent any

  environment {
    // ---- 아래 값들은 Jenkins에서 환경에 맞게 수정/대체하세요. ----
    // EC2 배포 경로 (EC2에 미리 만들고 docker-compose.ec2.yml + .env를 올려두는 방식)
    EC2_HOST = "3.35.206.118"
    EC2_USER = "ubuntu"
    EC2_PATH = "/home/ubuntu/pg-deploy"

    // Jenkins Credentials ID (Jenkins에 등록된 값)
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
        sh "docker build -t pg-app:latest ."
      }
    }

    stage("Save docker image") {
      steps {
        sh "rm -f pg-app.tar && docker save -o pg-app.tar pg-app:latest"
      }
    }

    stage("Deploy to EC2 (save/load + docker compose)") {
      steps {
        sshagent(credentials: ["${EC2_SSH_CREDENTIALS_ID}"]) {
          sh """
            set -e

            ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} '
              mkdir -p ${EC2_PATH}
            '

            # EC2에 이미 docker-compose.ec2.yml + deploy.env(또는 .env)를 올려두었다고 가정.
            # pg 컨테이너만 업데이트: 이미지 업로드 -> load -> compose up
            scp -o StrictHostKeyChecking=no pg-app.tar ${EC2_USER}@${EC2_HOST}:${EC2_PATH}/pg-app.tar

            ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} '
              cd ${EC2_PATH} && \
              docker load -i pg-app.tar && \
              docker compose -f docker-compose.ec2.yml up -d --no-deps pg
            '
          """
        }
      }
    }
  }
}

