/*
 * 네이버 지도 (Maps JavaScript API v3).
 * 실제 지도 위에 구역 다각형, 캠 위치, 사건 핀을 얹는다. map-plain.js와 같은 { setZones, render } 모양을 돌려준다.
 * 문서: https://navermaps.github.io/maps.js.ncp/
 */
'use strict';

window.KkokMap = window.KkokMap || {};

/** 스크립트를 필요할 때만 불러온다. 키가 없는 환경에서는 네이버 서버에 요청 자체를 보내지 않는다. */
function loadNaverMaps(keyId, onAuthFailure) {
  // 키가 틀렸거나 등록되지 않은 주소에서 열면 네이버가 이 전역 함수를 부른다(문서에 정해진 이름).
  window.navermap_authFailure = onAuthFailure;
  if (window.naver?.maps) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${encodeURIComponent(keyId)}`;
    // 인증에 실패하면 스크립트는 받아져도 naver.maps가 준비되지 않는다. 그때는 여기서 실패로 처리한다.
    s.onload = () => (window.naver?.maps ? resolve() : reject(new Error('네이버 지도 인증에 실패했어요.')));
    s.onerror = () => reject(new Error('네이버 지도 스크립트를 불러오지 못했어요.'));
    document.head.appendChild(s);
  });
}

window.KkokMap.createNaver = async function createNaver(container, keyId, opts) {
  await loadNaverMaps(keyId, opts.onAuthFailure);
  const N = window.naver.maps;
  const css = (name) => getComputedStyle(document.documentElement).getPropertyValue(name).trim();

  container.replaceChildren();
  const el = document.createElement('div');
  el.className = 'naver-map';
  container.appendChild(el);

  const map = new N.Map(el, {
    center: new N.LatLng(37.527, 126.934),
    zoom: 16,
    scaleControl: true,
    mapDataControl: false,
    zoomControl: true,
    zoomControlOptions: { position: N.Position.TOP_RIGHT, style: N.ZoomControlStyle.SMALL },
  });

  const zoneEls = new Map(); // 이름 → { polygon, label, center, zone, count }
  const pins = new Map(); // 사건 id → { marker, html, at }
  const camMarkers = [];

  // 멀리서 볼 때는 구역이 작아 글자와 아이콘이 겹친다. 확대 수준에 따라 보여 줄 것을 줄인다.
  //   - 캠 아이콘: 가까이(17 이상)에서만
  //   - 구역 이름표: 가까이에서는 전부, 멀리서는 사건이 있는 구역과 보호소만
  const DETAIL_ZOOM = 17;
  function applyZoomDetail() {
    const near = map.getZoom() >= DETAIL_ZOOM;
    camMarkers.forEach((m) => m.setVisible(near));
    for (const z of zoneEls.values()) z.label.setVisible(near || z.zone.shelter || z.count > 0);
  }
  N.Event.addListener(map, 'zoom_changed', applyZoomDetail);

  // 마커 내용은 우리 HTML이다. 기준점(anchor)을 왼쪽 위(0,0)에 두고 CSS transform으로 가운데를 맞춘다.
  const icon = (html) => ({ content: html, anchor: new N.Point(0, 0) });

  function setZones(zones) {
    let minLat = 90, maxLat = -90, minLng = 180, maxLng = -180;
    for (const z of zones) {
      const path = z.boundary.map((p) => new N.LatLng(p.lat, p.lng));
      const lats = z.boundary.map((p) => p.lat), lngs = z.boundary.map((p) => p.lng);
      minLat = Math.min(minLat, ...lats); maxLat = Math.max(maxLat, ...lats);
      minLng = Math.min(minLng, ...lngs); maxLng = Math.max(maxLng, ...lngs);

      const polygon = new N.Polygon({
        map, paths: [path], clickable: false,
        strokeWeight: 2, strokeStyle: z.hasCamera ? 'solid' : 'shortdash',
        ...zoneColors(z, false),
      });
      const top = new N.LatLng(Math.max(...lats), (Math.min(...lngs) + Math.max(...lngs)) / 2);
      const label = new N.Marker({ map, position: top, clickable: false, zIndex: 50, icon: icon(labelHtml(z, 0, false)) });
      const center = new N.LatLng((Math.min(...lats) + Math.max(...lats)) / 2, (Math.min(...lngs) + Math.max(...lngs)) / 2);
      for (const cam of z.cameras) {
        camMarkers.push(new N.Marker({ map, position: new N.LatLng(cam.lat, cam.lng), clickable: false, zIndex: 40,
          title: cam.code, icon: icon(`<div class="ncam" title="${esc(cam.code)}">📷<span>${esc(cam.code)}</span></div>`) }));
      }
      zoneEls.set(z.name, { polygon, label, center, zone: z, labelHtml: '', count: 0 });
    }
    // 구역 전체가 보이도록 화면을 맞춘다. 위쪽은 이름표가 들어갈 자리를 조금 더 둔다.
    const padLat = (maxLat - minLat) * 0.06, padLng = (maxLng - minLng) * 0.04;
    map.fitBounds(new N.LatLngBounds(new N.LatLng(minLat - padLat, minLng - padLng), new N.LatLng(maxLat + padLat * 2, maxLng + padLng)));
    applyZoomDetail();
  }

  function zoneColors(z, active) {
    if (active) return { strokeColor: css('--alert'), fillColor: css('--alert'), fillOpacity: 0.18 };
    if (z.shelter) return { strokeColor: css('--accent'), fillColor: css('--accent'), fillOpacity: 0.22 };
    return { strokeColor: '#5a6863', fillColor: '#ffffff', fillOpacity: z.hasCamera ? 0.35 : 0.15 };
  }

  function labelHtml(z, count, active) {
    const badge = count ? `<b class="${active ? 'alert' : 'safe'}">${count}건</b>` : '';
    return `<div class="nlabel${z.shelter ? ' shelter' : ''}">${esc(z.name)}${z.hasCamera ? '' : ' · 캠 없음'}${badge}</div>`;
  }

  function render(byZone, selectedId) {
    const seen = new Set();
    for (const [name, list] of byZone) {
      const z = zoneEls.get(name);
      const active = list.some((c) => opts.toneOf(c.status) !== 'safe');
      z.polygon.setOptions(zoneColors(z.zone, active));
      const lh = labelHtml(z.zone, list.length, active);
      if (lh !== z.labelHtml) { z.label.setIcon(icon(lh)); z.labelHtml = lh; }
      z.count = list.length;

      list.forEach((c, i) => {
        // 같은 구역 핀은 같은 지점에 두고, 화면에서 몇 픽셀씩 비켜 그린다(확대·축소해도 간격 유지)
        const perRow = Math.min(list.length, 3);
        const dx = ((i % 3) - (perRow - 1) / 2) * 38;
        const dy = Math.floor(i / 3) * 38;
        const html = `<div class="npin ${opts.toneOf(c.status)}${c.id === selectedId ? ' selected' : ''}"
            style="--dx:${dx}px;--dy:${dy}px" tabindex="0" role="button" data-id="${c.id}"
            aria-label="#${c.id} ${esc(c.clothingColor)} 옷, ${esc(c.statusLabel)}, ${esc(name)}">
            <span>#${c.id}</span><i style="background:${opts.colorOf(c.clothingColor)}"></i></div>`;
        seen.add(c.id);

        let p = pins.get(c.id);
        if (!p) {
          const marker = new N.Marker({ map, position: z.center, zIndex: 100, icon: icon(html) });
          N.Event.addListener(marker, 'click', () => opts.onSelect(c.id));
          p = { marker, html, at: z.center };
          pins.set(c.id, p);
        } else if (p.html !== html) {
          p.marker.setIcon(icon(html));
          p.html = html;
        }
        if (!p.at.equals(z.center)) { slide(p.marker, p.at, z.center); p.at = z.center; }
      });
    }
    for (const [id, p] of pins) {
      if (!seen.has(id)) { p.marker.setMap(null); pins.delete(id); }
    }
    applyZoomDetail(); // 사건이 생기거나 끝나면 이름표를 보일 구역도 바뀐다
  }

  /** 네이버 마커는 위치를 바꾸면 순간 이동한다. 0.9초 동안 조금씩 옮겨 이동이 눈에 보이게 한다. */
  function slide(marker, from, to) {
    if (matchMedia('(prefers-reduced-motion: reduce)').matches) { marker.setPosition(to); return; }
    const start = performance.now(), D = 900;
    const ease = (t) => 1 - Math.pow(1 - t, 3);
    const step = (now) => {
      const t = Math.min(1, (now - start) / D), e = ease(t);
      marker.setPosition(new N.LatLng(from.lat() + (to.lat() - from.lat()) * e, from.lng() + (to.lng() - from.lng()) * e));
      if (t < 1) requestAnimationFrame(step);
    };
    requestAnimationFrame(step);
  }

  // 키보드로 핀 고르기: 마커 내용도 페이지의 DOM이라 이벤트가 위로 올라온다
  el.addEventListener('keydown', (e) => {
    const pin = e.target.closest?.('.npin');
    if (pin && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); opts.onSelect(Number(pin.dataset.id)); }
  });

  function esc(s) {
    return String(s ?? '').replace(/[&<>"']/g, (ch) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
  }

  return { kind: '네이버 지도', setZones, render };
};
