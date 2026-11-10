// Pruebas de rendimiento de los RNF-01 a RNF-05 (Plan de Gestión de la Calidad v3.0) con k6.
// Corren en la integración continua (.github/workflows/rendimiento.yml) contra un ambiente de pruebas
// levantado en el mismo job: Oracle Free con kinet_bd.sql y la API Spring Boot. Nunca contra producción.
//
// Cada escenario mide un requisito; los umbrales (thresholds) son los valores del Plan de Calidad.
// El resumen queda en resultados/rnf.json, resultados/rnf.md y en el resumen del job de GitHub Actions.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const API = __ENV.API || 'http://localhost:8080/api';
const USUARIOS = Number(__ENV.USUARIOS_RNF02 || 50);      // jugadores propios para RNF-02 y RNF-03
const VU_LOGIN = Number(__ENV.VU_LOGIN || 500);           // RNF-01: 500 usuarios concurrentes
const VU_RANKING = Number(__ENV.VU_RANKING || 1000);      // RNF-04: lo que soporta el runner (ver Plan de Calidad)
const VU_PICO = Number(__ENV.VU_PICO || 500);             // RNF-05: pico de apertura de temporada

const tLogin = new Trend('rnf01_login', true);
const tFinalizar = new Trend('rnf02_finalizar_entrenamiento', true);
const tMatch = new Trend('rnf03_solicitud_matchmaking', true);
const tRanking = new Trend('rnf04_ranking', true);
const tPico = new Trend('rnf05_pico', true);
const errores = new Rate('errores');

const JSON_H = { headers: { 'Content-Type': 'application/json' } };
const auth = (t) => ({ headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${t}` } });

export const options = {
  setupTimeout: '5m',
  scenarios: {
    // RNF-01: 500 usuarios concurrentes inician sesión y siguen navegando (Inicio y ranking).
    rnf01_login: {
      executor: 'ramping-vus', exec: 'login', startTime: '0s',
      stages: [{ duration: '60s', target: VU_LOGIN }, { duration: '60s', target: VU_LOGIN }, { duration: '10s', target: 0 }],
    },
    // RNF-02: cada jugador inicia y finaliza un entrenamiento (validación anti-trampa, XP, racha, retos y logros).
    rnf02_finalizar: { executor: 'per-vu-iterations', exec: 'finalizar', vus: USUARIOS, iterations: 3, startTime: '140s', maxDuration: '2m' },
    // RNF-03: cada jugador pide un enfrentamiento (colocación o emparejamiento).
    rnf03_matchmaking: { executor: 'per-vu-iterations', exec: 'matchmaking', vus: USUARIOS, iterations: 1, startTime: '270s', maxDuration: '1m' },
    // RNF-04: muchos usuarios consultan el ranking a la vez.
    rnf04_ranking: {
      executor: 'ramping-vus', exec: 'ranking', startTime: '340s',
      stages: [{ duration: '60s', target: VU_RANKING }, { duration: '60s', target: VU_RANKING }, { duration: '10s', target: 0 }],
    },
    // RNF-05: pico brusco (de 0 a 500 usuarios en 10 s), como la apertura de una temporada.
    rnf05_pico: {
      executor: 'ramping-vus', exec: 'pico', startTime: '480s',
      stages: [{ duration: '10s', target: VU_PICO }, { duration: '40s', target: VU_PICO }, { duration: '10s', target: 0 }],
    },
  },
  thresholds: {
    'rnf01_login': ['p(95)<=2000'],
    'rnf02_finalizar_entrenamiento': ['p(95)<=3000'],
    'rnf03_solicitud_matchmaking': ['p(95)<=2000'],
    'rnf04_ranking': ['p(95)<=2000'],
    'rnf05_pico': ['p(95)<=2000'],
    'errores': ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max', 'count'],
};

export function setup() {
  // Jugadores propios para RNF-02 y RNF-03: un cronómetro por jugador, así que cada usuario virtual tiene su cuenta.
  const sufijo = Date.now().toString(36);
  const tokens = [];
  for (let i = 0; i < USUARIOS; i++) {
    const r = http.post(`${API}/auth/registro`, JSON.stringify({
      nombres: 'Carga', apellidos: `Prueba ${i}`, correo: `carga${i}_${sufijo}@prueba.kinet`,
      contrasena: 'Carga2026x', gamertag: `carga_${sufijo}_${i}`.slice(0, 30),
      fechaNacimiento: '2000-01-15', aceptaPolitica: true,
    }), JSON_H);
    check(r, { 'registro 201/200': (x) => x.status === 200 || x.status === 201 });
    tokens.push(r.json('respuesta.token'));
  }
  const act = http.get(`${API}/catalogos/actividades`, auth(tokens[0])).json('respuesta');
  return { tokens, idActividad: act[0].idTipoActividad ?? act[0].id };
}

export function login() {
  const r = http.post(`${API}/auth/login`, JSON.stringify({ correo: 'laura@kinet.com', contrasena: 'Kinet2026' }), JSON_H);
  tLogin.add(r.timings.duration);
  const ok = check(r, { 'login 200': (x) => x.status === 200 });
  errores.add(!ok);
  if (!ok) return sleep(1);
  const t = r.json('respuesta.token');
  for (let i = 0; i < 3; i++) {
    sleep(3 + Math.random() * 3);
    const p = http.get(`${API}/progreso`, auth(t));
    errores.add(p.status !== 200);
  }
}

export function finalizar(data) {
  const t = data.tokens[(__VU - 1) % data.tokens.length];
  const ini = http.post(`${API}/entrenamientos/en-curso`, JSON.stringify({ idTipoActividad: data.idActividad }), auth(t));
  if (ini.status !== 200 && ini.status !== 201) { errores.add(true); return; }
  sleep(1);
  const fin = http.post(`${API}/entrenamientos/en-curso/fin`, JSON.stringify({ intensidad: 'MEDIA' }), auth(t));
  tFinalizar.add(fin.timings.duration);
  // Una sesión de segundos se rechaza por duración mínima: igual recorre toda la validación, que es lo que se mide.
  const ok = check(fin, { 'finalizar 200': (x) => x.status === 200 });
  errores.add(!ok);
}

export function matchmaking(data) {
  const t = data.tokens[(__VU - 1) % data.tokens.length];
  const r = http.post(`${API}/matchmaking/solicitudes`, null, auth(t));
  tMatch.add(r.timings.duration);
  const ok = check(r, { 'solicitud 200/201': (x) => x.status === 200 || x.status === 201 });
  errores.add(!ok);
}

export function ranking(data) {
  const t = data.tokens[(__VU - 1) % data.tokens.length];
  const r = http.get(`${API}/temporadas/activa/ranking`, auth(t));
  tRanking.add(r.timings.duration);
  errores.add(r.status !== 200);
  sleep(1 + Math.random());
}

export function pico(data) {
  const t = data.tokens[(__VU - 1) % data.tokens.length];
  const urls = ['/temporadas/activa', '/temporadas/activa/ranking', '/progreso', '/retos'];
  const r = http.get(`${API}${urls[__ITER % urls.length]}`, auth(t));
  tPico.add(r.timings.duration);
  errores.add(r.status !== 200);
  sleep(1);
}

const REQ = [
  ['RNF-01', 'Inicio de sesión con 500 usuarios concurrentes', 'rnf01_login', 2000],
  ['RNF-02', 'Finalizar y validar un entrenamiento', 'rnf02_finalizar_entrenamiento', 3000],
  ['RNF-03', 'Solicitud de enfrentamiento (matchmaking)', 'rnf03_solicitud_matchmaking', 2000],
  ['RNF-04', 'Consulta del ranking con carga', 'rnf04_ranking', 2000],
  ['RNF-05', 'Pico de 0 a 500 usuarios en 10 s', 'rnf05_pico', 2000],
];

export function handleSummary(d) {
  const f = (v) => (v === undefined ? '—' : `${Math.round(v)} ms`);
  let md = `## Pruebas de rendimiento (k6) · ${new Date().toISOString().slice(0, 16).replace('T', ' ')} UTC\n\n`;
  md += `Ambiente de pruebas en GitHub Actions (Oracle Free + API). Usuarios: login ${VU_LOGIN}, ranking ${VU_RANKING}, pico ${VU_PICO}.\n\n`;
  md += '| RNF | Qué se mide | Peticiones | p50 | p95 | Máximo | Meta p95 | Resultado |\n|---|---|---|---|---|---|---|---|\n';
  for (const [id, desc, m, meta] of REQ) {
    const v = d.metrics[m] ? d.metrics[m].values : {};
    const okk = v['p(95)'] !== undefined && v['p(95)'] <= meta;
    md += `| ${id} | ${desc} | ${v.count ?? 0} | ${f(v.med)} | ${f(v['p(95)'])} | ${f(v.max)} | ≤ ${meta} ms | ${okk ? 'Cumple' : 'No cumple'} |\n`;
  }
  const e = d.metrics.errores ? d.metrics.errores.values.rate : 0;
  md += `\nTasa de errores: ${(e * 100).toFixed(2)} % (meta < 1 %). Peticiones HTTP totales: ${d.metrics.http_reqs.values.count}.\n`;
  return { 'resultados/rnf.json': JSON.stringify(d, null, 2), 'resultados/rnf.md': md, stdout: md };
}
