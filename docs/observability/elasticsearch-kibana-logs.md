# Elasticsearch + Kibana로 애플리케이션 로그 확인하기

Spring Boot(pg) 로그를 Elasticsearch에 쌓고 Kibana에서 조회하는 방법입니다.

---

## 1. 전체 구조

```
[pg 애플리케이션] → 로그 출력 (콘솔/파일)
        ↓
   [Filebeat] → 로그 수집·전송 (선택)
        ↓
[Elasticsearch] → 저장·검색
        ↓
   [Kibana] → 시각화·조회
```

- **Elasticsearch**: 로그 저장·검색 엔진
- **Kibana**: Elasticsearch 데이터 조회·대시보드
- **Filebeat**: 앱 로그 파일/콘솔을 읽어 Elasticsearch로 전송 (선택 사항)

---

## 2. Elasticsearch + Kibana 실행 (Docker)

### 2.1 단일 스택 (ES + Kibana만)

```bash
# Elasticsearch (단일 노드, 개발용)
docker run -d --name elasticsearch \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  -p 9200:9200 -p 9300:9300 \
  docker.elastic.co/elasticsearch/elasticsearch:8.15.0

# Kibana
docker run -d --name kibana \
  -e ELASTICSEARCH_HOSTS=http://host.docker.internal:9200 \
  -p 5601:5601 \
  docker.elastic.co/kibana/kibana:8.15.0
```

- Elasticsearch: **http://localhost:9200**
- Kibana: **http://localhost:5601**

(Windows/Mac에서 `host.docker.internal`이 동작하지 않으면 `--add-host=host.docker.internal:host-gateway` 추가 또는 같은 네트워크로 연결.)

### 2.2 Docker Compose로 한 번에 띄우기 (권장)

프로젝트 루트에 `docker-compose.logging.yml` 예시:

```yaml
# docker-compose.logging.yml (예시)
version: '3'
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.15.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
    ports:
      - "9200:9200"
      - "9300:9300"
    healthcheck:
      test: ["CMD-SHELL", "curl -s http://localhost:9200/_cluster/health | grep -q '\"status\":\"green\"\\|\"status\":\"yellow\"'"]
      interval: 10s
      timeout: 5s
      retries: 10

  kibana:
    image: docker.elastic.co/kibana/kibana:8.15.0
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    ports:
      - "5601:5601"
    depends_on:
      elasticsearch:
        condition: service_healthy
```

실행:

```bash
docker compose -f docker-compose.logging.yml up -d
```

---

## 3. 로그를 Elasticsearch로 보내는 방법

### 방법 A: Filebeat로 로그 파일 전송 (추천, 앱 수정 최소)

1. **앱에서 로그를 파일로 남기기**

`application.yml`에 예:

```yaml
logging:
  file:
    name: logs/pg.log
  level:
    root: INFO
    com.example.pg: DEBUG
```

2. **Filebeat 설치·설정**

- [Filebeat 다운로드](https://www.elastic.co/downloads/beats/filebeat) 후 압축 해제.
- `filebeat.yml`에서 다음처럼 설정:

```yaml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /path/to/your/project/pg/logs/pg.log
    json.keys_under_root: true
    json.add_error_key: true

output.elasticsearch:
  hosts: ["localhost:9200"]
  index: "pg-logs-%{+yyyy.MM.dd}"

setup.template.name: "pg-logs"
setup.template.pattern: "pg-logs-*"
```

3. **Filebeat 실행**

```bash
./filebeat -e -c filebeat.yml
```

- 로그가 `pg-logs-yyyy.MM.dd` 인덱스로 들어갑니다.

### 방법 B: Spring Boot에서 JSON 로그 + Filebeat로 수집

1. **의존성 추가** (`build.gradle`)

```groovy
implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
```

2. **Logback JSON 설정** (`src/main/resources/logback-spring.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/pg.json</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/pg-%d{yyyy-MM-dd}.json</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="JSON_FILE"/>
    </root>
    <logger name="com.example.pg" level="DEBUG"/>
</configuration>
```

3. **Filebeat**는 `logs/pg.json` (또는 `logs/pg-*.json`)을 가리키도록 설정하면, JSON 필드 그대로 Elasticsearch에서 검색·필터링하기 좋습니다.

### 방법 C: Logback에서 TCP로 Logstash/Elasticsearch에 직접 전송

- Logstash나 Elasticsearch에 TCP/Syslog로 보내는 appender를 두는 방식입니다.
- 설정이 복잡하고, 앱과 스택 결합이 커지므로, **먼저 방법 A 또는 B + Filebeat**를 추천합니다.

---

## 4. Kibana에서 로그 확인하기

1. **Kibana 접속**  
   브라우저에서 **http://localhost:5601** 로 접속.

2. **인덱스 패턴 생성**
   - 왼쪽 메뉴 **Stack Management** → **Data Views** (또는 Index Patterns).
   - **Create data view**:
     - Name: `pg-logs`
     - Index pattern: `pg-logs-*` (Filebeat 인덱스 이름에 맞게).
   - **Save**.

3. **로그 조회**
   - 왼쪽 메뉴 **Discover** 선택.
   - 상단에서 방금 만든 data view `pg-logs` 선택.
   - 시간 범위를 “Last 15 minutes” 등으로 설정하면, 해당 기간 로그가 표시됩니다.

4. **검색·필터**
   - 상단 검색창에 예: `message:*결제*`, `level:ERROR` 등으로 검색.
   - JSON으로 남겼다면 `message`, `logger_name`, `level` 등 필드로 필터 가능.

---

## 5. 요약 체크리스트

| 단계 | 작업 |
|------|------|
| 1 | Docker로 Elasticsearch, Kibana 실행 (또는 Docker Compose 사용) |
| 2 | 앱 로그를 파일로 남기기 (`logging.file.name` 또는 logback 파일 appender) |
| 3 | Filebeat로 해당 로그 파일 → Elasticsearch 전송 (인덱스 예: `pg-logs-*`) |
| 4 | Kibana에서 data view `pg-logs-*` 생성 후 Discover에서 로그 조회 |

이 순서대로 하면 Elasticsearch와 Kibana를 이용해 pg 애플리케이션의 데이터(로그)를 한곳에서 확인할 수 있습니다.

---

## 6. 배포 환경 + 로컬 ES/Kibana만 사용할 때 (추천 방식)

**상황**: pg 앱은 배포(서버/클라우드)하고, Elasticsearch와 Kibana는 로컬에서만 돌리며 배포된 앱의 로그를 로컬에서 보고 싶을 때.

**중요**: 배포 서버에서 로컬 PC의 Elasticsearch로 **직접 전송**하려면 로컬을 인터넷에 노출(ngrok 등)해야 해서 보안·설정이 부담됩니다. 그래서 **로그는 배포 서버에 파일로만 쌓고, 그 파일을 주기적으로 로컬로 가져온 뒤 로컬 Filebeat로 로컬 ES에 넣는 방식**을 추천합니다.

### 6.1 추천: 배포 서버 로그 파일 → 로컬로 가져와서 로컬 Filebeat → 로컬 ES

```
[배포된 pg 앱] → 로그 파일 (예: /var/log/pg/pg.log)
       ↑
  (주기적으로 가져오기: scp / rsync / S3 등)
       ↓
[로컬 PC] logs/pg.log (또는 logs/pg-*.log)
       ↓
[로컬 Filebeat] → [로컬 Elasticsearch] → [로컬 Kibana]
```

**배포 서버에서 할 일**

- 앱이 로그를 **파일**로 남기도록 설정 (배포용 설정 프로파일).
  - 예: `logging.file.name: /var/log/pg/pg.log` 또는 로그 백/롤링 경로 하나로 통일.
- (선택) 로그가 너무 많으면 일단 **일별로 S3/스토리지에 업로드**하는 스크립트만 배포 서버에 두고, 로컬에서는 그 스토리지에서만 가져와도 됨.

**로컬에서 할 일**

1. 로컬에서 **Elasticsearch + Kibana**만 Docker 등으로 실행 (이미 가이드한 대로).
2. 배포 서버의 로그 파일을 **주기적으로 로컬로 복사**.
   - 예: `scp`, `rsync`, 또는 S3 등에 올려뒀다면 `aws s3 cp` 등으로 로컬 `logs/` 로 다운로드.
3. 로컬 **Filebeat** 설정에서 `paths`를 복사해 온 파일 경로(예: `logs/pg.log`, `logs/pg-*.log`)로 두고, `output.elasticsearch`는 `localhost:9200`으로 설정.
4. Filebeat 실행 → 로컬 ES에 인덱싱 → Kibana Discover에서 조회.

**장점**: 로컬 ES/Kibana를 인터넷에 노출할 필요 없음. 배포 쪽은 “로그 파일만 남기기”로 단순함.

#### 단계별 작업 목록

아래 순서대로 진행하면 된다.

| 단계 | 구분 | 작업 | 비고 |
|------|------|------|------|
| **1** | 로컬 | Elasticsearch + Kibana 실행 | `docker compose -f docker-compose.logging.yml up -d` |
| **2** | 앱 설정 | 로그를 **파일**로 남기도록 설정 | 로컬: `logs/pg.log` / 배포: `prod` 프로파일 시 `/var/log/pg/pg.log`. 아래 "2번 과정 상세" 참고 |
| **3** | 로컬 | Filebeat 설치 | [다운로드](https://www.elastic.co/downloads/beats/filebeat) 후 압축 해제 |
| **4** | 로컬 | Filebeat 설정 (`filebeat.yml`) | `paths`: 로컬에 복사해 둔 로그 파일 경로, `output.elasticsearch.hosts`: `["localhost:9200"]`, `index`: `pg-logs-%{+yyyy.MM.dd}` |
| **5** | 배포 | pg 앱 배포 | 서버/클라우드에 배포 후 앱이 2번에서 정한 경로에 로그 파일 생성하는지 확인 |
| **6** | 로컬 | 배포 서버 로그 파일을 로컬로 복사 | `scp`, `rsync` 또는 S3 등에서 `aws s3 cp` 등으로 로컬 `logs/`(또는 정한 폴더)에 저장 |
| **7** | 로컬 | Filebeat 실행 | `./filebeat -e -c filebeat.yml` (4번 paths에 6번에서 복사한 파일이 있도록 함) |
| **8** | 로컬 | Kibana에서 Data View 생성 | Stack Management → Data Views → Create → Index pattern: `pg-logs-*` |
| **9** | 로컬 | Kibana Discover에서 로그 조회 | Data View 선택, 시간 범위 설정 후 검색·필터 |

**반복할 작업**: 로그를 계속 수집하려면 **6 → 7**을 주기적으로 실행하거나, 6번을 cron/스크립트로 자동화한 뒤 7번(Filebeat)을 상시 실행해 두면 된다.

#### 6.1.2 2번 과정 상세: 로그를 파일로 남기기

Spring Boot는 `logging.file.name`(또는 구식 `logging.file`)만 설정하면 콘솔 로그와 동일한 내용을 **해당 경로의 파일**에도 쓴다. 로컬과 배포에서 경로만 다르게 쓰려면 프로파일을 나누면 된다.

**1) 공통(로컬) 설정 — `application.yml`**

이미 다음이 들어가 있으면 로컬 실행 시 프로젝트 루트 기준 `logs/pg.log`에 로그가 쌓인다.

```yaml
logging:
  file:
    name: logs/pg.log
  level:
    root: INFO
    com.example.pg: DEBUG
```

- 실행 시 **작업 디렉터리**가 프로젝트 루트여야 `logs/pg.log`가 그 아래 생성된다. (IDE 실행 루트 또는 `./gradlew bootRun` 실행 위치 기준.)

**2) 배포용 설정 — `application-prod.yml`**

배포 서버에서는 보통 고정 경로를 쓰는 것이 좋다. `src/main/resources/application-prod.yml`에 다음만 두면, `prod` 프로파일 사용 시 해당 경로에 로그가 쌓인다.

```yaml
logging:
  file:
    name: /var/log/pg/pg.log
```

- 배포 서버에 `/var/log/pg/` 디렉터리를 미리 만들고, 앱을 실행하는 사용자에게 쓰기 권한을 줘야 한다.
  - 예: `sudo mkdir -p /var/log/pg && sudo chown 앱실행유저:앱실행유저 /var/log/pg`

**3) 배포 시 프로파일 활성화**

- JAR 실행: `java -jar pg.jar --spring.profiles.active=prod`
- 환경 변수: `SPRING_PROFILES_ACTIVE=prod`
- 클라우드/컨테이너 설정에서 `spring.profiles.active=prod` 지정

**4) 확인**

- 로컬: 앱 실행 후 `logs/pg.log` 파일이 생기고, 요청 등 로그가 계속 추가되는지 확인.
- 배포: 서버에서 `prod`로 실행한 뒤 `/var/log/pg/pg.log`가 생성·갱신되는지 확인.

이렇게 하면 2번(로그를 파일로 남기기)이 완료되고, 이후 단계에서 이 파일을 로컬로 가져와 Filebeat로 Elasticsearch에 넣으면 된다.

### 6.2 대안: 배포 서버에서 S3 등에 로그 적재 → 로컬에서 S3 수집 후 로컬 ES

- 배포 서버: 앱 로그 파일 + (cron 등으로) Filebeat 또는 스크립트가 **S3(또는 다른 스토리지)** 에 로그 파일 업로드.
- 로컬: 주기적으로 S3에서 로그 파일을 다운로드한 뒤, **로컬 Filebeat**가 그 파일을 읽어 **로컬 Elasticsearch**로 전송.
- 로컬 ES/Kibana는 그대로 로컬 전용; 배포 서버는 S3만 알면 됨.

### 6.3 비추천: 배포 앱/Filebeat가 로컬 ES로 직접 전송

- 로컬 Elasticsearch를 인터넷에서 접근 가능하게(예: ngrok, 포트포워딩) 해야 함.
- 보안·방화벽 관리가 부담되고, 개발/분석용으로는 보통 과한 구성입니다.

---

**요약**: 배포는 하되 ES/Kibana는 로컬만 쓸 때는 **“배포 서버는 로그 파일만 생성 → 그 파일을 로컬로 가져와서 로컬 Filebeat → 로컬 ES → 로컬 Kibana”** 방식이 가장 단순하고 안전합니다.
