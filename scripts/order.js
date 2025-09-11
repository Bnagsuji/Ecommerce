import http from 'k6/http';
import { check, sleep } from 'k6';

// ===== 환경 변수 =====
const MODE           = (__ENV.MODE || 'load').toLowerCase();     // load | endurance | stress(옵션)
const BASE_URL       = __ENV.BASE_URL || 'http://host.docker.internal:8080';

const USER_START     = Number(__ENV.USER_START || '1');
const USER_COUNT     = Number(__ENV.USER_COUNT || '2000');

const INITIAL_CHARGE = Number(__ENV.INITIAL_CHARGE || '50000');
const MAX_QTY        = Number(__ENV.MAX_QTY || '2');

const ORDER_RPS      = Number(__ENV.ORDER_RPS || '40');          // load/stress 기본 목표 RPS
const DURATION       = __ENV.DURATION || '1m';                   // load/stress 기본 기간

// endurance 전용 파라미터
const SOAK_RPS       = Number(__ENV.SOAK_RPS || '10');           // 내구 테스트 RPS
const SOAK_DURATION  = __ENV.SOAK_DURATION || '2h';              // 내구 테스트 기간
const TOPUP_RPS      = Number(__ENV.TOPUP_RPS || '1');           // 분산 충전 RPS

// 선택: 포인트 충전/캐시워밍 on/off
const SEED_BALANCE   = (__ENV.SEED_BALANCE || 'true').toLowerCase() === 'true';
const WARM_CACHE     = (__ENV.WARM_CACHE || 'true').toLowerCase() === 'true';

// top-selling이 비면 사용할 백업 상품
const FALLBACK_PRODUCT_IDS = (__ENV.FALLBACK_PRODUCT_IDS || '101,102,103')
    .split(',').map(s => Number(s.trim())).filter(Boolean);

// ===== 공통 옵션/임계치 =====
function thresholds() {
    return {
        http_req_failed: ['rate<0.02'],
        'http_req_duration{endpoint:top}':    ['p(95)<300'],
        'http_req_duration{endpoint:detail}': ['p(95)<300'],
        'http_req_duration{endpoint:order}':  ['p(95)<800'],
        'http_req_duration{endpoint:charge}': ['p(95)<800'],
        'checks{endpoint:top}':    ['rate>0.98'],
        'checks{endpoint:detail}': ['rate>0.98'],
        'checks{endpoint:order}':  ['rate>0.90'],
        'checks{endpoint:charge}': ['rate>0.95'],
    };
}

function buildOptions() {
    const base = {
        discardResponseBodies: true,   // 장기/고RPS 시 메모리 사용 줄이기
        thresholds: thresholds(),
    };

    if (MODE === 'endurance') {
        return {
            ...base,
            scenarios: {
                ...(WARM_CACHE ? {
                    warm_top_once: {               // 캐시 워밍: 짧게 top-selling만 두드림
                        executor: 'constant-arrival-rate',
                        rate: 50, timeUnit: '1s', duration: '10s',
                        preAllocatedVUs: 20, maxVUs: 50, exec: 'scenarioWarmTop',
                    },
                } : {}),
                ...(SEED_BALANCE ? {
                    seed_balance_once: {           // 초반 시드 충전(짧게)
                        executor: 'constant-arrival-rate',
                        rate: 50, timeUnit: '1s', duration: '20s',
                        preAllocatedVUs: 600, maxVUs: 1200, exec: 'scenarioSeedBalance',
                        startTime: WARM_CACHE ? '10s' : '0s',
                    },
                } : {}),
                periodic_topup: {                // 장기간 동안 계좌 잔액 보충
                    executor: 'constant-arrival-rate',
                    rate: TOPUP_RPS, timeUnit: '1s', duration: SOAK_DURATION,
                    preAllocatedVUs: Math.min(TOPUP_RPS * 2, 50),
                    maxVUs: Math.min(TOPUP_RPS * 5, 100),
                    exec: 'scenarioPeriodicTopup',
                    startTime: SEED_BALANCE ? '20s' : (WARM_CACHE ? '10s' : '0s'),
                },
                hot_browse_and_order: {          // 본 테스트(안정 RPS로 오래)
                    executor: 'constant-arrival-rate',
                    rate: SOAK_RPS, timeUnit: '1s', duration: SOAK_DURATION,
                    startTime: SEED_BALANCE ? '20s' : (WARM_CACHE ? '10s' : '0s'),
                    preAllocatedVUs: Math.min(SOAK_RPS * 4, 300),
                    maxVUs: Math.min(SOAK_RPS * 10, 800),
                    exec: 'scenarioHotBrowseAndOrder',
                    gracefulStop: '30s',
                },
            },
        };
    }

    if (MODE === 'load') {
        return {
            ...base,
            scenarios: {
                ...(WARM_CACHE ? {
                    warm_top_once: {
                        executor: 'constant-arrival-rate',
                        rate: 50, timeUnit: '1s', duration: '10s',
                        preAllocatedVUs: 20, maxVUs: 50, exec: 'scenarioWarmTop',
                    },
                } : {}),
                ...(SEED_BALANCE ? {
                    seed_balance_once: {
                        executor: 'constant-arrival-rate',
                        rate: 200, timeUnit: '1s', duration: '8s',
                        preAllocatedVUs: 120, maxVUs: 300, exec: 'scenarioSeedBalance',
                        startTime: WARM_CACHE ? '10s' : '0s',
                    },
                } : {}),
                // 워밍업 → 피크 유지 → 램프다운
                load_ramp_plateau: {
                    executor: 'ramping-arrival-rate',
                    preAllocatedVUs: Math.min(ORDER_RPS * 4, 400),
                    maxVUs: Math.min(ORDER_RPS * 10, 1200),
                    timeUnit: '1s',
                    startTime: SEED_BALANCE ? '12s' : (WARM_CACHE ? '10s' : '0s'),
                    stages: [
                        { target: Math.max(5, Math.floor(ORDER_RPS * 0.3)), duration: '1m' },
                        { target: Math.max(10, Math.floor(ORDER_RPS * 0.6)), duration: '1m' },
                        { target: ORDER_RPS, duration: DURATION },          // 평지 구간
                        { target: 0, duration: '30s' },                     // 램프다운
                    ],
                    exec: 'scenarioHotBrowseAndOrder',
                    gracefulStop: '30s',
                },
            },
        };
    }

    // 향후 테스트
    return {
        ...base,
        scenarios: {
            step_up: {
                executor: 'ramping-arrival-rate',
                preAllocatedVUs: 200, maxVUs: 2000, timeUnit: '1s',
                stages: [
                    { target: 20, duration: '1m' },
                    { target: 50, duration: '2m' },
                    { target: 100, duration: '2m' },
                    { target: 150, duration: '2m' },
                    { target: 200, duration: '2m' },
                    { target: 0, duration: '30s' },
                ],
                exec: 'scenarioHotBrowseAndOrder',
                gracefulStop: '30s',
            },
        },
    };
}

export const options = buildOptions();

// ===== 유틸 =====
function jsonHeaders(tags = {}) {
    return { headers: { 'Content-Type': 'application/json' }, tags };
}
const rnd = (n) => Math.floor(Math.random() * n);
function pickUserId() { return USER_START + rnd(USER_COUNT); }
function pickQty()    { return Math.max(1, rnd(MAX_QTY) + 1); }
function pickOne(arr) { return arr[rnd(arr.length)]; }

// ===== API =====
function getTopSelling() {
    const res = http.get(`${BASE_URL}/api/product/top-selling`, { tags: { endpoint: 'top', method: 'GET' } });
    check(res, { 'top 200': r => r.status === 200 }, { endpoint: 'top' });
    if (res.status !== 200) return [];
    try { const list = res.json(); return Array.isArray(list) ? list : []; } catch (_) { return []; }
}
function getProductDetail(id) {
    const res = http.get(`${BASE_URL}/api/product/${id}`, { tags: { endpoint: 'detail', method: 'GET' } });
    check(res, { 'detail 200': r => r.status === 200 }, { endpoint: 'detail' });
    return res;
}
function chargeBalance(userId, amount) {
    const res = http.post(`${BASE_URL}/api/balance/charge`, JSON.stringify({ userId, amount }),
        jsonHeaders({ endpoint: 'charge', method: 'POST' }));
    check(res, { 'charge 200': r => r.status === 200 }, { endpoint: 'charge' });
    return res;
}
function placeOrder(userId, items) {
    const res = http.post(`${BASE_URL}/api/orders`, JSON.stringify({ userId, items }),
        jsonHeaders({ endpoint: 'order', method: 'POST' }));
    check(res, { 'order 200|400': r => r.status === 200 || r.status === 400 }, { endpoint: 'order' });
    return res;
}

// ===== setup & 시나리오 =====
export function setup() {
    let topList = getTopSelling();
    if (!topList || topList.length === 0) {
        topList = FALLBACK_PRODUCT_IDS.map(id => ({ id }));
    }
    return { topIds: topList.map(p => p.id).filter(Boolean) };
}

export function scenarioWarmTop() {
    getTopSelling();
    sleep(0.02);
}

export function scenarioSeedBalance() {
    const userId = pickUserId();
    chargeBalance(userId, INITIAL_CHARGE);
    sleep(0.01 + Math.random() * 0.03);
}

export function scenarioPeriodicTopup() {
    const userId = pickUserId();
    // 내구 테스트 동안 잔액이 바닥나지 않도록 소액 충전
    chargeBalance(userId, Math.max(10000, Math.floor(INITIAL_CHARGE / 5)));
    sleep(0.5 + Math.random() * 0.5);
}

export function scenarioHotBrowseAndOrder(data) {
    const topIds = (data && data.topIds && data.topIds.length) ? data.topIds : FALLBACK_PRODUCT_IDS;
    const pid = pickOne(topIds);
    getProductDetail(pid);
    placeOrder(pickUserId(), [{ productId: pid, quantity: pickQty() }]);
    sleep(0.02 + Math.random() * 0.05);
}
