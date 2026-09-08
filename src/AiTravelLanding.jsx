import { useEffect, useRef, useState } from 'react';
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

// 캐시 키에 그대로 들어가므로 백엔드 CacheKey.KNOWN_* 와 값이 일치해야 한다
const COMPANIONS = ['혼자', '연인', '친구', '가족'];
const BUDGETS = ['가성비', '보통', '프리미엄'];
const INTERESTS = ['관광', '맛집', '쇼핑', '카페', '야경', '애니메이션', '자연', '테마파크'];
// 버튼으로 노출할 짧은 일정. 그보다 길면 '일주일 이상'에서 고른다
const QUICK_NIGHTS = [1, 2, 3, 4];
const LONG_NIGHTS = [5, 6, 7, 8, 9, 10, 13];

// 관심사는 고를수록 캐시 조합이 곱으로 늘어난다. 3개로 제한해 히트율을 지킨다
const MAX_INTERESTS = 3;

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

const krw = (n) => `${Math.round((n ?? 0) / 10000)}만원`;

// 도시명을 함께 넣어야 동명의 다른 장소로 잡히지 않는다
const mapUrl = (city, place) =>
  `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${city} ${place}`)}`;

function MapPinIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"
         strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" />
      <circle cx="12" cy="10" r="3" />
    </svg>
  );
}

/**
 * 생성된 일정 표시. 지금은 히어로 아래 인라인으로 붙어 있고,
 * 라우팅 추가 시 별도 결과 페이지로 분리할 예정.
 */
/** 목적지를 아직 안 정한 요청에 대한 추천 목록. 카드를 누르면 일정 생성으로 이어진다. */
function SuggestionsResult({ data, onPick, disabled }) {
  return (
    <section className="atl-section atl-result">
      <div className="atl-section-head">
        <h2>{data.theme}</h2>
        <p>마음에 드는 곳을 고르면 일정까지 짜드려요</p>
      </div>

      <ul className="atl-suggest-grid">
        {data.destinations.map((d) => (
          <li className="atl-suggest-card" key={`${d.city}-${d.country}`}>
            <div className="atl-suggest-head">
              <p className="atl-suggest-city">
                {d.city}
                <span className="atl-suggest-country">{d.country}</span>
              </p>
              <span className="atl-suggest-season">{d.bestSeason}</span>
            </div>

            <p className="atl-suggest-reason">{d.reason}</p>

            <ul className="atl-suggest-highlights">
              {d.highlights.map((h) => (
                <li key={h}>{h}</li>
              ))}
            </ul>

            <div className="atl-suggest-foot">
              <span className="atl-suggest-budget">
                {d.nights}박 {d.nights + 1}일 · {krw(d.estBudgetKrw)}
              </span>
              <button
                type="button"
                className="atl-suggest-btn"
                onClick={() => onPick(d.planPrompt)}
                disabled={disabled}
              >
                일정 짜기
              </button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}

export function ItineraryResult({ data }) {
  return (
    <section className="atl-section atl-result">
      <div className="atl-section-head">
        <h2>
          {data.destination}
          <span className="atl-result-country">{data.country}</span>
        </h2>
        <p>
          {data.nights}박 {data.nights + 1}일 · {data.style}
        </p>
      </div>

      <ul className="atl-result-days">
        {data.days.map((d) => (
          <li className="atl-result-day" key={d.day}>
            <div className="atl-result-day-head">
              <span className="atl-result-day-no">{d.day}일차</span>
              <span className="atl-result-day-theme">{d.theme}</span>
              <span className="atl-result-day-cost">{krw(d.estCostKrw)}</span>
            </div>
            <ol className="atl-result-places">
              {d.places.map((p, i) => (
                <li key={`${d.day}-${i}`}>
                  <span className="atl-result-time">{p.time}</span>
                  <div>
                    <p className="atl-result-place-name">
                      {p.name}
                      <span className="atl-result-cat">{p.category}</span>
                      <a
                        className="atl-map-link"
                        href={mapUrl(data.destination, p.name)}
                        target="_blank"
                        rel="noreferrer noopener"
                        title={`${p.name} 구글 지도에서 보기`}
                        aria-label={`${p.name} 구글 지도에서 보기 (새 탭)`}
                      >
                        <MapPinIcon />
                        지도
                      </a>
                    </p>
                    <p className="atl-result-desc">{p.description}</p>
                    {p.moveFromPrev && <p className="atl-result-move">{p.moveFromPrev}</p>}
                  </div>
                </li>
              ))}
            </ol>
          </li>
        ))}
      </ul>

      <div className="atl-result-budget">
        <h3>예상 경비 (1인)</h3>
        <ul>
          <li><span>항공</span><span>{krw(data.budget.flightKrw)}</span></li>
          <li><span>숙박</span><span>{krw(data.budget.stayKrw)}</span></li>
          <li><span>식비</span><span>{krw(data.budget.foodKrw)}</span></li>
          <li><span>활동</span><span>{krw(data.budget.activityKrw)}</span></li>
          <li className="atl-result-total"><span>합계</span><span>{krw(data.budget.totalKrw)}</span></li>
        </ul>
      </div>

      {data.tips?.length > 0 && (
        <ul className="atl-result-tips">
          {data.tips.map((t) => (
            <li key={t}>{t}</li>
          ))}
        </ul>
      )}
    </section>
  );
}

function FeatureIcon({ children }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {children}
    </svg>
  );
}

const EMPTY_FORM = {
  destination: '',
  airport: '',
  nights: 3,
  companion: '',
  budget: '',
  budgetKrw: '',
  interests: [],
  hotelArea: '',
  extra: '',
};

const nightsLabel = (n) => `${n}박 ${n + 1}일`;

/** 도시 자동완성 입력. 한글·영문·초성 어느 쪽으로 쳐도 서버가 찾아준다. */
function CityInput({ value, onChange, disabled }) {
  const [items, setItems] = useState([]);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const boxRef = useRef(null);
  // 선택으로 값이 바뀐 직후에는 다시 조회하지 않는다 (목록이 즉시 닫히도록)
  const skipFetch = useRef(false);

  useEffect(() => {
    if (skipFetch.current) {
      skipFetch.current = false;
      return;
    }
    if (!open) return;

    const timer = setTimeout(async () => {
      try {
        const res = await fetch(`/api/cities?q=${encodeURIComponent(value)}&limit=8`);
        if (res.ok) {
          setItems(await res.json());
          setActive(-1);
        }
      } catch {
        setItems([]); // 자동완성 실패가 입력 자체를 막지는 않는다
      }
    }, 150);
    return () => clearTimeout(timer);
  }, [value, open]);

  useEffect(() => {
    const onDocClick = (e) => {
      if (boxRef.current && !boxRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', onDocClick);
    return () => document.removeEventListener('mousedown', onDocClick);
  }, []);

  const pick = (city) => {
    skipFetch.current = true;
    onChange(city.city, city);
    setOpen(false);
  };

  const onKeyDown = (e) => {
    if (!open || items.length === 0) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActive((i) => (i + 1) % items.length);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActive((i) => (i - 1 + items.length) % items.length);
    } else if (e.key === 'Enter' && active >= 0) {
      e.preventDefault();
      pick(items[active]);
    } else if (e.key === 'Escape') {
      setOpen(false);
    }
  };

  return (
    <div className="atl-combo" ref={boxRef}>
      <input
        id="atl-dest"
        type="text"
        autoComplete="off"
        role="combobox"
        aria-expanded={open}
        aria-controls="atl-city-list"
        value={value}
        disabled={disabled}
        onChange={(e) => {
          onChange(e.target.value, null);
          setOpen(true);
        }}
        onFocus={() => setOpen(true)}
        onKeyDown={onKeyDown}
        placeholder="도시명을 입력하세요 (예: 도쿄, ㄷㅋ, Tokyo)"
        maxLength={40}
      />
      {open && items.length > 0 && (
        <ul className="atl-combo-list" id="atl-city-list" role="listbox">
          {items.map((c, i) => (
            <li key={`${c.city}-${c.country}`}>
              <button
                type="button"
                role="option"
                aria-selected={i === active}
                className={`atl-combo-item ${i === active ? 'is-active' : ''}`}
                onMouseEnter={() => setActive(i)}
                onClick={() => pick(c)}
              >
                <span className="atl-combo-city">{c.city}</span>
                <span className="atl-combo-meta">
                  {c.country} · {c.nameEn}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default function AiTravelLanding() {
  const [mode, setMode] = useState('free'); // free | form
  const [prompt, setPrompt] = useState('');
  const [form, setForm] = useState(EMPTY_FORM);
  const [showLongStay, setShowLongStay] = useState(false);
  const [airports, setAirports] = useState([]);
  const [customBudget, setCustomBudget] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  // 목적지가 바뀌면 그 도시의 공항 목록을 맞춰 둔다.
  // 목록에서 고른 경우와 직접 타이핑한 경우를 한 곳에서 처리한다
  useEffect(() => {
    const name = form.destination.trim();
    if (!name) {
      setAirports([]);
      return;
    }
    const timer = setTimeout(async () => {
      try {
        const res = await fetch(`/api/cities?q=${encodeURIComponent(name)}&limit=1`);
        if (!res.ok) return;
        const [top] = await res.json();
        // 정확히 일치할 때만 공항을 보여준다. "도"만 친 상태에서 도쿄 공항이 뜨면 안 된다
        setAirports(top && top.city === name ? top.airports : []);
      } catch {
        setAirports([]);
      }
    }, 200);
    return () => clearTimeout(timer);
  }, [form.destination]);

  // 자유 입력과 조건 선택이 같은 엔드포인트를 쓴다.
  // 추천 카드 클릭처럼 프롬프트가 이미 정해진 경우도 여기로 들어온다
  const requestPlan = async (payload) => {
    if (loading) return;

    setLoading(true);
    setError('');
    setResult(null);

    try {
      const res = await fetch('/api/plan', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        // 서버가 내려주는 사용자용 메시지를 그대로 노출한다
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || '결과를 만들지 못했어요. 다시 시도해 주세요.');
      }

      setResult(await res.json());
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!prompt.trim()) return;
    requestPlan({ prompt: prompt.trim() });
  };

  // 목적지를 직접 골랐으므로 서버가 추출 호출 없이 바로 생성한다
  const handleFormSubmit = (e) => {
    e.preventDefault();
    if (!form.destination.trim()) return;
    requestPlan({
      ...form,
      destination: form.destination.trim(),
      // 입력은 만원 단위로 받고 서버에는 원 단위로 보낸다
      budgetKrw: customBudget && form.budgetKrw ? Number(form.budgetKrw) * 10000 : null,
      budget: customBudget ? '' : form.budget,
    });
  };

  const toggleInterest = (interest) => {
    setForm((prev) => {
      const has = prev.interests.includes(interest);
      if (!has && prev.interests.length >= MAX_INTERESTS) return prev;
      return {
        ...prev,
        interests: has
          ? prev.interests.filter((i) => i !== interest)
          : [...prev.interests, interest],
      };
    });
  };

  // 추천 카드를 누르면 그 도시로 일정 생성을 이어간다
  const handlePickDestination = (planPrompt) => {
    setPrompt(planPrompt);
    setMode('free');
    requestPlan({ prompt: planPrompt });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="atl-page">
      <header className="atl-header">
        <div className="atl-header-inner">
          <a className="atl-logo" href="/">
            <span className="atl-logo-mark" aria-hidden="true" />
            WWW
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
          <p className="atl-eyebrow">Wherever · Whatever · Whenever</p>
          <h1 className="atl-title">
            가고 싶은 여행지를 입력하고
            <br />
            <span className="atl-title-accent">일정과 예약까지 한번에</span>
          </h1>
          <p className="atl-subtitle">
            목적지·기간·예산만 입력하면 일자별 코스와 예상 경비를 한 번에 정리해 드려요.
          </p>

          <div className="atl-tabs" role="tablist">
            <button
              type="button"
              role="tab"
              aria-selected={mode === 'free'}
              className={`atl-tab ${mode === 'free' ? 'is-active' : ''}`}
              onClick={() => setMode('free')}
            >
              자유롭게 입력
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={mode === 'form'}
              className={`atl-tab ${mode === 'form' ? 'is-active' : ''}`}
              onClick={() => setMode('form')}
            >
              조건 선택
            </button>
          </div>

          {mode === 'free' ? (
            <>
              <form className="atl-prompt-box" onSubmit={handleSubmit}>
                <textarea
                  value={prompt}
                  onChange={(e) => setPrompt(e.target.value)}
                  placeholder="떠나고 싶은 여행지, 계획을 세워보세요"
                  rows={2}
                />
                <button type="submit" className="atl-submit" aria-label="일정 만들기" disabled={loading || !prompt.trim()}>
                  {loading ? (
                    <span className="atl-spinner" aria-hidden="true" />
                  ) : (
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                      <path d="M5 12h13M12 5l7 7-7 7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  )}
                </button>
              </form>

              <div className="atl-chips">
                {QUICK_PROMPTS.map((q) => (
                  <button type="button" key={q} className="atl-chip" onClick={() => setPrompt(q)} disabled={loading}>
                    {q}
                  </button>
                ))}
              </div>
            </>
          ) : (
            <form className="atl-form" onSubmit={handleFormSubmit}>
              <div className="atl-field">
                <label htmlFor="atl-dest">어디로 가시나요?</label>
                <CityInput
                  value={form.destination}
                  onChange={(v) => setForm({ ...form, destination: v, airport: '' })}
                  disabled={loading}
                />

                {airports.length > 0 && (
                  <div className="atl-airports">
                    <span className="atl-airport-label">도착 공항</span>
                    <div className="atl-opts">
                      {airports.map((a) => (
                        <button
                          type="button"
                          key={a.code}
                          className={`atl-opt atl-opt-air ${form.airport === a.name ? 'is-on' : ''}`}
                          onClick={() =>
                            setForm({ ...form, airport: form.airport === a.name ? '' : a.name })
                          }
                          title={a.note}
                        >
                          {a.name}
                          <em>{a.note}</em>
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              <div className="atl-field">
                <span className="atl-field-label">기간</span>
                <div className="atl-opts atl-opts-fill">
                  {QUICK_NIGHTS.map((n) => (
                    <button
                      type="button"
                      key={n}
                      className={`atl-opt ${!showLongStay && form.nights === n ? 'is-on' : ''}`}
                      onClick={() => {
                        setShowLongStay(false);
                        setForm({ ...form, nights: n });
                      }}
                    >
                      {nightsLabel(n)}
                    </button>
                  ))}
                  <button
                    type="button"
                    className={`atl-opt ${showLongStay ? 'is-on' : ''}`}
                    onClick={() => {
                      const next = !showLongStay;
                      setShowLongStay(next);
                      if (next) setForm({ ...form, nights: LONG_NIGHTS[0] });
                    }}
                  >
                    일주일 이상
                  </button>
                </div>

                {showLongStay && (
                  <div className="atl-opts atl-opts-sub">
                    {LONG_NIGHTS.map((n) => (
                      <button
                        type="button"
                        key={n}
                        className={`atl-opt ${form.nights === n ? 'is-on' : ''}`}
                        onClick={() => setForm({ ...form, nights: n })}
                      >
                        {nightsLabel(n)}
                      </button>
                    ))}
                  </div>
                )}
              </div>

              <div className="atl-field">
                <span className="atl-field-label">동행</span>
                <div className="atl-opts">
                  {COMPANIONS.map((c) => (
                    <button
                      type="button"
                      key={c}
                      className={`atl-opt ${form.companion === c ? 'is-on' : ''}`}
                      onClick={() => setForm({ ...form, companion: form.companion === c ? '' : c })}
                    >
                      {c}
                    </button>
                  ))}
                </div>
              </div>

              <div className="atl-field">
                <span className="atl-field-label">예산<em>1인 기준</em></span>
                <div className="atl-opts atl-opts-budget">
                  {BUDGETS.map((b) => (
                    <button
                      type="button"
                      key={b}
                      className={`atl-opt ${!customBudget && form.budget === b ? 'is-on' : ''}`}
                      onClick={() => {
                        setCustomBudget(false);
                        setForm({ ...form, budget: form.budget === b ? '' : b, budgetKrw: '' });
                      }}
                    >
                      {b}
                    </button>
                  ))}
                  <button
                    type="button"
                    className={`atl-opt atl-opt-last ${customBudget ? 'is-on' : ''}`}
                    onClick={() => {
                      const next = !customBudget;
                      setCustomBudget(next);
                      if (next) setForm({ ...form, budget: '' });
                    }}
                  >
                    직접 입력
                  </button>
                </div>

                {customBudget && (
                  <div className="atl-budget-input">
                    <input
                      type="number"
                      min="1"
                      max="10000"
                      value={form.budgetKrw}
                      onChange={(e) => setForm({ ...form, budgetKrw: e.target.value })}
                      placeholder="80"
                      aria-label="1인 예산 (만원)"
                    />
                    <span>만원</span>
                  </div>
                )}
              </div>

              <div className="atl-field">
                <span className="atl-field-label">
                  관심사
                  <em>최대 {MAX_INTERESTS}개</em>
                </span>
                <div className="atl-opts">
                  {INTERESTS.map((i) => {
                    const on = form.interests.includes(i);
                    const full = !on && form.interests.length >= MAX_INTERESTS;
                    return (
                      <button
                        type="button"
                        key={i}
                        className={`atl-opt ${on ? 'is-on' : ''}`}
                        onClick={() => toggleInterest(i)}
                        disabled={full}
                      >
                        {i}
                      </button>
                    );
                  })}
                </div>
              </div>

              <details className="atl-more">
                <summary>숙소 위치·추가 요청 (선택)</summary>
                <div className="atl-field">
                  <label htmlFor="atl-hotel">숙소 위치</label>
                  <input
                    id="atl-hotel"
                    type="text"
                    value={form.hotelArea}
                    onChange={(e) => setForm({ ...form, hotelArea: e.target.value })}
                    placeholder="신주쿠, 시부야..."
                    maxLength={40}
                  />
                </div>
                <div className="atl-field">
                  <label htmlFor="atl-extra">추가 요청</label>
                  <input
                    id="atl-extra"
                    type="text"
                    value={form.extra}
                    onChange={(e) => setForm({ ...form, extra: e.target.value })}
                    placeholder="디즈니랜드는 꼭 가고 싶어요"
                    maxLength={300}
                  />
                </div>
              </details>

              <button
                type="submit"
                className="atl-form-submit"
                disabled={loading || !form.destination.trim()}
              >
                {loading ? '일정을 짜는 중...' : '일정 만들기'}
              </button>
            </form>
          )}

          {loading && <p className="atl-status">AI가 답을 만들고 있어요. 최대 2분 정도 걸려요.</p>}
          {error && <p className="atl-status atl-status-error" role="alert">{error}</p>}
        </section>

        {result?.type === 'ITINERARY' && <ItineraryResult data={result.itinerary} />}
        {result?.type === 'SUGGESTIONS' && (
          <SuggestionsResult data={result.suggestions} onPick={handlePickDestination} disabled={loading} />
        )}

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
              <p className="atl-footer-name">WWW</p>
              <p className="atl-footer-desc">어디로든, 무엇이든, 언제든 — AI 여행 일정 플래너</p>
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
