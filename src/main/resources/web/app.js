'use strict';
const $ = s => document.querySelector(s);
const names = {KING:'Rey', QUEEN:'Dama', ROOK:'Torre', BISHOP:'Alfil', KNIGHT:'Caballo', PAWN:'Peón'};
let state, selected = null, flipped = false, busy = false, soundOn = true, toastTimer, audio, feedbackTimer;
let focusedSquare = 'e2';
let effectsOn = true, momentTimer, runningAnimations = [];
try { effectsOn = localStorage.getItem('chessba-effects') !== 'off'; } catch { /* Optional preference. */ }
let jerseyPieces = true;
try { jerseyPieces = localStorage.getItem('chessba-piece-style-v3') !== 'classic'; } catch { /* Optional preference. */ }
let availableFranchises = [];
let currentView='home', setupReady=false;
let crowdOn = false, crowdPreset = 'defense', crowdPlaying = false, crowdLastAt = 0, crowdEndTimer, crowdVoice = null, crowdNodes = [];
try {crowdOn=localStorage.getItem('chessba-crowd')==='on';crowdPreset=localStorage.getItem('chessba-crowd-preset')||'defense';} catch { /* Optional preferences. */ }
const roles = {KING:['FP','Franchise Player'],QUEEN:['PG','Point Guard'],ROOK:['C','Center'],BISHOP:['SF','Small Forward'],KNIGHT:['SG','Shooting Guard'],PAWN:['RK','Rookie']};
try { soundOn = localStorage.getItem('chessba-sound') !== 'off'; } catch { /* Private browsing may disable storage. */ }
const text = (tag, value, className = '') => { const e = document.createElement(tag); e.textContent = value; e.className = className; return e; };
const controlPaths = {
  flip:['M8 3v18M4 7l4-4 4 4M16 21V3M12 17l4 4 4-4'],
  effects:['M12 3l2.4 6.6L21 12l-6.6 2.4L12 21l-2.4-6.6L3 12l6.6-2.4Z'],
  volume:['M11 5 6 9H3v6h3l5 4Z','M15 8a6 6 0 0 1 0 8M18 5a10 10 0 0 1 0 14'],
  mute:['M11 5 6 9H3v6h3l5 4Z','M16 9l5 6M21 9l-5 6'],
  undo:['M8 4 3 9l5 5','M4 9h10a6 6 0 0 1 0 12'],
  redo:['m16 4 5 5-5 5','M20 9H10a6 6 0 0 0 0 12'],
  draw:['M5 9h14M5 15h14'],
  flag:['M5 21V3','M5 4c5-4 9 4 14 0v10c-5 4-9-4-14 0'],
  plus:['M12 5v14M5 12h14'],
  arrow:['M6 18 18 6M6 6h12v12']
};
function controlIcon(name) {
  const svg=document.createElementNS('http://www.w3.org/2000/svg','svg');
  svg.setAttribute('viewBox','0 0 24 24');svg.setAttribute('class','control-icon');svg.setAttribute('aria-hidden','true');
  for (const d of controlPaths[name]) {const path=document.createElementNS(svg.namespaceURI,'path');path.setAttribute('d',d);svg.append(path);}
  return svg;
}
function renderControlIcons() {
  for (const [id,icon,label] of [['flip','flip'],['effectsToggle','effects'],['undo','undo','Deshacer'],['redo','redo','Rehacer'],['draw','draw','Proponer tablas'],['resign','flag','Rendirse'],['newOpen','plus','Nueva partida']]) {
    const button=$('#'+id);button.replaceChildren(controlIcon(icon));if(label)button.append(text('span',label));
  }
  $('#nbaQuick').querySelector('b').replaceChildren(controlIcon('arrow'));
}

async function api(path, params) {
  const response = await fetch('/api/' + path, params ? {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body:new URLSearchParams(params)} : {});
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || 'No se pudo completar la acción.');
  return result;
}
function toast(message, duration = 5000) { clearTimeout(toastTimer); $('#toast').textContent = message; $('#toast').hidden = false; toastTimer = setTimeout(() => $('#toast').hidden = true, duration); }
function connection(ok) { $('#connection').textContent = ok ? 'Conectado' : 'Sin conexión'; $('#connection').prepend(text('i','')); }
function errorMessage(error) {
  if (error instanceof TypeError) { connection(false); return 'No se pudo conectar. Revisá que el servidor Java siga encendido y recargá la página.'; }
  if (/Illegal move|king in check|game is over|already used/.test(error.message)) return 'Esa acción no está permitida en esta posición. Revisá el turno y las reglas.';
  return error.message;
}
async function action(path, params = {}) {
  if (busy) return false;
  busy = true;
  if(path !== 'move')stopCrowd();
  syncAvailability();
  $('#board').setAttribute('aria-busy','true');
  try {
    const before = state;
    const origin = path === 'move' ? moveOrigins(params, before) : [];
    state = await api(path, params); selected = null; connection(true); render();
    if (path === 'move') {
      animateMove(origin);
      const feedback = classifyMove(before, state);
      playSound(feedback.capture ? 'capture' : 'move');
      if (feedback.capture) {
        const scoreKey=before.turn==='WHITE'?'whiteScore':'blackScore';
        showCapture(params.to,feedback.capture,state.scoreboard?Math.max(0,state[scoreKey]-before[scoreKey]):1);
      }
      if (state.status !== 'ONGOING' && state.winner) playSound('win', feedback.capture ? .38 : .12);
      else if (state.check) playSound('check', feedback.capture ? .32 : .1);
      showMoveMoment(before, params, feedback);
      if (state.status !== 'ONGOING') stopCrowd();
      else if (crowdOn && (state.check || state.history.length % 6 === 0)) playCrowd();
    } else if (path === 'undo' || path === 'redo') playSound('navigate');
    if (state.result && currentView==='game') showView('result');
    return true;
  }
  catch (e) { toast(errorMessage(e)); return false; }
  finally { busy = false; $('#board').setAttribute('aria-busy','false'); syncAvailability(); }
}
function syncAvailability() {
  const unavailable = busy || !state;
  $('#newOpen').disabled = unavailable || !setupReady;
  for(const id of ['homeNba','homeClassic','homeCustom','homeTheme'])$('#'+id).disabled=unavailable||!setupReady;
  $('#homeResume').disabled=unavailable;
  $('#resultUndo').disabled=unavailable||!state?.canUndo;
  $('#flip').disabled = unavailable;
  $('#jerseyToggle').disabled = unavailable;
  $('#undo').disabled = unavailable || !state?.canUndo;
  $('#redo').disabled = unavailable || !state?.canRedo;
  $('#draw').disabled = unavailable || state?.status !== 'ONGOING';
  $('#resign').disabled = unavailable || state?.status !== 'ONGOING';
}
// Compare actual board pieces, so en passant and promotion captures are included.
function classifyMove(before, after) {
  const enemy = before.turn === 'WHITE' ? 'BLACK' : 'WHITE';
  const capture = before.board.find(piece => piece.team === enemy &&
    !after.board.some(next => next.team === enemy && next.square === piece.square));
  return {capture: capture || null};
}
function showCapture(square, captured, points = 1) {
  clearTimeout(feedbackTimer);
  const target = $('#board').querySelector(`[data-square="${square}"]`);
  if (motionAllowed()) target?.classList.add('capture-flash');
  toast(`Captura · ${names[captured.type]}`, 2000);
  const notice=$('#scoreboardCapture');
  notice.replaceChildren(text('strong','+'+points),text('span',`${state[captured.team==='WHITE'?'black':'white']} · ${names[captured.type]}`));
  notice.hidden=false;
  notice.classList.remove('score-pop');
  if(motionAllowed()){void notice.offsetWidth;notice.classList.add('score-pop');}
  if (motionAllowed()) {
    if (target) {
      const ring = text('span','','capture-ring');ring.setAttribute('aria-hidden','true');target.append(ring);
      for (let i=0;i<6;i++) {const spark=text('span','','capture-spark spark-'+i);spark.setAttribute('aria-hidden','true');target.append(spark);}
    }
  }
  feedbackTimer = setTimeout(()=>{
    $('#board').querySelectorAll('.capture-flash').forEach(el=>el.classList.remove('capture-flash'));
    $('#board').querySelectorAll('.capture-point, .capture-ring, .capture-spark').forEach(el=>el.remove());
  },1100);
}
function motionAllowed() {
  return effectsOn && !window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
}
function moveOrigins(params, before) {
  const routes = [[params.from,params.to]];
  const actor = before.board.find(piece=>piece.square===params.from);
  if (actor?.type === 'KING' && Math.abs(params.from.charCodeAt(0)-params.to.charCodeAt(0)) === 2) {
    const rank=params.from[1];routes.push(params.to[0]==='g'?['h'+rank,'f'+rank]:['a'+rank,'d'+rank]);
  }
  return routes.map(([from,to])=>({to,rect:$('#board').querySelector(`[data-square="${from}"] .piece`)?.getBoundingClientRect()}));
}
function animateMove(origins) {
  if (!motionAllowed()) return;
  for (const {to,rect} of origins) {
    const piece=$('#board').querySelector(`[data-square="${to}"] .piece`);
    if (!piece?.animate || !rect) continue;
    const end=piece.getBoundingClientRect();
    runningAnimations.push(piece.animate([
      {transform:`translate(${rect.left-end.left}px, ${rect.top-end.top}px) scale(1.04)`},
      {transform:'translate(0,0) scale(1)'}
    ],{duration:220,easing:'cubic-bezier(.2,.75,.25,1)'}));
  }
}
function showMoveMoment(before, params, feedback) {
  if (!effectsOn) return;
  const history=state.history.at(-1)||'';
  const moving=before.board.find(piece=>piece.square===params.from);
  const promoted= moving?.type==='PAWN' && state.board.some(piece=>piece.square===params.to&&piece.type!=='PAWN');
  const hot=history.includes('hot hand: extra move');
  let title,description,kind;
  if (state.winner && state.status!=='ONGOING') {title='GAME WINNER';description=state.decidedOnPoints?'Victoria por puntos':'Jaque mate. Bien jugado.';kind='winner';}
  else if (hot) {title='ON FIRE';description='Hot hand · Tenés otra jugada';kind='hot';}
  else if (promoted) {title='LEVEL UP';description='Tu peón acaba de ascender';kind='promotion';}
  else if (feedback.capture) {title='SWISH';description='Canasta limpia';kind='capture';}
  else if (history.includes('shot clock:')) {title='SHOT CLOCK';description='El rival pierde el turno';kind='clock';}
  else if (state.check) {title='EN JAQUE';description='El rey rival necesita una respuesta';kind='check';}
  else return;
  const call=$('#playMoment');call.dataset.kind=kind;
  call.replaceChildren(text('strong',title),text('small',description));call.hidden=false;
  if (motionAllowed() && (hot||kind==='winner')) $('.board-stage').classList.add('moment-'+kind);
  momentTimer=setTimeout(()=>{call.hidden=true;$('.board-stage').classList.remove('moment-hot','moment-winner');},kind==='winner'?2500:1400);
}
function clearPlayEffects() {
  clearTimeout(momentTimer);
  for (const animation of runningAnimations) animation.cancel();
  runningAnimations=[];
  $('#playMoment').hidden=true;
  $('.board-stage').classList.remove('moment-hot','moment-winner');
}
const pieceShapes = {
  PAWN: ['M33 17 A7 7 0 1 1 19 17 A7 7 0 1 1 33 17 Z', 'M21 24 H31 L33 28 Q26 31 19 28 Z', 'M22 30 H30 C29 36 31 40 35 43 H17 C21 39 23 35 22 30 Z'],
  ROOK: ['M14 11 H20 V17 H23 V11 H29 V17 H32 V11 H38 V24 L33 29 H19 L14 24 Z', 'M20 29 H32 L33 40 L36 43 H16 L19 40 Z', 'M19 25 H33', 'M20 38 H32'],
  KNIGHT: ['M16 43 C16 35 20 31 26 28 L24 24 L19 29 L12 26 L13 22 L20 14 L23 13 L23 7 L28 11 C37 11 42 21 39 32 L36 43 Z', 'M28 16 C33 22 32 29 27 34 C25 37 25 39 25 43', 'M13 24 L18 25', 'M20 15 L24 18'],
  BISHOP: ['M28 8 A2 2 0 1 1 24 8 A2 2 0 1 1 28 8 Z', 'M26 11 C24 16 17 19 17 24 C17 32 35 32 35 24 C35 19 28 15 26 11 Z', 'M29 17 L23 25', 'M20 30 Q26 32 32 30 L31 34 H21 Z', 'M23 34 H29 C29 38 32 41 35 43 H17 C21 40 23 37 23 34 Z'],
  QUEEN: ['M12 16 L19 24 L20 12 L26 23 L32 12 L33 24 L40 16 L35 32 H17 Z', 'M14 14 A2 2 0 1 1 10 14 A2 2 0 1 1 14 14 Z', 'M22 10 A2 2 0 1 1 18 10 A2 2 0 1 1 22 10 Z', 'M34 10 A2 2 0 1 1 30 10 A2 2 0 1 1 34 10 Z', 'M42 14 A2 2 0 1 1 38 14 A2 2 0 1 1 42 14 Z', 'M19 32 H33 L31 37 L35 43 H17 L21 37 Z', 'M20 36 H32'],
  KING: ['M24 6 H28 V11 H32 V15 H28 V21 H24 V15 H20 V11 H24 Z', 'M26 23 C20 13 10 21 16 29 L20 34 H32 L36 29 C42 21 32 13 26 23 Z', 'M20 34 H32 L31 38 L35 43 H17 L21 38 Z', 'M21 38 H31']
};
function pieceElement(piece, classic = false) {
  const wrapper = text('span', '', 'piece ' + piece.team.toLowerCase());
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('viewBox', '0 0 52 56'); svg.setAttribute('aria-hidden', 'true');
  for (const d of [...pieceShapes[piece.type], 'M16 43 H36 L39 48 V51 H13 V48 Z']) {
    const path = document.createElementNS(svg.namespaceURI, 'path'); path.setAttribute('d', d); svg.append(path);
  }
  if (piece.type === 'KNIGHT') { const eye = document.createElementNS(svg.namespaceURI,'circle');eye.setAttribute('cx','29');eye.setAttribute('cy','19');eye.setAttribute('r','1.3');eye.setAttribute('class','piece-eye');svg.append(eye); }
  if (!classic && state?.mode !== 'standard' && jerseyPieces) {
    wrapper.classList.add('dressed-piece');
    const code=franchiseCode(piece.team === 'WHITE' ? 'white' : 'black');wrapper.dataset.franchise=code;
    const y=piece.type==='PAWN'?27:29;
    const garment=document.createElementNS(svg.namespaceURI,'g');garment.setAttribute('class','piece-uniform');
    for (const [d,cls] of [
      [`M18 ${y} 22 ${y+1}Q26 ${y+5} 30 ${y+1}L34 ${y}Q32 ${y+6} 36 ${y+8}L33 ${y+10}V44H19V${y+10}L16 ${y+8}Q20 ${y+6} 18 ${y}Z`,'piece-kit-body'],
      [`M22 ${y+1}Q26 ${y+7} 30 ${y+1}M18 ${y}Q20 ${y+6} 16 ${y+8}M34 ${y}Q32 ${y+6} 36 ${y+8}M20 42h12`,'piece-kit-trim']
    ]) {const path=document.createElementNS(svg.namespaceURI,'path');path.setAttribute('d',d);path.setAttribute('class',cls);garment.append(path);}
    const logo=document.createElementNS(svg.namespaceURI,'image');logo.setAttribute('href',logoUrl(code));logo.setAttribute('x','21.5');logo.setAttribute('y',String(y+5));logo.setAttribute('width','9');logo.setAttribute('height','9');logo.setAttribute('preserveAspectRatio','xMidYMid meet');logo.setAttribute('class','piece-kit-logo');garment.append(logo);
    svg.append(garment);wrapper.append(svg);
    wrapper.title = `${names[piece.type]} · ${roles[piece.type][1]}`;
  } else wrapper.append(svg);
  return wrapper;
}
function renderPieceKey() {
  const legend = $('#rosterLegend');legend.replaceChildren();
  if (state.mode === 'standard' || !jerseyPieces) {legend.hidden=true;return;}
  legend.hidden=false;
  legend.append(text('strong','Cómo reconocer las piezas'));
  for (const type of ['KING','QUEEN','ROOK','BISHOP','KNIGHT','PAWN']) {
    const item=text('div','','piece-key-item');item.append(pieceElement({type,team:'WHITE'},true),text('span',names[type]));legend.append(item);
  }
  legend.append(text('small','La forma indica la pieza; la casaca y el escudo indican el equipo.'));
}
const franchiseIds = {ATL:1610612737,BOS:1610612738,BKN:1610612751,CHA:1610612766,CHI:1610612741,CLE:1610612739,DAL:1610612742,DEN:1610612743,DET:1610612765,GSW:1610612744,HOU:1610612745,IND:1610612754,LAC:1610612746,LAL:1610612747,MEM:1610612763,MIA:1610612748,MIL:1610612749,MIN:1610612750,NOP:1610612740,NYK:1610612752,OKC:1610612760,ORL:1610612753,PHI:1610612755,PHX:1610612756,POR:1610612757,SAC:1610612758,SAS:1610612759,TOR:1610612761,UTA:1610612762,WAS:1610612764};
function logoUrl(code) {return franchiseIds[code]?`https://cdn.nba.com/logos/nba/${franchiseIds[code]}/primary/L/logo.svg`:'';}
const franchiseCodes = {
  Hawks:'ATL', Celtics:'BOS', Nets:'BKN', Hornets:'CHA', Bulls:'CHI', Cavaliers:'CLE', Mavericks:'DAL', Nuggets:'DEN', Pistons:'DET', Warriors:'GSW', Rockets:'HOU', Pacers:'IND', Clippers:'LAC', Lakers:'LAL', Grizzlies:'MEM', Heat:'MIA', Bucks:'MIL', Timberwolves:'MIN', Pelicans:'NOP', Knicks:'NYK', Thunder:'OKC', Magic:'ORL', '76ers':'PHI', Suns:'PHX', 'Trail Blazers':'POR', Kings:'SAC', Spurs:'SAS', Raptors:'TOR', Jazz:'UTA', Wizards:'WAS'
};
function franchiseCode(key) {
  if (state.mode === 'standard') return key === 'white' ? 'WHT' : 'BLK';
  return Object.entries(franchiseCodes).find(([name])=>state[key].endsWith(name))?.[1] || 'NBA';
}
function teamMark(code) {
  const mark = text('span','','team-mark');mark.dataset.franchise = code;
  mark.append(text('span',code === 'WHT' ? 'W' : code === 'BLK' ? 'B' : code,'team-monogram'));
  if (franchiseIds[code]) {
    const image=document.createElement('img');image.className='franchise-logo';image.alt='';image.width=64;image.height=64;image.decoding='async';image.referrerPolicy='no-referrer';
    image.addEventListener('load',()=>mark.classList.add('has-logo'));
    image.addEventListener('error',()=>{mark.classList.remove('has-logo');image.hidden=true;});
    image.src=logoUrl(code);mark.append(image);
  }
  mark.setAttribute('aria-hidden','true');
  return mark;
}
function franchiseLabel(name) {
  const nickname = Object.keys(franchiseCodes).find(label=>name.endsWith(label));
  return nickname ? {city:name.slice(0,-nickname.length).trim(),name:nickname} : {city:'AJEDREZ CLÁSICO',name};
}
function render() {
  clearTimeout(feedbackTimer);
  $('#scoreboardCapture').hidden=true;
  clearPlayEffects();
  document.body.dataset.mode = state.mode;
  $('#arenaBadge').textContent = state.status === 'ONGOING' ? 'PARTIDA LOCAL' : 'PARTIDA FINALIZADA';
  $('#matchAction').textContent = state.history.length ? `JUGADA ${String(state.history.length).padStart(2,'0')}` : 'A JUGAR';
  $('#jerseyToggle').hidden = state.mode === 'standard';
  $('#jerseyToggle').setAttribute('aria-pressed',String(jerseyPieces));
  $('#jerseyToggle').textContent = jerseyPieces?'Casacas: sí':'Casacas: no';
  $('#jerseyToggle').setAttribute('aria-label',jerseyPieces?'Quitar las casacas de las piezas':'Vestir las piezas con la casaca de su equipo');
  renderPieceKey();
  $('#courtAway').textContent = state[flipped?'white':'black'].toUpperCase();
  $('#courtHome').textContent = state[flipped?'black':'white'].toUpperCase();
  const courtLogo=$('#courtLogo'),courtBrand=$('#courtBrand');
  if (state.mode !== 'standard') {
    const url=logoUrl(franchiseCode('white'));
    courtLogo.onload=()=>{courtLogo.setAttribute('visibility','visible');courtBrand.setAttribute('visibility','hidden');};
    courtLogo.onerror=()=>{courtLogo.setAttribute('visibility','hidden');courtBrand.setAttribute('visibility','visible');};
    if (courtLogo.getAttribute('href')!==url) {courtLogo.setAttribute('visibility','hidden');courtBrand.setAttribute('visibility','visible');courtLogo.setAttribute('href',url);}
  } else {courtLogo.removeAttribute('href');courtLogo.setAttribute('visibility','hidden');courtBrand.setAttribute('visibility','visible');}
  for (const key of ['white','black']) {
    const code = franchiseCode(key), team = $(`#match${key === 'white' ? 'White' : 'Black'}`);
    team.dataset.franchise = code;
    team.classList.toggle('has-turn',state.turn === key.toUpperCase() && state.status === 'ONGOING');
    const label = state.mode === 'standard' ? {city:key==='white'?'LOCAL · BLANCAS':'VISITANTE · NEGRAS',name:state[key]} : franchiseLabel(state[key]);
    const info=text('div','','team-info');
    info.append(text('small',label.city),text('strong',label.name));
    const turn = text('span',state.status === 'ONGOING' && state.turn === key.toUpperCase() ? 'EN TURNO' : key === 'white' ? 'BLANCAS' : 'NEGRAS','possession');
    info.append(turn);
    team.replaceChildren(teamMark(code),info);
    team.title = state[key];
  }
  $('#nbaQuick').hidden = state.mode !== 'standard';
  renderBoard();
  const over = state.status !== 'ONGOING';
  const side = state.turn === 'WHITE' ? 'blancas' : 'negras';
  const titles = {CHECKMATE:'Jaque mate.', STALEMATE:'Tablas por ahogado.', DRAW_THREEFOLD_REPETITION:'Tablas por repetición.', DRAW_FIFTY_MOVE_RULE:'Tablas: 50 movimientos.', DRAW_INSUFFICIENT_MATERIAL:'Material insuficiente.', DRAW_BY_AGREEMENT:'Tablas de común acuerdo.', RESIGNATION:'Partida terminada.'};
  const activeName = state.mode === 'standard' ? side : franchiseLabel(state[state.turn === 'WHITE' ? 'white' : 'black']).name;
  $('#turnTitle').textContent = over ? state.decidedOnPoints?'Victoria por puntos.':titles[state.status] || 'Partida terminada.' : state.check ? 'Jaque al rey.' : `Juegan ${activeName}.`;
  $('#turnCopy').textContent = over ? state.winner ? `${state[state.winner === 'WHITE' ? 'white' : 'black']} ganó la partida.` : 'La partida terminó en tablas.' : state.check ? 'Hay que salir del jaque para seguir.' : state.history.length ? 'Seleccioná una pieza para ver dónde puede ir.' : 'Las blancas hacen el primer movimiento.';
  $('#turnIndicator').replaceChildren(text('i', '', state.turn === 'BLACK' ? 'black' : ''), text('span', over ? 'Podés deshacer para seguir explorando' : `${state[state.turn === 'WHITE' ? 'white' : 'black']} tiene el turno`));
  $('#modePill').textContent = state.mode === 'standard' ? 'CLÁSICO' : state.mode === 'nba' ? 'NBA COMPLETO' : state.mode === 'theme' ? 'NBA · REGLAS CLÁSICAS' : 'NBA A TU MANERA';
  $('#moveCount').textContent = state.history.length;
  $('#progressClock').textContent = `Regla de 50 movimientos: ${state.halfmoveClock ?? 0}/100 medias jugadas sin captura ni movimiento de peón.`;
  $('#benchCount').textContent = state.whiteBench.length + state.blackBench.length;
  const history = $('#history'); history.replaceChildren();
  if (!state.history.length) {
    const empty = text('div', '', 'empty-history');const illustration=text('span','','empty-illustration');illustration.append(pieceElement({type:'KNIGHT',team:'WHITE'}));
    empty.append(illustration,text('p','Todavía no hay jugadas'),text('small','Los movimientos se guardan acá.')); history.append(empty);
  } else state.history.forEach((description, i) => {
    const row = text('div','','history-row'); row.append(text('span', String(i + 1).padStart(2,'0')), text('span', formatMove(description))); row.title = description; history.append(row);
  });
  history.scrollTop = history.scrollHeight;
  $('#undo').disabled = !state.canUndo; $('#redo').disabled = !state.canRedo;
  $('#draw').disabled = over; $('#resign').disabled = over;
  $('#mechanics').replaceChildren();
  renderArenaScoreboard();
  if (state.shotClock) nbaStat('SHOT CLOCK',`Jugadas restantes · límite ${state.limit}`,Math.max(0,state.limit-state.whiteClock),Math.max(0,state.limit-state.blackClock),'clock');
  if (state.hotHand) nbaStat('HOT HAND','Capturas propias consecutivas',state.whiteStreak + '/2',state.blackStreak + '/2','streak');
  if (state.trade) nbaStat('TRADE','Un intercambio por equipo',state.whiteTraded ? 'Usado' : 'Listo',state.blackTraded ? 'Usado' : 'Listo','trade');
  renderBench(); updateHint(); renderHome(); renderResult(); syncAvailability();
  if(currentView==='result'&&!state.result)showView('game',false);
}
function renderArenaScoreboard() {
  const result=state.result;
  $('#scoreboardStatus').textContent=result?'FINAL':'EN JUEGO';
  $('#arenaScoreboard').classList.toggle('is-final',Boolean(result));
  $('#scoreboardTurn').textContent=result?result.reason:`En turno · ${state[state.turn==='WHITE'?'white':'black']}`;
  for(const side of ['white','black']) {
    const suffix=side==='white'?'White':'Black';
    $('#scoreboard'+suffix+'Logo').replaceChildren(teamMark(franchiseCode(side)));
    $('#scoreboard'+suffix+'Name').textContent=state.mode==='standard'?state[side]:franchiseLabel(state[side]).name;
    $('#scoreboard'+suffix+'Score').textContent=state.scoreboard?state[side+'Score']:result?(result.winner?result.winner===side.toUpperCase()?'1':'0':'½'):'—';
    $('#scoreboard'+suffix+'Name').parentElement.classList.toggle('in-possession',!result&&state.turn===side.toUpperCase());
  }
  $('#scoreboardMove').textContent=String(state.history.length).padStart(2,'0');
  $('#scoreboardCaption').textContent=state.scoreboard?'PUNTOS POR CAPTURAS':'RESULTADO DE AJEDREZ';
  $('#scoreboardRules').textContent=state.scoreboard?'Peón 1 · Caballo y alfil 3 · Torre 5 · Dama 9':'El resultado se define al terminar la partida.';
}
function showView(view, focus = true) {
  currentView=view;
  for(const name of ['home','game','result'])$('#'+name+'Screen').hidden=name!==view;
  document.body.dataset.view=view;
  if(view!=='game')stopCrowd();
  if(focus){const heading=view==='home'?$('#homeTitle'):view==='result'?$('#resultTitle'):$('#gameScreen').querySelector('h1');heading?.setAttribute('tabindex','-1');heading?.focus?.({preventScroll:true});window.scrollTo?.({top:0,behavior:'instant'});}
}
function renderHome() {
  $('#homeResume').hidden=!state.started&&state.history.length===0;
  $('#homeResume').textContent=state.result?'Revisar tablero':'Continuar partida';
  $('#homeResult').hidden=!state.result;
  if(!$('#homeTeamMarks').children.length)$('#homeTeamMarks').append(teamMark('LAL'),teamMark('BOS'));
}
function renderResult() {
  const result=state.result;
  $('#gameResult').hidden=!result;
  if(!result)return;
  $('#resultTitle').textContent=result.winnerName?`Ganó ${result.winnerName}`:'La partida terminó en tablas';
  $('#resultReason').textContent=result.reason;
  $('#resultOutcome').textContent=result.outcome;
  for(const key of ['white','black']) {
    const name=key==='white'?'White':'Black',team=$('#result'+name);
    team.replaceChildren(teamMark(franchiseCode(key)),text('strong',result[key]),text('small',key==='white'?'Blancas':'Negras'));
    team.classList.toggle('result-winner',result.winner===key.toUpperCase());
    $('#result'+name+'Label').textContent=franchiseLabel(result[key]).name;
    $('#result'+name+'Points').textContent=result[key+'Score'];
    $('#result'+name+'Pieces').textContent=result[key+'Pieces'];
    $('#result'+name+'Bench').textContent=result[key+'Bench'];
  }
  $('#resultPointsRow').hidden=!result.scoreboard;
  $('#resultActions').textContent=`${result.actions} ${result.actions===1?'acción registrada':'acciones registradas'} en esta partida.`;
  $('#resultPointsNote').hidden=!result.decidedOnPoints;
  $('#resultPointsNote').textContent=result.decidedOnPoints?'El ajedrez terminó en tablas por una regla. El marcador NBA definió al ganador.':'';
}
function formatMove(value) {
  const m = value.match(/([a-h][1-8])([a-h][1-8])(=[QRBN])?/);
  if (m) return `${m[1]} → ${m[2]}${m[3] || ''}${value.includes('hot hand') ? ' ✧' : ''}${value.includes('shot clock') ? ' ◷' : ''}`;
  if (value.startsWith('Trade ')) return value.replace('Trade ', 'Cambio ').replace(' for bench ', ' por ');
  if (/resign/i.test(value)) return 'Rendición';
  if (/draw/i.test(value)) return 'Tablas acordadas';
  return value;
}
function nbaStat(title, subtitle, white, black, kind) {
  const card = text('section','','nba-stat '+kind), header = text('div','','stat-label');
  header.append(text('strong',title),text('small',subtitle));
  const values = text('div','','stat-values');
  for (const [key,value] of [['white',white],['black',black]]) {
    const side = text('div','','stat-side');side.dataset.franchise=franchiseCode(key);
    if (kind === 'clock' && Number(value) <= 3) side.classList.add('clock-warning');
    const expired=kind==='clock'&&state[key+'ClockExpired'];
    side.title=state[key]+(expired?' · Límite agotado: salto de turno pendiente':'');
    side.append(teamMark(franchiseCode(key)),text('b',String(value)));
    if(expired)side.append(text('small','Agotado'));
    values.append(side);
  }
  card.append(header,values);$('#mechanics').append(card);
}
function renderBoard() {
  const board = $('#board');
  const restoreFocus = board.contains?.(document.activeElement);
  board.replaceChildren();
  const last = state.history.at(-1)?.match(/([a-h][1-8])([a-h][1-8])/);
  const targets = state.legalMoves.filter(m => m.from === selected);
  for (let row=0;row<8;row++) for (let col=0;col<8;col++) {
    const file = flipped ? 7-col : col, rank = flipped ? row : 7-row;
    const square = String.fromCharCode(97+file)+(rank+1), piece = state.board.find(p => p.square === square);
    const el = document.createElement('button'); el.type = 'button'; el.className = 'square'+((file+rank)%2===0?' dark':'');
    el.dataset.square = square;
    el.tabIndex = square === focusedSquare ? 0 : -1;
    if (last && (last[1] === square || last[2] === square)) el.classList.add('last');
    if (square === selected) el.classList.add('selected');
    if (state.check && piece?.type === 'KING' && piece.team === state.turn) el.classList.add('check');
    el.setAttribute('aria-label', square + (piece ? `, ${names[piece.type]} ${piece.team === 'WHITE' ? 'blanco' : 'negro'}` : ', vacía'));
    if (piece) el.title=`${names[piece.type]} · ${state[piece.team === 'WHITE'?'white':'black']}${state.mode!=='standard'?' · '+roles[piece.type][1]:''}`;
    el.setAttribute('aria-pressed', String(square === selected));
    if (col === 0) el.append(text('span', rank+1, 'coord rank'));
    if (row === 7) el.append(text('span', String.fromCharCode(97+file),'coord file'));
    if (piece) { const p = pieceElement(piece); p.draggable = piece.team === state.turn; p.addEventListener('dragstart', event => { if (busy) {event.preventDefault();return;} selected=square; event.dataTransfer.setData('text/plain',square); event.dataTransfer.effectAllowed='move'; updateHint(); }); el.append(p); }
    if (targets.some(m=>m.to === square)) el.append(text('span','',''+(piece?'legal-ring':'legal-dot')));
    el.addEventListener('focus',()=>{focusedSquare=square;});
    el.addEventListener('click',()=>{focusedSquare=square;chooseSquare(square);});
    el.addEventListener('keydown',event=>{
      const offsets = {ArrowLeft:[-1,0],ArrowRight:[1,0],ArrowUp:[0,1],ArrowDown:[0,-1]};
      if (event.key === 'Escape') {selected=null;renderBoard();renderBench();updateHint();return;}
      if (!offsets[event.key]) return;
      event.preventDefault();
      const [df,dr] = offsets[event.key].map(value=>flipped?-value:value);
      const nextFile=file+df,nextRank=rank+dr;
      if (nextFile<0||nextFile>7||nextRank<0||nextRank>7) return;
      focusedSquare=String.fromCharCode(97+nextFile)+(nextRank+1);
      board.querySelectorAll('[data-square]').forEach(button=>button.tabIndex=button.dataset.square===focusedSquare?0:-1);
      board.querySelector(`[data-square="${focusedSquare}"]`)?.focus();
    });
    el.addEventListener('dragover',e=>{e.preventDefault(); e.dataTransfer.dropEffect='move';});
    el.addEventListener('drop',e=>{e.preventDefault(); if (e.dataTransfer.getData('text/plain') === selected) chooseSquare(square);});
    board.append(el);
  }
  if (restoreFocus) board.querySelector(`[data-square="${focusedSquare}"]`)?.focus();
}
const pieceTips = {
  PAWN: 'Avanza una casilla y captura en diagonal. Desde su lugar inicial puede avanzar dos. Si llega al otro extremo, se transforma en otra pieza.',
  ROOK: 'Se mueve en línea recta: hacia adelante, atrás y a los costados, tantas casillas como estén libres.',
  KNIGHT: 'Se mueve en L: dos casillas en una dirección y una hacia el costado. Es la única pieza que salta sobre otras.',
  BISHOP: 'Se mueve en diagonal, tantas casillas libres como quieras. Siempre permanece en el mismo color.',
  QUEEN: 'Combina torre y alfil: se mueve en línea recta o diagonal, tantas casillas libres como quieras.',
  KING: 'Se mueve una casilla en cualquier dirección. Protegelo: nunca puede terminar en una casilla atacada.'
};
function updateHint() {
  $('#boardHint').textContent = state.status !== 'ONGOING' ? 'Bien jugado. ¿Una más?' : selected ? `Pieza en ${selected} · elegí una casilla marcada` : 'Seleccioná una pieza para ver sus movimientos';
  const piece = state.board.find(p=>p.square === selected);
  const guide = $('#learnGuide');
  if (!guide) return;
  const moves = new Set(state.legalMoves.filter(m=>m.from === selected).map(m=>m.to)).size;
  $('#learnTitle').textContent = piece ? `${names[piece.type]} en ${selected}${state.mode !== 'standard' ? ' · ' + roles[piece.type][1] : ''}` : state.check ? 'Tu rey está en jaque' : '¿Recién empezás?';
  $('#learnText').textContent = piece ? pieceTips[piece.type] : state.check ? 'Estás en jaque. Mové el rey, bloqueá el ataque o capturá la pieza que lo amenaza. El tablero solo marca jugadas que te protegen.' : 'Tocá una pieza de tu equipo. Los puntos indican dónde puede ir. Después tocá uno de esos destinos para jugar.';
  $('#learnBadge').textContent = piece ? `${moves} ${moves === 1 ? 'casilla disponible' : 'casillas disponibles'}` : 'Una mano para empezar';
  $('#learnIcon').replaceChildren(pieceElement(piece || {type:'KNIGHT',team:'WHITE'}));
}
async function chooseSquare(square) {
  if (busy || state.status !== 'ONGOING') return;
  unlockSound();
  const candidates = state.legalMoves.filter(m=>m.from === selected && m.to === square);
  if (selected && candidates.length) {
    const from = selected;
    let promotion;
    if (candidates.some(m=>m.promotion)) { promotion = await askPromotion(candidates); if (!promotion) return; }
    await action('move', {from,to:square,...(promotion ? {promotion} : {})});
    return;
  }
  const piece = state.board.find(p=>p.square === square);
  selected = selected !== square && piece?.team === state.turn ? square : null;
  renderBoard(); renderBench(); updateHint();
}
function askPromotion(candidates) {
  const dialog = $('#promotionDialog'); const choices = $('#promotionChoices'); choices.replaceChildren();
  return new Promise(resolve=>{
    const done = value => {dialog.close();resolve(value);};
    [...new Set(candidates.map(m=>m.promotion))].forEach(type=>{const button=text('button',''); button.append(pieceElement({type,team:state.turn}),text('small',names[type]));button.onclick=()=>done(type);choices.append(button);});
    dialog.oncancel=()=>resolve(null); dialog.showModal();
  });
}
function renderBench() {
  const bench = $('#bench'); bench.replaceChildren();
  for (const key of ['white','black']) {
    const el = text('div', state[key], 'bench-team'), pieces = text('div','','bench-pieces');
    if (!state[key+'Bench'].length) pieces.append(text('span','Sin piezas en el banco.','bench-description'));
    state[key+'Bench'].forEach(type=>{
      const button=text('button','','bench-piece');button.append(pieceElement({type,team:key.toUpperCase()}));button.title=`Intercambiar por ${names[type]}`;
      button.setAttribute('aria-label',`Intercambiar pieza seleccionada por ${names[type]}`);
      const own = state.turn === key.toUpperCase();
      const chosen = state.board.find(p=>p.square===selected);
      const invalidPawnRank=type==='PAWN'&&/[18]$/.test(selected||'');
      button.disabled = !state.trade || !own || !chosen || chosen.team!==state.turn || chosen.type === 'KING' || invalidPawnRank || state[key+'Traded'] || state.check || state.status !== 'ONGOING';
      if(invalidPawnRank)button.title='Un peón no puede entrar en la primera ni en la última fila.';
      button.onclick=async()=>{const square=selected;if(await confirmAction('¿Cambio de estrategia?',`Reemplazar la pieza en ${square} por ${names[type].toLowerCase()}. El intercambio consume tu turno.`)){await action('trade',{square,piece:type});}};
      pieces.append(button);
    });
    el.append(pieces);bench.append(el);
  }
  $('#tradeHint').textContent = state.trade ? selected ? 'Elegí una pieza de tu banco para intercambiar. Una vez por equipo.' : 'Para intercambiar, primero seleccioná una pieza propia del tablero.' : 'Activá los intercambios al crear una partida NBA para usar tu banco.';
}
function confirmAction(title, copy) {
  const d=$('#confirmDialog');$('#confirmTitle').textContent=title;$('#confirmCopy').textContent=copy;
  return new Promise(resolve=>{ const done=v=>{d.close();resolve(v);};$('#confirmAccept').onclick=()=>done(true);$('#confirmCancel').onclick=()=>done(false);d.oncancel=()=>resolve(false);d.showModal(); });
}
function unlockSound() {
  if (!soundOn) return;
  try {
    audio ||= new (window.AudioContext || window.webkitAudioContext)();
    if (audio.state === 'suspended') audio.resume().catch(()=>{});
  } catch { /* Audio is optional and never blocks a move. */ }
}
function syncSoundButton() {
  $('#sound').setAttribute('aria-pressed',String(soundOn));
  $('#sound').setAttribute('aria-label',soundOn ? 'Silenciar sonidos de cancha' : 'Activar sonidos de cancha');
  $('#sound').title = soundOn ? 'Sonidos de cancha activados · silenciar' : 'Activar sonidos de cancha';
  $('#sound').replaceChildren(controlIcon(soundOn?'volume':'mute'));
  $('#crowdTest').disabled=!soundOn;
}
function syncCrowdControls() {
  $('#crowdPreset').value=['defense','letsgo','claps'].includes(crowdPreset)?crowdPreset:'defense';
  $('#crowdToggle').textContent=crowdOn?'Activada':'Activar';
  $('#crowdToggle').setAttribute('aria-pressed',String(crowdOn));
  $('#crowdTest').disabled=!soundOn;
}
function stopCrowd() {
  clearTimeout(crowdEndTimer);
  if (crowdVoice) {window.speechSynthesis?.cancel();crowdVoice=null;}
  for(const node of crowdNodes){try{node.stop();node.disconnect();}catch{/* Already ended. */}}
  crowdNodes=[];crowdPlaying=false;
}
function playCrowd(preview = false) {
  if (!soundOn || (!preview && (!crowdOn || crowdPlaying || Date.now()-crowdLastAt<20000))) return;
  stopCrowd();unlockSound();crowdPlaying=true;crowdLastAt=Date.now();
  const beats=crowdPreset==='claps'?[0,.28,.56,1.1,1.38,1.66]:[0,.3,.9,1.2,1.8,2.1];
  for(const beat of beats)playSound('crowdClap',beat);
  if(crowdPreset!=='claps') {
    for(const beat of [0,.9,1.8])playSound('crowdStomp',beat);
    if(window.speechSynthesis && window.SpeechSynthesisUtterance) {
      crowdVoice=new window.SpeechSynthesisUtterance(crowdPreset==='letsgo'?"Let's go! Let's go!":'Defense! Defense!');
      crowdVoice.lang='en-US';crowdVoice.rate=.88;crowdVoice.pitch=.72;crowdVoice.volume=.42;
      try{window.speechSynthesis.speak(crowdVoice);}catch{/* The rhythm works even without speech support. */}
    }
  }
  crowdEndTimer=setTimeout(()=>{crowdPlaying=false;crowdVoice=null;crowdNodes=[];},4000);
}
function playSound(kind = 'move', delay = 0) {
  if (!soundOn) return;
  unlockSound();
  if (!audio || audio.state !== 'running') return;
  try {
    const now = audio.currentTime + delay;
    const tone = (hz, start, duration, volume = .07, endHz = hz) => {
      const oscillator = audio.createOscillator(), gain = audio.createGain();
      oscillator.type = 'sine'; oscillator.frequency.setValueAtTime(hz,start);
      oscillator.frequency.exponentialRampToValueAtTime(endHz,start+duration);
      gain.gain.setValueAtTime(.0001,start);gain.gain.exponentialRampToValueAtTime(volume,start+.008);
      gain.gain.exponentialRampToValueAtTime(.0001,start+duration);
      oscillator.connect(gain);gain.connect(audio.destination);oscillator.start(start);oscillator.stop(start+duration+.01);
      if(kind.startsWith('crowd'))crowdNodes.push(oscillator);
      oscillator.onended=()=>{oscillator.disconnect();gain.disconnect();};
    };
    if (kind === 'capture') {
      tone(115,now,.16,.18,42);
      // Filtered air followed by a short net rustle: a clean basket, without a rim hit.
      const duration = .48, buffer = audio.createBuffer(1,Math.ceil(audio.sampleRate*duration),audio.sampleRate);
      const samples = buffer.getChannelData(0);
      for (let i=0;i<samples.length;i++) samples[i] = Math.random()*2-1;
      const source = audio.createBufferSource(), filter = audio.createBiquadFilter(), gain = audio.createGain();
      source.buffer = buffer; filter.type = 'bandpass';filter.Q.value=.75;
      filter.frequency.setValueAtTime(4200,now);filter.frequency.exponentialRampToValueAtTime(650,now+.44);
      gain.gain.setValueAtTime(.0001,now);gain.gain.exponentialRampToValueAtTime(.28,now+.045);
      gain.gain.exponentialRampToValueAtTime(.06,now+.19);gain.gain.exponentialRampToValueAtTime(.13,now+.24);
      gain.gain.exponentialRampToValueAtTime(.0001,now+duration);
      source.connect(filter);filter.connect(gain);gain.connect(audio.destination);source.start(now);source.stop(now+duration);
      source.onended=()=>{source.disconnect();filter.disconnect();gain.disconnect();};
    } else if(kind === 'crowdClap') {
      const buffer=audio.createBuffer(1,Math.ceil(audio.sampleRate*.12),audio.sampleRate),samples=buffer.getChannelData(0);
      for(let i=0;i<samples.length;i++)samples[i]=Math.random()*2-1;
      const source=audio.createBufferSource(),filter=audio.createBiquadFilter(),gain=audio.createGain();source.buffer=buffer;filter.type='bandpass';filter.frequency.value=1600;filter.Q.value=.7;
      gain.gain.setValueAtTime(.0001,now);gain.gain.exponentialRampToValueAtTime(.12,now+.004);gain.gain.exponentialRampToValueAtTime(.018,now+.025);gain.gain.exponentialRampToValueAtTime(.075,now+.035);gain.gain.exponentialRampToValueAtTime(.0001,now+.12);
      source.connect(filter);filter.connect(gain);gain.connect(audio.destination);source.start(now);source.stop(now+.13);crowdNodes.push(source);source.onended=()=>{source.disconnect();filter.disconnect();gain.disconnect();};
    } else if(kind === 'crowdStomp') tone(110,now,.12,.085,55);
    else if (kind === 'check') {
      tone(660,now,.13,.045);tone(880,now+.14,.18,.04);
    } else if (kind === 'win' || kind === 'start') {
      (kind === 'win' ? [523,659,784,1047] : [392,523,784]).forEach((hz,i)=>tone(hz,now+i*.09,.23,.045));
    } else if (kind === 'navigate') tone(330,now,.065,.035,260);
    else { tone(230,now,.065,.12,100);tone(620,now,.035,.025,380); }
  } catch { /* Browsers without audio support can still play chess. */ }
}
function openNewGame(mode = ['custom','theme'].includes(state?.mode) ? state.mode : 'nba') {
  if (busy || !state || !setupReady) return;
  const form = $('#newForm');
  form.querySelector(`[name=mode][value=${mode}]`).checked = true;
  for (const [name,value] of [['shotClock',state.shotClock],['trade',state.trade],['hotHand',state.hotHand],['scoreboard',state.scoreboard]]) form.querySelector(`[name=${name}]`).checked = Boolean(value);
  form.querySelector('[name=limit]').value = Math.max(1,Math.min(24,state.limit || 10));
  if (state.mode !== 'standard') {
    $('#whiteFranchise').value = franchiseCode('white');
    $('#blackFranchise').value = franchiseCode('black');
  }
  form.dispatchEvent(new Event('change'));
  $('#newDialog').showModal();
}
$('#newOpen').onclick=()=>openNewGame();
$('#backMenu').onclick=()=>showView('home');
$('#homeNba').onclick=()=>openNewGame('nba');
$('#homeClassic').onclick=()=>openNewGame('standard');
$('#homeCustom').onclick=()=>openNewGame('custom');
$('#homeTheme').onclick=()=>openNewGame('theme');
$('#homeResume').onclick=()=>showView('game');
$('#homeResult').onclick=()=>showView('result');
$('#homeRules').onclick=()=>$('#rulesDialog').showModal();
$('#gameResult').onclick=()=>showView('result');
$('#resultNew').onclick=()=>openNewGame();
$('#resultMenu').onclick=()=>showView('home');
$('#resultReview').onclick=()=>showView('game');
$('#resultUndo').onclick=()=>action('undo');
$('#jerseyToggle').onclick=()=>{
  jerseyPieces=!jerseyPieces;
  try {localStorage.setItem('chessba-piece-style-v3',jerseyPieces?'jerseys':'classic');} catch { /* Optional preference. */ }
  render();
};
function syncEffectsButton() {
  document.body.dataset.effects=effectsOn?'on':'off';
  $('#effectsToggle').setAttribute('aria-pressed',String(effectsOn));
  $('#effectsToggle').setAttribute('aria-label',effectsOn?'Desactivar efectos visuales':'Activar efectos visuales');
  $('#effectsToggle').title=effectsOn?'Efectos activados · desactivar':'Activar efectos visuales';
}
$('#effectsToggle').onclick=()=>{
  effectsOn=!effectsOn;syncEffectsButton();clearPlayEffects();
  try {localStorage.setItem('chessba-effects',effectsOn?'on':'off');} catch { /* Optional preference. */ }
  toast(effectsOn?'Efectos visuales activados':'Efectos visuales desactivados');
};
function updateTeamPreview() {
  for (const key of ['white','black']) {
    const select=$(`#${key}Franchise`), preview=$(`#${key}Preview`);
    preview.dataset.franchise=select.value;
    const selectedOption=Array.from(select.options).find(option=>option.value===select.value);
    const label=franchiseLabel(selectedOption?.textContent || '');
    preview.replaceChildren(teamMark(select.value),text('small',label.city),text('strong',label.name),text('span',key==='white'?'Local · blancas':'Visitante · negras','preview-side'));
    const trigger=$(`#${key}Picker`);
    trigger.replaceChildren(teamMark(select.value),text('span',selectedOption?.textContent||'Elegí un equipo'),text('span','⌄','picker-chevron'));
    for (const option of $(`#${key}Options`).querySelectorAll('[data-code]')) {
      const chosen=option.dataset.code===select.value;
      option.setAttribute('aria-selected',String(chosen));
      option.disabled=option.dataset.code===$(`#${key==='white'?'black':'white'}Franchise`).value;
    }
  }
}
function closeTeamMenus() {
  for (const key of ['white','black']) {$(`#${key}Menu`).hidden=true;$(`#${key}Picker`).setAttribute('aria-expanded','false');}
}
function initializeTeamPickers() {
  for (const key of ['white','black']) {
    const list=$(`#${key}Options`),search=$(`#${key}Search`),trigger=$(`#${key}Picker`),menu=$(`#${key}Menu`);
    list.replaceChildren();
    for (const franchise of availableFranchises) {
      const option=text('button','','team-option');option.type='button';option.dataset.code=franchise.code;option.dataset.name=franchise.name.toLowerCase();option.setAttribute('role','option');
      option.append(teamMark(franchise.code),text('span',franchise.name));
      option.onclick=()=>{$(`#${key}Franchise`).value=franchise.code;$('#newError').textContent='';closeTeamMenus();updateTeamPreview();trigger.focus();};list.append(option);
    }
    const filter=()=>{const query=search.value.trim().toLowerCase();for(const option of list.querySelectorAll('[data-code]'))option.hidden=!option.dataset.name.includes(query);};
    search.addEventListener('input',filter);
    trigger.onclick=()=>{const opening=menu.hidden;closeTeamMenus();if(opening){menu.hidden=false;trigger.setAttribute('aria-expanded','true');search.value='';filter();search.focus();}};
    menu.addEventListener('keydown',event=>{
      if(event.key==='Escape'){event.preventDefault();event.stopPropagation();closeTeamMenus();trigger.focus();return;}
      if(!['ArrowDown','ArrowUp','Home','End'].includes(event.key))return;
      const options=[...list.querySelectorAll('[data-code]')].filter(option=>!option.hidden&&!option.disabled);if(!options.length)return;
      event.preventDefault();const index=options.indexOf(document.activeElement);
      const next=event.key==='Home'?0:event.key==='End'?options.length-1:event.key==='ArrowDown'?(index+1)%options.length:index<0?options.length-1:(index-1+options.length)%options.length;options[next].focus();
    });
  }
  $('#newDialog').addEventListener('click',event=>{if(!event.target.closest('.team-choice'))closeTeamMenus();});
}
$('#nbaQuick').onclick=()=>{
  openNewGame('nba');
};
$('#playNav').onclick=()=>showView('home');
$('#rulesOpen').onclick=()=>$('#rulesDialog').showModal();
$('#learnRules').onclick=()=>$('#rulesDialog').showModal();
document.querySelectorAll('[data-close]').forEach(button=>button.onclick=()=>button.closest('dialog').close());
$('#flip').onclick=()=>{flipped=!flipped;if(state)render();};
$('#sound').onclick=()=>{
  soundOn=!soundOn;syncSoundButton();
  try {localStorage.setItem('chessba-sound',soundOn?'on':'off');} catch { /* Optional preference. */ }
  if (soundOn) {unlockSound();audio?.resume().then(()=>playSound('capture')).catch(()=>{});}
  else {stopCrowd();if(audio?.state==='running')audio.suspend().catch(()=>{});}
  toast(soundOn?'Sonidos de cancha activados · swish en cada captura':'Sonidos silenciados');
};
$('#crowdToggle').onclick=()=>{
  crowdOn=!crowdOn;syncCrowdControls();
  try{localStorage.setItem('chessba-crowd',crowdOn?'on':'off');}catch{/* Optional preference. */}
  if(crowdOn&&soundOn){unlockSound();audio?.resume().then(()=>playCrowd(true)).catch(()=>{});}else stopCrowd();
  if(crowdOn&&!soundOn)toast('Tribuna activada. Activá también el sonido del tablero para escucharla.');
};
$('#crowdTest').onclick=()=>{unlockSound();audio?.resume().then(()=>playCrowd(true)).catch(()=>{});};
$('#crowdPreset').onchange=()=>{crowdPreset=$('#crowdPreset').value;stopCrowd();try{localStorage.setItem('chessba-crowd-preset',crowdPreset);}catch{/* Optional preference. */}};
$('#undo').onclick=()=>action('undo');$('#redo').onclick=()=>action('redo');
$('#draw').onclick=async()=>{if(await confirmAction('¿Tablas de común acuerdo?','Ambos jugadores deben estar de acuerdo. Confirmar termina la partida en empate.'))await action('draw');};
$('#resign').onclick=async()=>{if(await confirmAction('¿Querés rendirte?','El equipo que tiene el turno abandona y su rival gana la partida.'))await action('resign');};
document.querySelectorAll('[data-tab]').forEach(button=>button.onclick=()=>{document.querySelectorAll('[data-tab]').forEach(b=>b.classList.toggle('selected',b===button));$('#historyTab').hidden=button.dataset.tab!=='history';$('#benchTab').hidden=button.dataset.tab!=='bench';});
$('#newForm').addEventListener('change',()=>{
  const form=$('#newForm'),mode=new FormData(form).get('mode');
  $('#nbaOptions').hidden=mode==='standard';$('#customOptions').hidden=mode!=='custom';
  const limit=form.querySelector('[name=limit]');
  limit.disabled=mode==='standard'||mode==='theme'||(mode==='custom'&&!form.querySelector('[name=shotClock]').checked);
  $('#shotClockSettings').hidden=mode==='theme';
  $('#newError').textContent='';closeTeamMenus();updateTeamPreview();
});
$('#newForm').onsubmit=async event=>{
  event.preventDefault();if(busy)return;
  stopCrowd();
  unlockSound();
  const form=new FormData(event.target), params=Object.fromEntries(form.entries());
  for(const flag of ['shotClock','trade','hotHand','scoreboard'])params[flag]=String(form.has(flag));
  const button=event.target.querySelector('[type=submit]');button.disabled=true;
  try {busy=true;syncAvailability();state=await api('new',params);selected=null;flipped=false;render();connection(true);$('#newDialog').close();showView('game');playSound('start');toast('Nueva partida. ¡A jugar!');}
  catch(error){$('#newError').textContent=errorMessage(error);}
  finally{busy=false;button.disabled=false;syncAvailability();}
};
document.addEventListener('visibilitychange',()=>{if(document.hidden)stopCrowd();});
async function init() {
  renderControlIcons();
  syncSoundButton();
  syncCrowdControls();
  syncEffectsButton();
  syncAvailability();
  try {
    state=await api('state');
    if(typeof state.scoreboard!=='boolean'||!('result' in state))throw new Error('La aplicación se actualizó. Reiniciá el servidor Java y volvé a abrir la página.');
    connection(true);render();
    const franchises=await api('franchises');availableFranchises=franchises;
    for(const key of ['white','black']) { const select=$(`#${key}Franchise`);franchises.forEach(f=>{const option=text('option',f.name);option.value=f.code;select.append(option);});select.value=key==='white'?'LAL':'BOS'; }
    initializeTeamPickers();updateTeamPreview();setupReady=true;syncAvailability();
  } catch(error){toast(errorMessage(error));connection(false);$('#homeError').textContent=errorMessage(error);$('#turnTitle').textContent='Conectemos el tablero.';$('#turnCopy').textContent='Recargá cuando el servidor Java esté disponible.';}
}
init();
