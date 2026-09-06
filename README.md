# AI 여행 플래너 (Wayfarer)

목적지·기간·예산을 입력하면 AI가 여행 일정을 짜주고, 예약 플랫폼으로 연결하는 서비스의 프론트엔드 스타터입니다.

## 시작하기

```bash
npm install
npm run dev
```

브라우저에서 `http://localhost:5173` 접속.

## 폴더 구조

```
src/
  main.jsx              # 앱 진입점
  App.jsx                # 최상위 컴포넌트
  AiTravelLanding.jsx     # 랜딩 페이지 (Top5 목적지 / 프롬프트 입력 / 예산·시기 추천)
  AiTravelLanding.css     # 랜딩 페이지 스타일
  index.css               # 글로벌 리셋
```

## 다음 단계 (TODO)

- [ ] `AiTravelLanding.jsx`의 `handleSubmit`에 LLM API 연동
- [ ] Top5 목적지 이미지 실제 사진으로 교체
- [ ] 어필리에이트 딥링크 연결 (아고다/부킹닷컴/스카이스캐너)
- [ ] 무료 3회 이용 후 로그인 게이트 구현
- [ ] 라우팅 라이브러리 추가 (react-router 등) 및 결과 페이지 분리
