# CozyNote
<img width="1918" height="1025" alt="image" src="https://github.com/user-attachments/assets/5d4def09-a1f9-473d-bc5e-c9c37150af46" />
JavaFX 21 기반의 블록형 메모장 앱입니다. 사진의 설계 흐름에 맞춰 Gradle, 블록 데이터 모델, SQLite 저장 방식을 반영했습니다.

## 실행

```powershell
gradle run
```

Gradle 대신 기존 보조 스크립트를 쓸 수도 있습니다. SQLite 드라이버는 Gradle 캐시에 내려받혀 있으면 자동으로 classpath에 포함됩니다.

```powershell
.\run.ps1
```

## 기술 스택

- 언어: Java
- UI: JavaFX
- 빌드 도구: Gradle
- UI 구조: JavaFX 코드 기반, 블록 에디터는 `VBox` 렌더링
- 로컬 저장소: SQLite
- 이미지/아이콘 리소스: `src/main/resources`

## 현재 구현 범위

- 1차 앱 껍데기: `main.fxml` 기준 화면 진입
- 상단 메뉴바: 파일, 편집, 보기, 삽입, 서식, 도구, 도움말
- `fx:include` 기반 왼쪽 사이드바, 중앙 에디터 카드, 오른쪽 위젯 패널
- 새 메모 만들기
- 제목 수정
- 블록 기반 본문 작성
- 제목, 텍스트, 체크리스트, 글머리, 인용, 구분선 블록 추가
- 블록 삭제
- 자동 저장: 입력 후 800ms 동안 추가 입력이 없으면 SQLite에 저장
- 저장 상태 표시: 저장 대기, 저장 중, 저장 완료, 저장 실패
- 보상 시스템: 완료된 체크리스트 기준 포인트 계산 및 `user_progress` 저장
- 검색/필터: 제목, 본문 블록, 카테고리, 즐겨찾기, 고정 상태 기준 필터링
- 설정 화면: 일반, 화면, 데이터, 음악, 꾸미기 항목
- 백업/복원: JSON 내보내기와 JSON 파일 읽기 진입점
- 단축키: Ctrl+N, Ctrl+S, Ctrl+F, Ctrl+B/I/U, Ctrl+1, Ctrl+Shift+B/C, Esc
- 즐겨찾기/카테고리/최근 메모 사이드바 UI
- 오른쪽 음악 플레이어/방 꾸미기 위젯 UI
- SQLite 저장: `%USERPROFILE%\.cozynote\cozynote.db`
- 하단 글자 수, 공백 제외 글자 수, 블록 수, 확대 비율 표시

## 데이터 모델

```text
Note
|-- id
|-- title
|-- category
|-- favorite
|-- pinned
|-- createdAt
|-- updatedAt
`-- blocks: List<NoteBlock>

NoteBlock
|-- id
|-- type: HEADING | TEXT | TODO | BULLET | QUOTE | DIVIDER
|-- text
|-- checked
`-- orderIndex
```

## SQLite 스키마

```sql
CREATE TABLE notes (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    category_id TEXT,
    content_json TEXT NOT NULL,
    favorite INTEGER NOT NULL DEFAULT 0,
    pinned INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE categories (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    icon TEXT,
    color TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE app_settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE TABLE user_progress (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
```

처음에는 블록을 별도 테이블로 분리하지 않고 `notes.content_json`에 저장합니다. 검색과 정렬이 복잡해지면 블록 테이블로 분리할 수 있습니다.

## FXML 화면 구조

```text
views/main.fxml
|-- top: MenuBar
`-- center: HBox.app-body
    |-- sidebar.fxml
    |-- editor.fxml
    `-- music-player.fxml
```

오른쪽 위젯에는 `music-player.fxml`과 `room-decor.fxml`이 함께 포함됩니다.
