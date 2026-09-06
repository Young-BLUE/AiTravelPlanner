import { useState } from 'react';
import './AiTravelLanding.css';

// 임시 목업 데이터 - 실제로는 API/CMS에서 받아올 예정
// 이미지는 public/destinations/ 에 로컬 보관 (출처/라이선스는 CREDITS 참고)
const TOP_DESTINATIONS = [
  { rank: 1, city: '도쿄', country: '일본', landmark: '도쿄타워', img: '/destinations/tokyo.jpg', tag: '3박 4일 · 70만원대' },
  { rank: 2, city: '파리', country: '프랑스', landmark: '에펠탑', img: '/destinations/paris.jpg', tag: '5박 7일 · 220만원대' },
  { rank: 3, city: '방콕', country: '태국', landmark: '왓 아룬', img: '/destinations/bangkok.jpg', tag: '4박 5일 · 90만원대' },
  { rank: 4, city: '발리', country: '인도네시아', landmark: '타나롯 사원', img: '/destinations/bali.jpg', tag: '5박 6일 · 130만원대' },
  { rank: 5, city: '뉴욕', country: '미국', landmark: '타임스스퀘어', img: '/destinations/newyork.jpg', tag: '5박 7일 · 250만원대' },
];

const QUICK_PROMPTS = [
  '3박 4일 도쿄, 예산 80만원',
  '아이와 함께 가는 오사카',
  '가을에 좋은 유럽 소도시',
  '혼자 떠나는 제주 2박 3일',
];

const BUDGET_TIERS = [
  { label: '50만원 이하', sub: '가까운 동남아·근거리', places: ['다낭', '방콕', '세부'] },
  { label: '50~100만원', sub: '일본·대만·홍콩', places: ['오사카', '타이베이', '홍콩'] },
  { label: '100만원 이상', sub: '장거리 · 유럽/미주', places: ['파리', '뉴욕', '스위스'] },
];

const SEASON_PICKS = [
  { label: '봄', period: '3–5월', places: ['교토', '워싱턴DC'] },
  { label: '여름', period: '6–8월', places: ['발리', '산토리니'] },
  { label: '가을', period: '9–11월', places: ['교토', '뉴욕'] },
  { label: '겨울', period: '12–2월', places: ['삿포로', '헬싱키'] },
];

const FEATURES = [
  {
    title: '일정 생성',
    desc: 'AI가 목적지와 취향에 맞춰 일자별 코스를 짜드려요',
    icon: (
      <>
        <rect x="3" y="5" width="18" height="16" rx="3" />
        <path d="M3 10h18M8 3v4M16 3v4" />
      </>
    ),
  },
  {
    title: '예산 비교',
    desc: '숙소·항공권 예상 가격대를 한눈에 확인해요',
    icon: (
      <>
        <path d="M4 19V10M10 19V5M16 19v-6M22 19H2" />
      </>
    ),
  },
  {
    title: '예약 연결',
    desc: '마음에 드는 옵션을 골라 바로 예약까지 이어가요',
    icon: (
      <>
        <path d="M14 4h6v6" />
        <path d="M20 4l-9 9" />
        <path d="M19 14v4a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h4" />
      </>
    ),
  },
];

// 사진 출처 - CC BY-SA 라이선스는 저작자 표시가 필요해 푸터에 노출
const PHOTO_CREDITS = [
  { city: '도쿄', author: 'Akonnchiroll', license: 'CC BY-SA 4.0', href: 'https://commons.wikimedia.org/wiki/File:Tokyo_Tower_2023.jpg' },
  { city: '파리', author: 'Jorge Royan', license: 'CC BY-SA 3.0', href: 'https://commons.wikimedia.org/wiki/File:Paris_-_The_Eiffel_Tower_in_spring_-_2307.jpg' },
  { city: '방콕', author: 'Kasidhorn Rachaoros', license: 'CC BY-SA 4.0', href: 'https://commons.wikimedia.org/wiki/File:(2022)_%E0%B8%A7%E0%B8%B1%E0%B8%94%E0%B8%AD%E0%B8%A3%E0%B8%B8%E0%B8%93%E0%B8%A3%E0%B8%B2%E0%B8%8A%E0%B8%A7%E0%B8%A3%E0%B8%B2%E0%B8%A3%E0%B8%B2%E0%B8%A1%E0%B8%A3%E0%B8%B2%E0%B8%8A%E0%B8%A7%E0%B8%A3%E0%B8%A1%E0%B8%AB%E0%B8%B2%E0%B8%A7%E0%B8%B4%E0%B8%AB%E0%B8%B2%E0%B8%A3_%E0%B9%80%E0%B8%82%E0%B8%95%E0%B8%9A%E0%B8%B2%E0%B8%87%E0%B8%81%E0%B8%AD%E0%B8%81%E0%B9%83%E0%B8%AB%E0%B8%8D%E0%B9%88_%E0%B8%81%E0%B8%A3%E0%B8%B8%E0%B8%87%E0%B9%80%E0%B8%97%E0%B8%9E%E0%B8%A1%E0%B8%AB%E0%B8%B2%E0%B8%99%E0%B8%84%E0%B8%A3_Wat_Arun_(23).jpg' },
  { city: '발리', author: 'Grayswoodsurrey', license: 'CC BY-SA 4.0', href: 'https://commons.wikimedia.org/wiki/File:TanahLot_2014.JPG' },
  { city: '뉴욕', author: 'Terabass', license: 'CC BY-SA 3.0', href: 'https://commons.wikimedia.org/wiki/File:New_york_times_square-terabass.jpg' },
];

function FeatureIcon({ children }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {children}
    </svg>
  );
}

export default function AiTravelLanding() {
  const [prompt, setPrompt] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    // TODO: LLM API 호출로 연결
    console.log('여행 계획 요청:', prompt);
  };

  return (
    <div className="atl-page">
      <header className="atl-header">
        <div className="atl-header-inner">
          <a className="atl-logo" href="/">
            <span className="atl-logo-mark" aria-hidden="true" />
            Wayfarer
          </a>
          <nav className="atl-nav">
            <a href="#top5">인기 여행지</a>
            <a href="#budget">예산별 추천</a>
            <a href="#season">시기별 추천</a>
          </nav>
          <button type="button" className="atl-login">로그인</button>
        </div>
      </header>

      <main className="atl-main">
        {/* 히어로 + 프롬프트 입력 */}
        <section className="atl-hero">
          <p className="atl-eyebrow">AI 여행 플래너</p>
          <h1 className="atl-title">
            어디로 갈지만 알려주세요.
            <br />
            <span className="atl-title-accent">일정은 AI가 짜드릴게요.</span>
          </h1>
          <p className="atl-subtitle">
            목적지·기간·예산만 입력하면 일자별 코스와 예상 경비를 한 번에 정리해 드려요.
          </p>

          <form className="atl-prompt-box" onSubmit={handleSubmit}>
            <textarea
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              placeholder="떠나고 싶은 여행지, 계획을 세워보세요"
              rows={2}
            />
            <button type="submit" className="atl-submit" aria-label="일정 만들기">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M5 12h13M12 5l7 7-7 7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          </form>

          <div className="atl-chips">
            {QUICK_PROMPTS.map((q) => (
              <button type="button" key={q} className="atl-chip" onClick={() => setPrompt(q)}>
                {q}
              </button>
            ))}
          </div>
        </section>

        {/* Top5 인기 여행지 */}
        <section className="atl-section" id="top5">
          <div className="atl-section-head">
            <h2>지금 인기 여행지</h2>
            <p>최근 일주일간 가장 많이 검색된 목적지예요</p>
          </div>
          <ul className="atl-top5">
            {TOP_DESTINATIONS.map((d) => (
              <li className="atl-dest-card" key={d.rank}>
                <div className="atl-dest-thumb">
                  <img src={d.img} alt={`${d.city}의 ${d.landmark}`} loading="lazy" style={d.pos ? { objectPosition: d.pos } : undefined} />
                  <span className="atl-dest-rank">{d.rank}</span>
                </div>
                <div className="atl-dest-body">
                  <p className="atl-dest-city">
                    {d.city}
                    <span className="atl-dest-country">{d.country}</span>
                  </p>
                  <p className="atl-dest-landmark">{d.landmark}</p>
                  <p className="atl-dest-tag">{d.tag}</p>
                </div>
              </li>
            ))}
          </ul>
        </section>

        {/* 기능 소개 */}
        <section className="atl-section">
          <ul className="atl-features">
            {FEATURES.map((f) => (
              <li className="atl-feature" key={f.title}>
                <span className="atl-feature-icon">
                  <FeatureIcon>{f.icon}</FeatureIcon>
                </span>
                <div>
                  <h3>{f.title}</h3>
                  <p>{f.desc}</p>
                </div>
              </li>
            ))}
          </ul>
        </section>

        {/* 예산별 / 시기별 추천 */}
        <section className="atl-section atl-columns">
          <div className="atl-col" id="budget">
            <div className="atl-section-head">
              <h2>예산별 추천</h2>
              <p>항공·숙소를 포함한 1인 기준 예상 경비예요</p>
            </div>
            <ul className="atl-tier-list">
              {BUDGET_TIERS.map((t) => (
                <li key={t.label}>
                  <div className="atl-tier-main">
                    <span className="atl-tier-label">{t.label}</span>
                    <span className="atl-tier-sub">{t.sub}</span>
                  </div>
                  <span className="atl-tier-places">{t.places.join(' · ')}</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="atl-col" id="season">
            <div className="atl-section-head">
              <h2>시기별 추천</h2>
              <p>날씨와 성수기를 함께 고려한 추천이에요</p>
            </div>
            <ul className="atl-season-grid">
              {SEASON_PICKS.map((s) => (
                <li className="atl-season-chip" key={s.label}>
                  <span className="atl-season-label">
                    {s.label}
                    <em>{s.period}</em>
                  </span>
                  <span className="atl-season-places">{s.places.join(', ')}</span>
                </li>
              ))}
            </ul>
          </div>
        </section>

        <p className="atl-disclaimer">
          표시되는 금액은 실시간 요금이 아닌 <strong>참고용 예상 가격대</strong>입니다. 실제 요금은 예약 시점과 조건에 따라
          달라질 수 있어요.
        </p>
      </main>

      <footer className="atl-footer">
        <div className="atl-footer-inner">
          <div className="atl-footer-brand">
            <span className="atl-logo-mark" aria-hidden="true" />
            <div>
              <p className="atl-footer-name">Wayfarer</p>
              <p className="atl-footer-desc">AI 여행 일정 추천 &amp; 예약 연결</p>
            </div>
          </div>
          <p className="atl-credits">
            사진 출처:{' '}
            {PHOTO_CREDITS.map((c, i) => (
              <span key={c.city}>
                {i > 0 && ' · '}
                <a href={c.href} target="_blank" rel="noreferrer noopener">
                  {c.city}
                </a>{' '}
                {c.author} ({c.license})
              </span>
            ))}{' '}
            / Wikimedia Commons
          </p>
        </div>
      </footer>
    </div>
  );
}
