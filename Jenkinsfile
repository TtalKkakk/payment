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

    // (권장) 운영 .env 파일을 Jenkins Credentials(Secret file)로 관리할 때 사용
    // Jenkins → Credentials에서 Kind: "Secret file"로 업로드한 뒤 ID를 여기에 넣으세요.
    // 비워두면(기본값) repo 내 ".env.ec2"가 있을 때만 전송합니다.
    EC2_ENV_FILE_CREDENTIALS_ID = "pg-ec2-env"
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

    stage("Resolve jar path") {
      steps {
        script {
          env.BOOT_JAR = sh(returnStdout: true, script: "ls -1 build/libs/*.jar | head -n 1").trim()
          if (!env.BOOT_JAR) {
            error("bootJar 결과물을 찾지 못했습니다. (build/libs/*.jar)")
          }
          echo "BOOT_JAR=${env.BOOT_JAR}"
        }
      }
    }

    stage("Deploy to EC2 (jar -> docker build -> docker compose)") {
      steps {
        script {
          def useEnvCredential = env.EC2_ENV_FILE_CREDENTIALS_ID != null && !env.EC2_ENV_FILE_CREDENTIALS_ID.trim().isEmpty()

          def creds = [
            sshUserPrivateKey(
              credentialsId: "${env.EC2_SSH_CREDENTIALS_ID}",
              keyFileVariable: "SSH_KEY",
              usernameVariable: "SSH_USER"
            )
          ]
          if (useEnvCredential) {
            creds.add(file(credentialsId: "${env.EC2_ENV_FILE_CREDENTIALS_ID}", variable: "ENV_FILE"))
          }

          withCredentials(creds) {
            sh """
              set -e
              chmod 600 "\$SSH_KEY"

              ssh -i "\$SSH_KEY" -o StrictHostKeyChecking=no \$SSH_USER@${env.EC2_HOST} '
                mkdir -p ${env.EC2_PATH}
              '

              # Jenkins에서 만든 jar + Dockerfile + compose(+.dockerignore)를 EC2로 전송
              scp -i "\$SSH_KEY" -o StrictHostKeyChecking=no "${BOOT_JAR}" \$SSH_USER@${env.EC2_HOST}:${env.EC2_PATH}/app.jar
              scp -i "\$SSH_KEY" -o StrictHostKeyChecking=no Dockerfile \$SSH_USER@${env.EC2_HOST}:${env.EC2_PATH}/Dockerfile
              scp -i "\$SSH_KEY" -o StrictHostKeyChecking=no docker-compose.ec2.yml \$SSH_USER@${env.EC2_HOST}:${env.EC2_PATH}/docker-compose.ec2.yml
              if [ -f .dockerignore ]; then
                scp -i "\$SSH_KEY" -o StrictHostKeyChecking=no .dockerignore \$SSH_USER@${env.EC2_HOST}:${env.EC2_PATH}/.dockerignore
              fi
            """

            if (useEnvCredential) {
              sh """
                # 운영 .env (Jenkins Secret file) → EC2로 덮어쓰기
                scp -i "\$SSH_KEY" -o StrictHostKeyChecking=no "\$ENV_FILE" \$SSH_USER@${env.EC2_HOST}:${env.EC2_PATH}/.env
              """
            } else {
              def envFileExists = (sh(returnStatus: true, script: 'test -f .env.ec2') == 0)
              if (envFileExists) {
                sh """
                  scp -i "\$SSH_KEY" -o StrictHostKeyChecking=no .env.ec2 \$SSH_USER@${env.EC2_HOST}:${env.EC2_PATH}/.env
                """
              } else {
                echo "SKIP: .env.ec2가 워크스페이스에 없어 .env 전송을 건너뜁니다. (권장: Jenkins Secret file로 EC2_ENV_FILE_CREDENTIALS_ID 설정)"
              }
            }

            sh """
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
}

