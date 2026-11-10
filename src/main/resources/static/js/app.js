/*
 * 꼭잡아 관제실 화면.
 * 원칙 두 가지:
 *   1) 화면은 규칙을 판단하지 않는다. "다음 단계가 뭔지"는 서버가 준 nextSteps를 그대로 따른다.
 *   2) 화면의 사건 목록은 서버의 그림자다. 내가 누른 결과든 다른 직원이 누른 결과든 같은 upsert()로 반영한다.
 */
'use strict';

const ZONES = ['정문 광장', '퍼레이드 거리', '회전목마', '푸드코트', '롤러코스터', '물놀이장', '기념품샵', '숲 산책로', '안내소 · 미아 보호소'];
const COLORS = { 노랑: '#f2c230', 빨강: '#e0473c', 초록: '#3aa35b', 분홍: '#e879a6', 하늘: '#4fa8e0', 주황: '#f08a24' };
const STEPS = [
  { label: '접수', statuses: ['DETECTED', 'REPORTED'] },
  { label: '수색 중', statuses: ['SEARCHING'] },
  { label: '발견', statuses: ['FOUND'] },
  { label: '보호 중', statuses: ['SHELTERED'] },
  { label: '인계 완료', statuses: ['HANDED_OVER'] },
];

const cases = new Map(); // id → 사건 (서버 응답 그대로)
let selectedId = null;

const $ = (sel) => document.querySelector(sel);

/* ---------- 서버 호출 ---------- */

/** 실패하면 서버가 준 ProblemDetail의 detail을 메시지로 던진다. */
async function api(method, path, body, version) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (version != null) headers['If-Match'] = `"${version}"`; // 내가 마지막으로 본 version
  const res = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const err = new Error(data?.detail || `요청에 실패했어요 (${res.status})`);
    err.status = res.status;
    err.title = data?.title;
    throw err;
  }
  return data;
}

async function reload() {
  const list = await api('GET', '/api/cases');
  cases.clear();
  list.forEach((c) => cases.set(c.id, c));
  render();
}

/** 단계 버튼 공통 처리: 성공하면 반영, 동시 수정으로 거절되면 최신 상태를 다시 받아 온다. */
async function act(id, path, body) {
  const c = cases.get(id) ?? detailOnly;
  try {
    upsert(await api('POST', `/api/cases/${id}/${path}`, body, c?.version));
  } catch (e) {
    toast(e.message);
    if (e.status === 409 && e.title === '동시 수정') {
      upsert(await api('GET', `/api/cases/${id}`));
    }
  }
}

/* ---------- 실시간 연결 (SSE) ---------- */

function connect() {
  const es = new EventSource('/api/cases/stream');

  // 처음 연결될 때와, 끊겼다가 다시 붙을 때마다 온다. 그사이 놓친 변경이 있을 수 있으니 목록을 새로 받는다.
  es.addEventListener('connected', () => {
    setConn(true);
    reload().catch((e) => toast(e.message));
  });

  es.addEventListener('opened', (e) => {
    const c = JSON.parse(e.data);
    upsert(c, true);
    log(`새 사건 #${c.id} · ${c.statusLabel} · ${c.clothingColor} 옷 · ${c.lastSeenZone}`, 'alert');
  });

  es.addEventListener('updated', (e) => {
    const c = JSON.parse(e.data);
    upsert(c, true);
    log(`#${c.id} → ${c.statusLabel}${c.assignedStaff && c.status === 'SEARCHING' ? ` (${c.assignedStaff})` : ''}`,
      c.status === 'HANDED_OVER' || c.status === 'FOUND' ? 'safe' : '');
  });

  // EventSource는 끊기면 스스로 다시 연결을 시도한다. 화면에는 상태만 알려 준다.
  es.onerror = () => setConn(false);
}

/* ---------- 상태 반영 ---------- */

let detailOnly = null; // 인계가 끝나 목록에서 빠졌지만, 지금 상세에 띄워 둔 사건

function upsert(c, fromStream = false) {
  const known = cases.get(c.id) ?? (detailOnly?.id === c.id ? detailOnly : null);
  if (known && known.version > c.version) return; // 더 오래된 소식이 늦게 도착한 경우 무시

  if (c.status === 'HANDED_OVER') {
    cases.delete(c.id);
    if (selectedId === c.id) detailOnly = c;
  } else {
    cases.set(c.id, c);
  }
  render();
  if (fromStream) flash(c.id);
}

function select(id) {
  selectedId = id;
  detailOnly = null;
  render();
}

/* ---------- 그리기 ---------- */

function render() {
  renderSummary();
  renderList();
  renderDetail();
}

function renderSummary() {
  const all = [...cases.values()];
  $('#openCount').textContent = all.length;
  $('#searchCount').textContent = all.filter((c) => c.status === 'SEARCHING').length;
  $('#shelterCount').textContent = all.filter((c) => c.status === 'SHELTERED').length;
}

function renderList() {
  const ul = $('#caseList');
  const sorted = [...cases.values()].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  $('#emptyList').hidden = sorted.length > 0;
  ul.replaceChildren(...sorted.map((c) => {
    const li = document.createElement('li');
    // 사용자가 입력한 값(옷 색, 위치, 이름)은 innerHTML에 그대로 넣지 않고 esc()로 막는다 (XSS 방지)
    li.innerHTML = `
      <button class="case" type="button" data-id="${c.id}" aria-current="${c.id === selectedId}">
        <span class="swatch" style="background:${colorOf(c.clothingColor)}"></span>
        <span class="who">#${c.id} · ${esc(c.clothingColor)} 옷</span>
        <span class="where">${esc(c.lastSeenZone)}${c.assignedStaff ? ` · ${esc(c.assignedStaff)}` : ''}</span>
        <span class="since"><span class="chip ${c.status}">${esc(c.statusLabel)}</span><span class="mono" data-since="${c.createdAt}"></span></span>
      </button>`;
    return li;
  }));
  tick();
}

function renderDetail() {
  const box = $('#detail');
  const c = cases.get(selectedId) ?? (detailOnly?.id === selectedId ? detailOnly : null);
  if (!c) {
    $('#detailSub').textContent = '';
    delete box.dataset.sig;
    box.innerHTML = '<p class="empty">왼쪽 목록에서 사건을 고르세요.</p>';
    return;
  }
  // 같은 사건·같은 version이면 다시 그리지 않는다. 입력 중인 칸이 지워지지 않게.
  const sig = `${c.id}:${c.version}`;
  if (box.dataset.sig === sig) return;
  box.dataset.sig = sig;

  $('#detailSub').textContent = `#${c.id} · version ${c.version}`;
  const nowIndex = STEPS.findIndex((s) => s.statuses.includes(c.status));
  box.innerHTML = `
    <ol class="steps">${STEPS.map((s, i) =>
      `<li class="${i < nowIndex ? 'done' : i === nowIndex ? 'now' : ''}">${i === 0 && nowIndex === 0 ? esc(c.statusLabel) : s.label}</li>`).join('')}
    </ol>
    <dl class="facts">
      <div><dt>옷 색</dt><dd><span class="swatch" style="display:inline-block;width:14px;height:14px;vertical-align:-2px;background:${colorOf(c.clothingColor)}"></span> ${esc(c.clothingColor)}</dd></div>
      <div><dt>마지막 위치</dt><dd>${esc(c.lastSeenZone)}</dd></div>
      <div><dt>담당 직원</dt><dd>${c.assignedStaff ? esc(c.assignedStaff) : '아직 없음'}</dd></div>
      <div><dt>보호자</dt><dd>${c.guardianLinked ? '신고 연결됨' : '<span style="color:var(--alert)">미연결</span>'}</dd></div>
      <div><dt>접수 후</dt><dd class="mono" data-since="${c.createdAt}"></dd></div>
    </dl>
    ${actionsFor(c)}`;
  tick();
}

/** 지금 단계에서 할 수 있는 일만 보여 준다. 판단 근거는 서버가 준 status / nextSteps / guardianLinked. */
function actionsFor(c) {
  const next = c.nextSteps[0];
  const parts = [];

  if (!c.guardianLinked && c.status !== 'HANDED_OVER') {
    parts.push(`
      <form class="action" data-act="guardian">
        <p>보호자가 안내소에 왔다면, 보호자 팔찌 번호를 이 사건에 연결하세요.</p>
        <div class="row"><input type="text" name="guardianBand" placeholder="팔찌 번호 (예: B-1137)" maxlength="20" required autocomplete="off">
        <button class="btn" type="submit">보호자 연결</button></div>
      </form>`);
  }

  if (next === 'SEARCHING') {
    parts.push(`
      <form class="action" data-act="assign">
        <p>가장 가까운 직원을 배정하면 수색이 시작돼요.</p>
        <div class="row"><input type="text" name="staffName" placeholder="직원 이름" maxlength="40" required autocomplete="off">
        <button class="btn primary" type="submit">직원 배정</button></div>
      </form>`);
  } else if (next === 'FOUND') {
    parts.push(`
      <form class="action" data-act="found">
        <p>아이를 찾았다면 발견한 위치를 고르세요.</p>
        <div class="row"><select name="zone">${zoneOptions(c.lastSeenZone)}</select>
        <button class="btn primary" type="submit">발견 처리</button></div>
      </form>`);
  } else if (next === 'SHELTERED') {
    parts.push(`
      <form class="action" data-act="shelter">
        <p>직원이 아이와 함께 안내소 · 미아 보호소에 도착하면 누르세요.</p>
        <div class="row"><button class="btn primary" type="submit">보호소 도착</button></div>
      </form>`);
  } else if (next === 'HANDED_OVER') {
    parts.push(`
      <form class="action" data-act="handover">
        <p>보호자 팔찌 번호를 직접 확인하세요. 번호가 맞아야만 인계돼요.</p>
        <div class="row"><input type="text" name="guardianBand" placeholder="보호자가 보여 준 팔찌 번호" maxlength="20" required autocomplete="off" ${c.guardianLinked ? '' : 'disabled'}>
        <button class="btn primary" type="submit" ${c.guardianLinked ? '' : 'disabled'}>인계</button></div>
      </form>`);
  } else {
    parts.push('<p class="done-msg">보호자에게 무사히 인계됐어요.</p>');
  }
  return parts.join('');
}

/* ---------- 이벤트 연결 ---------- */

$('#caseList').addEventListener('click', (e) => {
  const btn = e.target.closest('.case');
  if (btn) select(Number(btn.dataset.id));
});

// 상세 영역의 폼은 매번 새로 그려지므로, 부모 하나에 이벤트를 걸어 둔다 (이벤트 위임).
$('#detail').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.target;
  const data = Object.fromEntries(new FormData(form));
  const body = Object.keys(data).length ? data : undefined;
  await withBusy(form, () => act(selectedId, form.dataset.act, body));
});

$('#reportForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.target;
  await withBusy(form, async () => {
    try {
      const c = await api('POST', '/api/cases/reports', Object.fromEntries(new FormData(form)));
      upsert(c);
      select(c.id);
      form.guardianBand.value = '';
    } catch (err) { toast(err.message); }
  });
});

$('#detectForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.target;
  await withBusy(form, async () => {
    try {
      upsert(await api('POST', '/api/cases/detections', Object.fromEntries(new FormData(form))));
    } catch (err) { toast(err.message); }
  });
});

/** 요청 중에는 버튼을 잠가서 두 번 눌림을 막는다. */
async function withBusy(form, fn) {
  const buttons = form.querySelectorAll('button');
  buttons.forEach((b) => (b.disabled = true));
  try { await fn(); } finally { buttons.forEach((b) => (b.disabled = false)); }
}

/* ---------- 작은 도구들 ---------- */

function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, (ch) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
}
function colorOf(name) { return COLORS[name] ?? '#9aa5a1'; }
function zoneOptions(selected) {
  return ZONES.map((z) => `<option${z === selected ? ' selected' : ''}>${esc(z)}</option>`).join('');
}

function setConn(live) {
  const el = $('#conn');
  el.className = `conn ${live ? 'live' : 'wait'}`;
  el.textContent = live ? '실시간 연결됨' : '다시 연결하는 중…';
}

function log(text, cls = '') {
  const li = document.createElement('li');
  li.innerHTML = `<time>${hhmmss(new Date())}</time><span class="${cls}">${esc(text)}</span>`;
  $('#log').prepend(li);
  while ($('#log').children.length > 50) $('#log').lastChild.remove();
}

function flash(id) {
  document.querySelector(`.case[data-id="${id}"]`)?.classList.add('flash');
}

let toastTimer;
function toast(msg) {
  const t = $('#toast');
  t.textContent = msg;
  t.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (t.hidden = true), 4000);
}

const pad = (n) => String(n).padStart(2, '0');
function hhmmss(d) { return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`; }

/** 시계와 "접수 후 몇 분" 표시를 1초마다 갱신. 서버 시각(Asia/Seoul)과 브라우저 시각이 같은 시간대라고 본다. */
function tick() {
  $('#clock').textContent = hhmmss(new Date());
  document.querySelectorAll('[data-since]').forEach((el) => {
    const sec = Math.max(0, Math.floor((Date.now() - new Date(el.dataset.since)) / 1000));
    el.textContent = sec < 60 ? `${sec}초` : `${Math.floor(sec / 60)}분 ${pad(sec % 60)}초`;
  });
}

/* ---------- 시작 ---------- */

document.querySelectorAll('[data-colors]').forEach((s) => {
  s.innerHTML = Object.keys(COLORS).map((c) => `<option>${c}</option>`).join('');
});
document.querySelectorAll('[data-zones]').forEach((s) => { s.innerHTML = zoneOptions(); });
setInterval(tick, 1000);
tick();
connect();
