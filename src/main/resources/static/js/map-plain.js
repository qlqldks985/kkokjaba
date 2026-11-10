/*
 * 간이 지도 (네이버 지도 키가 없을 때).
 * 서버가 준 구역 경계(위도·경도)를 화면 좌표로 바꿔 SVG로 그린다. 배경 지도는 없지만 배치와 거리 비율은 실제와 같다.
 *
 * 지도 구현은 두 가지(간이 / 네이버)이고 둘 다 같은 모양의 객체를 돌려준다:
 *   { kind, setZones(zones), render(byZone, selectedId) }
 * app.js는 어떤 지도인지 모른 채 이 두 메서드만 부른다.
 */
'use strict';

window.KkokMap = window.KkokMap || {};

window.KkokMap.createPlain = function createPlain(container, opts) {
  const NS = 'http://www.w3.org/2000/svg';
  const svg = (tag, attrs = {}, parent) => {
    const el = document.createElementNS(NS, tag);
    for (const [k, v] of Object.entries(attrs)) el.setAttribute(k, v);
    if (parent) parent.appendChild(el);
    return el;
  };

  container.replaceChildren();
  const root = svg('svg', { class: 'map', role: 'group', 'aria-label': '행사장 구역별 진행 중인 사건' }, container);
  const zoneEls = new Map(); // 구역 이름 → { poly, count, center }
  const pinEls = new Map(); // 사건 id → 핀 <g>. 재사용해야 위치 변화가 애니메이션으로 보인다.
  let pinLayer;

  function setZones(zones) {
    // 위도·경도 → 화면 좌표. 경도 1도의 실제 거리는 위도에 따라 줄어드므로 cos(위도)를 곱해 비율을 맞춘다.
    const pts = zones.flatMap((z) => z.boundary);
    const minLat = Math.min(...pts.map((p) => p.lat)), maxLat = Math.max(...pts.map((p) => p.lat));
    const minLng = Math.min(...pts.map((p) => p.lng)), maxLng = Math.max(...pts.map((p) => p.lng));
    const k = Math.cos(((minLat + maxLat) / 2) * Math.PI / 180);
    const W = 1000, PAD = 70;
    const scale = (W - 2 * PAD) / ((maxLng - minLng) * k);
    const H = Math.round((maxLat - minLat) * scale + 2 * PAD);
    const xy = (p) => [PAD + (p.lng - minLng) * k * scale, PAD + (maxLat - p.lat) * scale];
    root.setAttribute('viewBox', `0 0 ${W} ${H}`);

    const defs = svg('defs', {}, root);
    const hatch = svg('pattern', { id: 'hatch', width: 12, height: 12, patternUnits: 'userSpaceOnUse', patternTransform: 'rotate(45)' }, defs);
    svg('rect', { width: 12, height: 12, fill: 'var(--surface)' }, hatch);
    svg('line', { x1: 0, y1: 0, x2: 0, y2: 12, class: 'hatch-line' }, hatch);
    svg('text', { x: W - 20, y: 34, class: 'zsub', 'text-anchor': 'end' }, root).textContent = '↑ 북 · 한강 쪽';

    for (const z of zones) {
      const corners = z.boundary.map(xy);
      const xs = corners.map((c) => c[0]), ys = corners.map((c) => c[1]);
      const center = [(Math.min(...xs) + Math.max(...xs)) / 2, (Math.min(...ys) + Math.max(...ys)) / 2];
      const g = svg('g', {}, root);
      const poly = svg('polygon', {
        points: corners.map((c) => c.join(',')).join(' '),
        class: `zone${z.shelter ? ' shelter' : ''}${z.hasCamera ? '' : ' blind'}`,
      }, g);
      svg('text', { x: center[0], y: Math.min(...ys) - 22, class: 'zname', 'text-anchor': 'middle' }, g).textContent = z.name;
      svg('text', { x: center[0], y: Math.min(...ys) - 7, class: 'zsub', 'text-anchor': 'middle' }, g).textContent =
        z.hasCamera ? `📷 ${z.cameras.map((c) => c.code).join(' · ')}` : '캠 없음 · 순찰 필요';
      const count = svg('text', { x: Math.max(...xs) - 6, y: Math.max(...ys) - 8, class: 'zcount' }, g);
      zoneEls.set(z.name, { poly, count, center });
    }
    pinLayer = svg('g', {}, root); // 핀은 구역 위에 그려야 하므로 마지막에 둔다
  }

  function render(byZone, selectedId) {
    const seen = new Set();
    for (const [name, list] of byZone) {
      const z = zoneEls.get(name);
      z.poly.classList.toggle('active', list.some((c) => opts.toneOf(c.status) !== 'safe'));
      z.count.textContent = list.length ? `${list.length}건` : '';

      list.forEach((c, i) => {
        // 한 구역에 여러 명이면 구역 가운데를 기준으로 3열로 나눠 놓는다
        const perRow = Math.min(list.length, 3);
        const x = z.center[0] + ((i % 3) - (perRow - 1) / 2) * 36;
        const y = z.center[1] + 6 + Math.floor(i / 3) * 36;
        seen.add(c.id);

        let pin = pinEls.get(c.id);
        if (!pin) {
          pin = svg('g', { class: 'pin new', tabindex: 0, role: 'button', 'data-id': c.id }, pinLayer);
          svg('circle', { r: 16, class: 'halo' }, pin);
          svg('circle', { r: 11, class: 'body' }, pin);
          svg('text', { y: -21, class: 'pid' }, pin).textContent = `#${c.id}`;
          pin.style.transform = `translate(${x}px, ${y}px)`; // 새 핀은 미끄러지지 않고 제자리에 나타난다
          pinEls.set(c.id, pin);
        }
        pin.setAttribute('class', `pin ${opts.toneOf(c.status)}${c.id === selectedId ? ' selected' : ''}${pin.classList.contains('new') ? ' new' : ''}`);
        pin.setAttribute('aria-label', `#${c.id} ${c.clothingColor} 옷, ${c.statusLabel}, ${name}`);
        pin.querySelector('.body').setAttribute('fill', opts.colorOf(c.clothingColor));
        pin.style.transform = `translate(${x}px, ${y}px)`;
      });
    }
    for (const [id, pin] of pinEls) {
      if (!seen.has(id)) { pin.remove(); pinEls.delete(id); }
    }
  }

  // 핀 선택: 마우스 클릭과 키보드(Enter·Space) 둘 다
  root.addEventListener('click', (e) => {
    const pin = e.target.closest('.pin');
    if (pin) opts.onSelect(Number(pin.dataset.id));
  });
  root.addEventListener('keydown', (e) => {
    const pin = e.target.closest('.pin');
    if (pin && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); opts.onSelect(Number(pin.dataset.id)); }
  });
  root.addEventListener('animationend', (e) => e.target.classList?.remove('new'));

  return { kind: '간이 지도', setZones, render };
};
