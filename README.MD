
# 🎟️ 대규모 트래픽 대응 티켓팅 예매 플랫폼 프로젝트 기획서

## 📝 프로젝트 개요 (Introduction)

본 프로젝트는 단기간에 엄청난 트래픽이 몰리는 대형 콘서트 및 뮤지컬의 '선착순 좌석 예매' 시스템을 모티브로 합니다. 단순한 수량 차감(Coupon) 방식을 넘어, [회원 - 콘서트 - 좌석 - 예매]로 이어지는 유기적인 도메인 흐름 속에서 데이터 정합성을 보장하고 고성능을 발휘하는 아키텍처를 구축합니다.

### ⚡ 기술적 기반 (Architectural Baseline)

본 프로젝트의 핵심 예매 메커니즘은 이전 단계에서 진행한 **"Redis 비동기 대기열 아키텍처"** 성능 실험 결과를 모태로 합니다.

* **이전 실험 지표:** 가상 사용자 3,000명 1초 동시 접속 환경에서 **평균 응답 속도 267ms, 초당 처리량(Throughput) 1,550.4/sec, 에러율 0.00%** 달성.
* https://github.com/Kanghs0212/coupon/blob/master/Redis_README.MD

* **본 프로젝트의 확장 방향:** 락(Lock) 점유로 인한 동기식 DB 커넥션 병목 현상을 배제하기 위해, 사용자의 좌석 선택 요청을 Redis 메모리 계층(List/Set 구조)에서 초고속으로 먼저 선점(Queueing)한 뒤 백그라운드 Worker를 통해 고속으로 DB 갱신 및 영수증(Reservation)을 발행하는 비동기 이벤트를 그대로 계승합니다.

---

## 🛠 1. 개발 환경 및 기술 스택 (Tech Stack)

* **Language:** Java 17
* **Framework:** Spring Boot 3.5.14, Spring Security
* **Build Tool:** Gradle
* **Database:** * PostgreSQL 15 (운영 및 최종 부하 테스트용)
* Redis (초고속 좌석 선점 대기열 및 인증 토큰 관리용)


* **ORM:** Spring Data JPA, Hibernate
* **Authentication:** JWT (JSON Web Token)
* **Performance Test:** Apache JMeter
* **Infrastructure:** Docker & Docker Compose

---

## 🏗 2. 데이터베이스 모델링 및 ER 다이어그램

복잡해진 비즈니스 요구사항에 맞춰 데이터의 중복을 제거하고 연관 관계를 명확히 정의한 논리적 구조입니다.

![diagram.png](coupon/src/main/resources/templates/diagram.png)

### 📊 연관 관계 핵심 설명

* **Member : Reservation (1 : N):** 한 회원은 여러 번 예매할 수 있지만, 특정 예매 내역은 단 한 명의 회원에게 속합니다.
* **Concert : Seat (1 : N):** 하나의 콘서트는 여러 개의 좌석을 가집니다.
* **Seat : Reservation (1 : 1):** 특정 콘서트의 특정 좌석(예: A열 1번)은 단 하나의 예매 내역(영수증)과 매핑됩니다. 이를 통해 동일 좌석에 대한 중복 예매를 원천 차단합니다.

---

## 🔍 3. 데이터베이스 정규화 분석 (Normalization Analysis)

본 시스템에 설계된 테이블 구조는 데이터 중복과 이상 현상(Anomaly)을 방지하기 위해 엄격한 제3정규형(3NF)을 만족하도록 설계되었습니다.

### ① 제1정규형 (1NF) 만족 여부

* **조건:** 모든 도메인이 원자 값(Atomic Value)으로만 구성되어야 합니다.
* **검증:** `Member`의 이름이나 `Seat`의 좌석명(`A열 1번`), `Reservation`의 예매 일시 등 모든 속성은 더 이상 분해할 수 없는 단일 원자 값을 가집니다. 따라서 **1NF를 완벽히 만족**합니다.

### ② 제2정규형 (2NF) 만족 여부

* **조건:** 1NF를 만족하고, 기본키가 복합키일 경우 기본키의 진부분집합이 결정자가 되는 '부분 함수 종속(Partial Functional Dependency)'이 없어야 합니다. (즉, 완전 함수 종속이어야 함)
* **검증:** 본 설계의 모든 테이블(`Member`, `Concert`, `Seat`, `Reservation`)은 복합키가 아닌 대리키(Surrogate Key)인 `id`(단일 컬럼)를 기본키로 사용합니다. 기본키가 단일 컬럼이므로 기본키의 일부에 종속되는 속성이 존재할 수 없습니다. 따라서 **2NF를 완벽히 만족**합니다.

### ③ 제3정규형 (3NF) 만족 여부

* **조건:** 2NF를 만족하고, 기본키가 아닌 일반 속성들 간에 '이행적 함수 종속(Transitive Functional Dependency)'이 없어야 합니다. (즉, $X \rightarrow Y$, $Y \rightarrow Z$ 일 때 $X \rightarrow Z$ 가 성립하는 종속 관계 제거)
* **검증 (Seat 테이블 예시):**
* $id(\text{기본키}) \rightarrow concert\_id$ (좌석 ID는 콘서트를 결정함)
* $id(\text{기본키}) \rightarrow seat\_name$ (좌석 ID는 좌석 이름을 결정함)
* 일반 속성인 $seat\_name \rightarrow price$ 등의 관계가 성립하지 않습니다. (동일한 좌석 이름이라도 공연마다 가격이 다를 수 있으며, 가격은 오직 좌석 ID에 의해서만 결정됨)
* 모든 비기본 속성들은 오직 기본키인 `id`에만 직접 종속될 뿐, 일반 속성을 거쳐 이행적으로 종속되지 않으므로 **3NF를 완벽히 만족**하며 삽입/삭제/수정 이상 현상이 발생하지 않습니다.
