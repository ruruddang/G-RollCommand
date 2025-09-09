# G-RollCommand

**G-RollCommand**는 미리 정의된 **가중치 리스트(weighted list)**에서 무작위로 커맨드를 뽑아  
콘솔에서 실행하는 마인크래프트 플러그인입니다.  

---

## ✨ 주요 기능
- 가중치 기반 랜덤 추첨 시스템
- 콘솔 명령어 자동 실행
- 여러 리스트 관리 가능
- `config.yml` 수정 후 `/rollreload`로 즉시 반영

---

## ⚙️ 설치 방법
1. `plugins/` 폴더에 `G-RollCommand.jar` 파일을 넣습니다.
2. 서버 실행 시 `config.yml`이 생성됩니다.
3. `config.yml`에서 **리스트와 가중치**를 설정합니다.
4. `/rollreload` 명령어로 설정을 다시 불러옵니다.

---

## 🛠️ 사용법

### 명령어
- `/roll <리스트명>`  
  지정한 리스트에서 가중치에 따라 랜덤으로 선택된 커맨드를 **콘솔**에서 실행합니다.  
  - 예: `/roll rewards`

- `/rollreload`  
  `config.yml`을 다시 불러와서 최신 설정을 적용합니다.

---

### 권한
```yaml
permissions:
  groll.use:
    description: "/roll 명령어 사용 권한"
    default: true
  groll.reload:
    description: "/rollreload 명령어 사용 권한"
    default: op
