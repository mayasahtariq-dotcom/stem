// ---------- Hand-drawn sticker icons (24×24, brown outline like the mascot) — used instead of emoji everywhere
const ICONS=(()=>{const o='stroke="#5a3d2e" stroke-width="1.7" stroke-linejoin="round" stroke-linecap="round"';const P=(d,f)=>`<path d="${d}" fill="${f}" ${o}/>`;return {
 home:P("M4 11 12 4l8 7v8.5a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 19.5Z","#ffd77a")+P("M10 21v-5.5h4V21","#f08a32")+P("M2.5 12 12 3.5 21.5 12","none"),
 quests:P("M6 4h12v16l-6-3.5L6 20Z","#7fd3f5")+P("M9 9h6M9 12.5h4","none"),
 book:P("M3 5.5c3-1.5 6-1.5 9 .5v14c-3-2-6-2-9-.5Z","#7fd3f5")+P("M21 5.5c-3-1.5-6-1.5-9 .5v14c3-2 6-2 9-.5Z","#bfe6f7"),
 shop:P("M4 10h16v10H4Z","#fff4e0")+P("M3 6h18l-1 4c-1 1.6-3 1.6-4 0-1 1.6-3 1.6-4 0-1 1.6-3 1.6-4 0-1 1.6-3 1.6-4 0Z","#f7889a")+P("M10 20v-5h4v5","#7fd3f5"),
 school:P("M3 10l9-6 9 6","#f08a32")+P("M5 9.5h14V20H5Z","#ffd77a")+P("M10 20v-5h4v5","#7fd3f5")+P("M12 4V1.8l3 1-3 1","#e2574c"),
 duck:`<ellipse cx="12" cy="13" rx="8.5" ry="7.5" fill="#fde68f" ${o}/><circle cx="9" cy="12" r="1.1" fill="#2a1d14"/><circle cx="15" cy="12" r="1.1" fill="#2a1d14"/>`+P("M10 15q2-1.4 4 0-.5 1.6-2 1.6t-2-1.6Z","#f7a23e")+P("M11.5 5.6q-.5-2 .8-2.6M12.7 5.6q1-1.8 2.2-1.6","none"),
 flame:P("M12 21c-4 0-6.5-2.6-6.5-6 0-4 3.5-6 4-10 2.5 1.5 4 4 4 6 1-1 1.5-2 1.5-3 2 2 3.5 4.3 3.5 7 0 3.4-2.5 6-6.5 6Z","#f7943a")+P("M12 21c-1.8 0-3-1.2-3-3 0-1.8 1.5-2.8 2-4.5 1.6 1 4 2.6 4 4.5 0 1.8-1.2 3-3 3Z","#ffd25a"),
 star:P("M12 3l2.6 5.4 5.9.8-4.3 4.1 1 5.9L12 16.4 6.8 19.2l1-5.9L3.5 9.2l5.9-.8Z","#ffd25a"),
 heart:P("M12 20s-7.5-4.6-7.5-10A4 4 0 0 1 12 7.8 4 4 0 0 1 19.5 10C19.5 15.4 12 20 12 20Z","#f7889a"),
 repeat:P("M5 11a7 7 0 0 1 12-4.5L19 8.5M19 4.5v4h-4","none")+P("M19 13a7 7 0 0 1-12 4.5L5 15.5M5 19.5v-4h4","none"),
 chat:P("M4 5h16v11H10l-4 3.5V16H4Z","#bfe6f7")+P("M8 9.5h8M8 12.5h5","none"),
 calendar:P("M4 6h16v14H4Z","#fff")+P("M4 6h16v4H4Z","#f7889a")+P("M8 3.5v4M16 3.5v4","none")+P("M8 14h2M12 14h2M16 14h0","none"),
 speaker:P("M4 9h4l5-4v14l-5-4H4Z","#ffd77a")+P("M16 9q2 3 0 6M18.5 6.5q4 5.5 0 11","none"),
 sprout:P("M12 21v-8","none")+P("M12 13C7 13 5 10 5 6c4 0 7 2.5 7 7Z","#8ee07a")+P("M12 11c0-4 2.5-6.5 7-6.5 0 4-2.5 6.5-7 6.5Z","#5cb046"),
 target:`<circle cx="12" cy="12" r="8.5" fill="#f7889a" ${o}/><circle cx="12" cy="12" r="5.2" fill="#fff" ${o}/><circle cx="12" cy="12" r="2" fill="#e2574c" ${o}/>`,
 people:`<circle cx="9" cy="8" r="3" fill="#ffd77a" ${o}/><circle cx="16.5" cy="9" r="2.5" fill="#7fd3f5" ${o}/>`+P("M3.5 19c0-3.5 2.5-6 5.5-6s5.5 2.5 5.5 6Z","#ffd77a")+P("M13.5 19c.3-3 1.5-5 3.5-5 2.5 0 4 2 4 5Z","#7fd3f5"),
 chart:P("M4 20h16","none")+P("M6 13h3v7H6Z","#7fd3f5")+P("M10.5 8h3v12h-3Z","#ffd25a")+P("M15 11h3v9h-3Z","#8ee07a"),
 puzzle:P("M5 7h4a2 2 0 1 1 4 0h4v4a2 2 0 1 1 0 4v4H5v-4a2 2 0 1 0 0-4Z","#c9b5f5"),
 snail:P("M3 18h15q3 0 3-3","none")+`<circle cx="11" cy="12" r="5.5" fill="#f7b267" ${o}/>`+P("M11 12m-2.5 0a2.5 2.5 0 1 0 5 0","none")+P("M17 18c1-2 1-5 1-7M20 18c.5-2 .3-4 0-6","none"),
 books:P("M4 5h4v15H4Z","#f7889a")+P("M8 6h4v14H8Z","#7fd3f5")+P("M12.5 6.5l3.6-1 3.8 13.8-3.6 1Z","#ffd25a"),
 raise:P("M12 21v-6M8 21c0-3 1.8-5 4-5s4 2 4 5Z","#7fd3f5")+`<circle cx="12" cy="10.5" r="2.8" fill="#ffd77a" ${o}/>`+P("M15.5 13.5 18 4","none")+`<circle cx="18.2" cy="3.6" r="1.6" fill="#ffd77a" ${o}/>`,
 lock:P("M5.5 11h13v9h-13Z","#ffd25a")+P("M8.5 11V8a3.5 3.5 0 0 1 7 0v3","none")+`<circle cx="12" cy="15.5" r="1.4" fill="#5a3d2e"/>`,
 trophy:P("M7 4h10v5a5 5 0 0 1-10 0Z","#ffd25a")+P("M7 6H4.5q0 4 3 4.5M17 6h2.5q0 4-3 4.5","none")+P("M12 14v3M8.5 20h7l-1-3h-5Z","#f7b267"),
 bowl:P("M3.5 11h17a8.5 8 0 0 1-17 0Z","#f7889a")+P("M8 11c0-2.5 1.5-4 4-4s4 1.5 4 4","#8ee07a"),
 gift:P("M4 10h16v4H4Z","#f7889a")+P("M5.5 14h13v6.5h-13Z","#ffb3c0")+P("M12 10v10.5","none")+P("M12 10C9 10 7 8.5 8 7s3 .5 4 3c1-2.5 3-4.5 4-3s-1 3-4 3Z","#ffd25a"),
 timer:P("M7 3h10M7 21h10M8 3c0 5 8 5 8 9s-8 4-8 9M16 3c0 5-8 5-8 9s8 4 8 9","none")+P("M10 19l2-2 2 2Z","#ffd25a"),
 party:P("M4 20l4-12 8 8Z","#ffd25a")+P("M14 4v2M19 9h2M17 6l1.5-1.5M13 9q2-1 2-3M15 11q1-2 3-2","none"),
 doc:P("M6 3h8l4 4v14H6Z","#fff")+P("M14 3v4h4","none")+P("M9 12h6M9 15h6M9 18h4","none"),
 image:P("M4 5h16v14H4Z","#bfe6f7")+P("M4 17l5-5 4 4 2.5-2.5L20 17v2H4Z","#8ee07a")+`<circle cx="16" cy="9" r="1.6" fill="#ffd25a" ${o}/>`,
 camera:P("M4 8h3l1.5-2h7L17 8h3v11H4Z","#f7a1b8")+`<circle cx="12" cy="13" r="3.5" fill="#bfe6f7" ${o}/>`,
 copy:P("M8 8h11v12H8Z","#fff")+P("M5 16V4h11","none"),
 refresh:P("M19 12a7 7 0 1 1-2-5M19 4v4h-4","none"),
 pencil:P("M5 19l1-4L16 5l3 3L9 18Z","#ffd25a")+P("M14 7l3 3","none")+P("M5 19l1-4 3 3Z","#f7b267"),
 trash:P("M6 7h12l-1 13H7Z","#fbd0cc")+P("M4.5 7h15M9.5 7V4.5h5V7M10 11v6M14 11v6","none"),
 warn:P("M12 3.5 21 19.5H3Z","#ffd25a")+P("M12 9.5v4.5","none")+`<circle cx="12" cy="17" r="1" fill="#5a3d2e"/>`,
 sparkle:P("M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8Z","#ffd25a")+P("M18.5 15l.7 1.8 1.8.7-1.8.7-.7 1.8-.7-1.8-1.8-.7 1.8-.7Z","#ffd25a"),
 plus:`<circle cx="12" cy="12" r="9" fill="#8ee07a" ${o}/>`+P("M12 7.5v9M7.5 12h9","none"),
 backpack:P("M6 9a6 6 0 0 1 12 0v11H6Z","#7fd3f5")+P("M9 5.5V4h6v1.5","none")+P("M8.5 14h7v4h-7Z","#ffd77a"),
 teacher:`<circle cx="9" cy="7" r="3" fill="#ffd77a" ${o}/>`+P("M3.5 20c0-4 2.5-7 5.5-7s5.5 3 5.5 7Z","#c9b5f5")+P("M13 4h8v7h-8Z","#8ee07a")+P("M12 13l3-3","none"),
 drop:P("M12 3.5c3 4 6 7 6 10.5a6 6 0 0 1-12 0C6 10.5 9 7.5 12 3.5Z","#7fd3f5"),
 seeds:`<circle cx="12" cy="12" r="4" fill="#8a5a3a" ${o}/>`+[0,45,90,135,180,225,270,315].map(a=>`<ellipse cx="12" cy="4.6" rx="2" ry="3" fill="#ffd25a" ${o} transform="rotate(${a} 12 12)"/>`).join("")+`<circle cx="12" cy="12" r="4" fill="#8a5a3a" ${o}/>`,
 peas:P("M3.5 15C6 8 14 5 20.5 8.5 18 15 10 18 3.5 15Z","#8ee07a")+`<circle cx="9" cy="12.5" r="1.8" fill="#5cb046" ${o}/><circle cx="12.6" cy="11.2" r="1.8" fill="#5cb046" ${o}/><circle cx="16.2" cy="10" r="1.8" fill="#5cb046" ${o}/>`,
 corn:P("M12 3c3 0 4.5 4 4.5 8.5S15 20 12 20s-4.5-4-4.5-8.5S9 3 12 3Z","#ffd25a")+P("M7.5 12c-2 3-1.5 6 1 8.5 1-3 2-5 3.5-6M16.5 12c2 3 1.5 6-1 8.5-1-3-2-5-3.5-6","#8ee07a")+P("M10 7h4M9.5 10h5","none"),
 melon:P("M3 9h18a9 9 0 0 1-18 0Z","#8ee07a")+P("M5 9h14a7 7 0 0 1-14 0Z","#f7889a")+`<circle cx="9" cy="12" r=".8" fill="#2a1d14"/><circle cx="12" cy="14" r=".8" fill="#2a1d14"/><circle cx="15" cy="12" r=".8" fill="#2a1d14"/>`,
 berry:P("M12 8c4 0 6.5 1.5 6.5 4.5S15.5 21 12 21s-6.5-5.5-6.5-8.5S8 8 12 8Z","#f7586a")+P("M8 8.5 12 5l4 3.5-4 1.5Z","#5cb046")+`<circle cx="10" cy="13" r=".7" fill="#ffd25a"/><circle cx="14" cy="13" r=".7" fill="#ffd25a"/><circle cx="12" cy="16.5" r=".7" fill="#ffd25a"/>`,
 cupcake:P("M6 13h12l-1.5 7.5h-9Z","#f7a1b8")+P("M5 13c0-4 3-6.5 7-6.5s7 2.5 7 6.5Z","#fff4e0")+`<circle cx="12" cy="5.5" r="1.8" fill="#e2574c" ${o}/>`,
 cake:P("M4 11 18 6.5V19H4Z","#fff4e0")+P("M4 14.5h14","none")+P("M4 11 18 6.5","none")+`<circle cx="13" cy="6" r="2" fill="#e2574c" ${o}/>`,
 flask:P("M9.5 3h5M10.5 3v5.5L5 19a1.5 1.5 0 0 0 1.3 2h11.4a1.5 1.5 0 0 0 1.3-2l-5.5-10.5V3","#fff")+P("M7 15h10l2 4.2a1.5 1.5 0 0 1-1.3 1.8H6.3A1.5 1.5 0 0 1 5 19.2Z","#8ee07a"),
 calc:P("M6 3h12v18H6Z","#c9b5f5")+P("M8.5 5.5h7v3.5h-7Z","#fff")+`${[[9,12.5],[12,12.5],[15,12.5],[9,16],[12,16],[15,16]].map(([x,y])=>`<circle cx="${x}" cy="${y}" r="1" fill="#5a3d2e"/>`).join("")}`,
 cowface:`<ellipse cx="12" cy="12.5" rx="8.5" ry="7.5" fill="#fffdf8" ${o}/>`+P("M14 6.5q3-1 4.5 1.5-1 2.5-3.5 2Z","#6e4a33")+`<circle cx="9" cy="10.5" r="1.1" fill="#2a1d14"/><circle cx="15" cy="10.5" r="1.1" fill="#2a1d14"/><ellipse cx="12" cy="15.5" rx="4.5" ry="3" fill="#f9cbbd" ${o}/>`+P("M6 6.5 4.5 3.8M18 6.5l1.5-2.7","none"),
 grass:P("M3 20c0-6 1-10 3-13 1 4 1.5 8 1.5 13M7.5 20c0-7 1.5-12 4.5-16 1.5 5 1.5 11 1 16M12 20c.5-6 2.5-10 5.5-12-.5 4-1 8-1.5 12M16 20c1-4 3-7 5-8-.5 3-1 6-1.5 8Z","#8ee07a"),
 hay:P("M5 9h14v10H5Z","#f2cf6b")+P("M5 12.5h14M5 16h14M8 9v10M16 9v10","none")+P("M7 9c0-3 2-4 5-4s5 1 5 4","#f2cf6b"),
 carrot:P("M17 7 6 20 4 18 15.5 5.5Z","#f7943a")+P("M15.5 5.5c0-2 1-3 2.5-3.5M17 7c2 0 3-1 3.5-2.5M16.5 6l3-3","none")+P("M9 14l2 2M11.5 11l1.8 1.8","none"),
 cane:P("M9 21 13 3h2.5L11.5 21Z","#c8e08a")+P("M10.3 15.5h2.8M11.6 9.5h2.8","none")+P("M14 4c2-2 5-2 6 0-2 1-4 1-6 0ZM13.5 5c-2-2-5-1-6 1 2 1 4 0 6-1Z","#5cb046"),
 gud:P("M5 10l7-4 7 4v7l-7 4-7-4Z","#b9783a")+P("M5 10l7 4 7-4M12 14v7","none"),
 leaf:P("M5 19C5 10 10 5 20 4c0 10-5 15-14 15Z","#8ee07a")+P("M5 19 14 10","none")
}})();
const ic=(n,s=22)=>n==="cowface"?cow("wave").replace('class="cow ',`width="${s}" height="${s}" class="ico cowico `):`<svg class="ico" width="${s}" height="${s}" viewBox="0 0 24 24" aria-hidden="true">${ICONS[n]||""}</svg>`;

// ---------- Chibi cow (cream with brown patches, one brown ear, tan horns, peach muzzle, tufted tail): round merged head+body, head tuft, flat orange beak, sticker outline.
// Each pose = item drawn in front of the cow. Older pose names map onto duck poses.
const POSES=["wave","heart","thumbs","hat","boba","flowers","sleep","question","rain","cool","teddy","cap"];
const POSE_NAMES={wave:"नमस्ते|ನಮಸ್ಕಾರ|Hello",heart:"प्यार|ಪ್ರೀತಿ|Love",thumbs:"शाबाश|ಭೇಷ್|Great job",hat:"घुमक्कड़|ಪ್ರವಾಸಿ|Explorer",boba:"मज़े|ಖುಷಿ|Treat time",flowers:"तुम्हारे लिए|ನಿನಗಾಗಿ|For you",sleep:"झपकी|ನಿದ್ದೆ|Nap",question:"हम्म?|ಹ್ಮ್?|Hmm?",rain:"बारिश|ಮಳೆ|Rainy day",cool:"कूल|ಕೂಲ್|Too cool",teddy:"दोस्त|ಗೆಳೆಯ|Best friend",cap:"स्कूल चलो|ಶಾಲೆಗೆ|School time"};
const ALIAS={happy:"wave",book:"cap",cake:"boba",cupcake:"boba",icecream:"boba",donut:"boba",camera:"thumbs",sprout:"flowers",plant:"flowers",paint:"flowers",yarn:"teddy",telescope:"cool",cloud:"rain"};
// ---------- Coded chibi cow (original SVG): squircle head, small pear body, glossy eyes, clipped spots, separate animated parts
let COW_N=0;
function cow(mood="wave",cls=""){
  mood=ALIAS[mood]||mood;
  const id="c"+(++COW_N);
  const O="#5b3a29",W="#fffaf2",SP="#8a5a44",PK="#f6b3b6",HF="#6e4733";
  const st=`stroke="${O}" stroke-width="3.6" stroke-linejoin="round" stroke-linecap="round"`;
  const HEAD="M100 24C150 24 176 46 176 84C176 122 148 142 100 142C52 142 24 122 24 84C24 46 50 24 100 24Z";
  const BODY="M62 128C52 160 54 196 74 204C90 209 110 209 126 204C146 196 148 160 138 128Z";
  const eye=x=>`<ellipse cx="${x}" cy="88" rx="9" ry="11.5" fill="#2a1b14"/><ellipse cx="${x}" cy="93" rx="6" ry="5" fill="#4a3024"/><circle cx="${x+3}" cy="83" r="3.8" fill="#fff"/><circle cx="${x-3.2}" cy="93.5" r="1.7" fill="#fff"/>`;
  const shut=(x,d=-7)=>`<path d="M${x-9} 90q9 ${d} 18 0" stroke="#2a1b14" stroke-width="4" fill="none" stroke-linecap="round"/>`;
  let eyes=["boba","eat","sleep"].includes(mood)?(mood==="sleep"?shut(74,6)+shut(126,6):shut(74)+shut(126)):eye(74)+eye(126);
  if(mood==="thumbs")eyes=eye(74)+`<path d="M134 82l-13 7 13 7" stroke="#2a1b14" stroke-width="4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>`;
  if(mood==="cool")eyes=`<path d="M46 84h108" stroke="#1d1d1d" stroke-width="4"/><path d="M50 82h44q0 24-22 24t-22-24zM106 82h44q0 24-22 24t-22-24z" fill="#1d1d1d"/><path d="M58 88h10" stroke="#fff" stroke-width="3" stroke-linecap="round" opacity=".55"/>`;
  if(mood==="sad")eyes+=`<path d="M62 70l16 5M138 70l-16 5" stroke="#2a1b14" stroke-width="3.4" stroke-linecap="round"/>`;
  if(mood==="mad")eyes+=`<path d="M60 68l20 9M140 68l-20 9" stroke="#2a1b14" stroke-width="5" stroke-linecap="round"/>`;
  const mouth=mood==="sad"||mood==="mad"?`<path d="M93 125q7-6 14 0" stroke="#7a3e30" stroke-width="3" fill="none" stroke-linecap="round"/>`
    :mood==="eat"||mood==="boba"?`<ellipse class="c-chew" cx="100" cy="123" rx="5.5" ry="3.6" fill="#d9584f"/>`
    :`<path d="M91 120q9 9 18 0" stroke="#7a3e30" stroke-width="3" fill="#e9776c" stroke-linecap="round" stroke-linejoin="round"/>`;
  const arm=(x,y,r)=>`<ellipse cx="${x}" cy="${y}" rx="11" ry="16" fill="${W}" ${st} transform="rotate(${r} ${x} ${y})"/>`;
  const hold=arm(70,160,40)+arm(130,160,-40);
  const spark=(x,y,s=1)=>`<path class="spark" transform="translate(${x} ${y}) scale(${s})" d="M0-11l3 8 8 3-8 3-3 8-3-8-8-3 8-3z" fill="#ffc23a"/>`;
  const fheart=(x,y)=>`<path class="float" d="M${x} ${y}c-9-6-12-9-12-13 0-4 4-6 7-3l5 4 5-4c3-3 7-1 7 3 0 4-3 7-12 13z" fill="#f7889a"/>`;
  const side=arm(56,152,25), sideR=arm(144,152,-25);
  const I={
    wave:`${side}<g class="c-arm">${arm(160,112,-150)}</g><g class="spark"><path d="M178 84l10-6M182 100h12M178 116l10 6" stroke="${O}" stroke-width="3.4" stroke-linecap="round"/></g>`,
    heart:`<g class="pulse"><path d="M100 196c-24-14-32-23-32-33 0-10 11-15 20-8l12 9 12-9c9-7 20-2 20 8 0 10-8 19-32 33z" fill="#f7889a" ${st}/><path d="M82 162q2-6 8-6" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" opacity=".7"/></g>${arm(72,166,40)}${arm(128,166,-40)}${fheart(182,46)}`,
    thumbs:`${side}<g class="c-arm"><rect x="138" y="140" width="22" height="22" rx="9" fill="${W}" ${st}/><rect x="143" y="121" width="10" height="24" rx="5" fill="${W}" ${st}/></g>${spark(178,86)}${spark(190,112,.6)}`,
    hat:`<path d="M64 146q36 24 72 20" stroke="${O}" stroke-width="3" fill="none"/>${side}${sideR}<g class="swing"><rect x="126" y="164" width="28" height="22" rx="7" fill="#ffd25a" ${st}/><circle cx="140" cy="174" r="3" fill="${O}"/></g><g class="tilt"><ellipse cx="100" cy="38" rx="86" ry="14" fill="#ffd25a" ${st}/><path d="M58 38q0-36 42-36t42 36z" fill="#ffd25a" ${st}/><path d="M60 32q40 8 80 0" stroke="#f0b93a" stroke-width="5" fill="none"/></g>`,
    boba:`<g class="sip"><rect x="87" y="146" width="26" height="36" rx="6" fill="#dcb88e" ${st}/><circle cx="94" cy="174" r="3" fill="${O}"/><circle cx="104" cy="176" r="3" fill="${O}"/><circle cx="100" cy="167" r="3" fill="${O}"/><path d="M104 146l4-22" stroke="#f08a32" stroke-width="5" stroke-linecap="round"/></g>${arm(80,166,15)}${arm(120,166,-15)}`,
    flowers:`${side}<g class="sway"><path d="M136 196l14-46M142 196l24-38" stroke="#4a9a3a" stroke-width="4" stroke-linecap="round"/><circle cx="150" cy="144" r="11" fill="#f7a1b8" ${st}/><circle cx="150" cy="144" r="4" fill="#ffc23a"/><circle cx="168" cy="152" r="10" fill="#ffd25a" ${st}/><circle cx="168" cy="152" r="4" fill="#f08a32"/></g>${arm(138,168,-10)}`,
    sleep:`${side}${sideR}<g class="zz" font-family="Rubik,sans-serif" font-weight="800" fill="#3b8de0"><text x="150" y="36" font-size="30">Z</text><text x="172" y="14" font-size="22">z</text><text x="188" y="-4" font-size="15">z</text></g>`,
    question:`${side}<g class="c-arm">${arm(128,140,-10)}</g><text class="bob2" x="164" y="40" font-family="Rubik,sans-serif" font-weight="800" font-size="42" fill="#3b8de0">?</text>`,
    rain:`<path d="M56 136h88l6 66q-50 12-100 0z" fill="#ffd25a" ${st}/><circle cx="100" cy="160" r="3" fill="${O}"/><circle cx="100" cy="178" r="3" fill="${O}"/><path d="M16 112q-4-104 84-104t84 104q-14-70-84-70t-84 70z" fill="#ffd25a" ${st}/>${arm(58,162,25).replace(W,"#ffd25a")}${arm(142,162,-25).replace(W,"#ffd25a")}${[[0,36],[190,24],[198,94],[-4,110]].map(([x,y],i)=>`<path class="drop" style="animation-delay:${i*.3}s" d="M${x} ${y}q7 11 0 15q-7-4 0-15z" fill="#5aaee8"/>`).join("")}`,
    cool:`<rect x="60" y="146" width="80" height="24" rx="12" fill="${W}" ${st}/>${spark(184,54,1.2)}`,
    teddy:`<g class="hug"><circle cx="86" cy="150" r="8" fill="#c48d5c" ${st}/><circle cx="110" cy="150" r="8" fill="#c48d5c" ${st}/><ellipse cx="98" cy="190" rx="17" ry="14" fill="#c48d5c" ${st}/><circle cx="98" cy="164" r="15" fill="#c48d5c" ${st}/><circle cx="92" cy="162" r="2.5" fill="${O}"/><circle cx="104" cy="162" r="2.5" fill="${O}"/><ellipse cx="98" cy="170" rx="5" ry="4" fill="#ebc79c"/></g>${arm(126,170,-60)}${fheart(184,104)}`,
    cap:`<path d="M138 146q28 12 18 48" stroke="#3b8de0" stroke-width="9" fill="none" stroke-linecap="round"/>${side}${sideR}<g class="tilt"><path d="M32 62q0-50 68-50t68 50q-68-16-136 0z" fill="#5aaee8" ${st}/><path d="M162 54q30-6 40 10-22 8-42 0z" fill="#5aaee8" ${st}/><circle cx="100" cy="14" r="5" fill="#3b8de0"/></g>`,
    sad:`${side}${sideR}<path class="drop" d="M146 96q7 11 0 15q-7-4 0-15z" fill="#5aaee8" stroke="#3d7fb8" stroke-width="1.5"/>`,
    mad:`${arm(60,146,60)}${arm(140,146,-60)}<g class="puff"><path d="M166 20l9 9M184 16l-4 13M192 32l-13 3" stroke="#e2574c" stroke-width="5.5" stroke-linecap="round"/></g>`,
    eat:`${hold}${[[86,134],[114,134],[100,140]].map(([x,y])=>`<circle class="drop" cx="${x}" cy="${y}" r="2.6" fill="#7cbf5a"/>`).join("")}${fheart(182,50)}`
  };
  return `<svg class="cow ${cls}" viewBox="-14 -14 228 248" aria-hidden="true"><defs>
    <radialGradient id="${id}h" cx=".38" cy=".3" r=".8"><stop offset="0" stop-color="#ffffff"/><stop offset=".7" stop-color="${W}"/><stop offset="1" stop-color="#f1e4d0"/></radialGradient>
    <linearGradient id="${id}m" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fcd2c2"/><stop offset="1" stop-color="#f3ab98"/></linearGradient>
    <clipPath id="${id}ch"><path d="${HEAD}"/></clipPath><clipPath id="${id}cb"><path d="${BODY}"/></clipPath></defs>
    <g class="pose p-${mood}">
    <ellipse cx="100" cy="214" rx="56" ry="7" fill="rgba(60,40,30,.12)"/>
    <g class="c-tail"><path d="M136 182q26 0 28-24" fill="none" ${st}/><path d="M164 160q-12-8-6-22 14 6 13 20z" fill="${SP}" ${st}/></g>
    <path d="${BODY}" fill="url(#${id}h)" ${st}/>
    <g clip-path="url(#${id}cb)"><path d="M134 138C150 148 152 172 140 182C126 178 122 156 134 138Z" fill="${SP}"/><path d="M60 170C72 164 82 174 80 188C70 196 58 188 60 170Z" fill="${SP}"/><ellipse cx="100" cy="132" rx="44" ry="9" fill="rgba(91,58,41,.13)"/></g>
    <path d="M74 182v20a8 8 0 0 0 8 8h4a8 8 0 0 0 8-8v-20" fill="${W}" ${st}/><path d="M106 182v20a8 8 0 0 0 8 8h4a8 8 0 0 0 8-8v-20" fill="${W}" ${st}/>
    <path d="M74 198v4a8 8 0 0 0 8 8h4a8 8 0 0 0 8-8v-4z" fill="${HF}"/><path d="M106 198v4a8 8 0 0 0 8 8h4a8 8 0 0 0 8-8v-4z" fill="${HF}"/>
    <g class="c-head">
      <path d="M64 36q-8-20 4-30 9 10 12 24z" fill="#f1cf8f" ${st}/><path d="M136 36q8-20-4-30-9 10-12 24z" fill="#f1cf8f" ${st}/>
      <g class="c-earl"><path d="M34 70C18 62 4 70 2 82c10 8 26 8 36 0z" fill="${W}" ${st}/><path d="M30 74C20 70 12 74 10 80c7 4 16 4 22 0z" fill="${PK}"/></g>
      <g class="c-earr"><path d="M166 70C182 62 196 70 198 82c-10 8-26 8-36 0z" fill="${W}" ${st}/><path d="M170 74C180 70 188 74 190 80c-7 4-16 4-22 0z" fill="${PK}"/></g>
      <path d="${HEAD}" fill="url(#${id}h)" ${st}/>
      <g clip-path="url(#${id}ch)"><path d="M116 22C150 18 176 40 176 68C154 74 136 64 126 48C120 38 114 30 116 22Z" fill="${SP}"/><path d="M24 92C36 84 52 90 52 104C50 116 36 120 24 112Z" fill="${SP}"/><circle cx="72" cy="48" r="6" fill="${SP}" opacity=".85"/></g>
      <path d="M88 32q6-10 12-4 6-8 12 2" fill="none" ${st} stroke-width="3"/>
      <g class="c-eyes">${eyes}</g>
      <ellipse cx="54" cy="110" rx="12" ry="7" fill="#f59a92" opacity=".5"/><ellipse cx="146" cy="110" rx="12" ry="7" fill="#f59a92" opacity=".5"/>
      <ellipse cx="100" cy="114" rx="34" ry="22" fill="url(#${id}m)"/><ellipse cx="94" cy="104" rx="12" ry="4" fill="#fff" opacity=".35"/>
      <ellipse cx="88" cy="110" rx="4" ry="5.5" fill="#b9685a"/><ellipse cx="112" cy="110" rx="4" ry="5.5" fill="#b9685a"/>
      ${mouth}
    </g>
    ${I[mood]||I.wave}
    </g></svg>`;
}


// ---------- Every line of UI text, in all three languages. {x} = filled in at runtime
const D={
  hello:["नमस्ते!","ನಮಸ್ಕಾರ!","Hello!"],
  app:["मूशाला","ಮೂಶಾಲೆ","Mooshala"],
  tag:["तुम्हारे नोट्स, तुम्हारी भाषा में पाठ","ನಿಮ್ಮ ನೋಟ್ಸ್, ನಿಮ್ಮ ಭಾಷೆಯಲ್ಲಿ ಪಾಠ","Your notes, turned into lessons in your language"],
  meet:["अपनी गाय से मिलो","ನಿಮ್ಮ ಹಸುವನ್ನು ಭೇಟಿ ಮಾಡಿ","Meet your cow"],
  haveKey:["मेरे पास क्लास की है","ನನ್ನ ಬಳಿ ತರಗತಿ ಕೀ ಇದೆ","I have a class key"],
  terms:["आगे बढ़कर तुम हमारी शर्तें और गोपनीयता नीति मानते हो","ಮುಂದುವರಿದರೆ ನೀವು ನಮ್ಮ ನಿಯಮ ಮತ್ತು ಗೌಪ್ಯತಾ ನೀತಿಗೆ ಒಪ್ಪುತ್ತೀರಿ","By continuing, you agree to our Terms and Privacy Policy"],
  cowAsk:["हेलो! मेरा अभी कोई नाम नहीं है। तुम मुझे क्या बुलाओगे?","ಹಲೋ! ನನಗೆ ಇನ್ನೂ ಹೆಸರಿಲ್ಲ. ನನ್ನನ್ನು ಏನೆಂದು ಕರೆಯುತ್ತೀಯ?","Hi! I don’t have a name yet. What will you call me?"],
  cowPh:["गाय का नाम","ಹಸುವಿನ ಹೆಸರು","Cow’s name"],
  myName:["यही मेरा नाम है!","ಇದೇ ನನ್ನ ಹೆಸರು!","That’s my name!"],
  youAsk:["वाह, मैं {cow} हूँ! अब तुम्हारा नाम क्या है?","ವಾಹ್, ನಾನು {cow}! ಈಗ ನಿನ್ನ ಹೆಸರೇನು?","I love it, I’m {cow}! What’s your name?"],
  youPh:["तुम्हारा नाम","ನಿನ್ನ ಹೆಸರು","Your name"],
  next:["आगे","ಮುಂದೆ","Next"],
  learnT:["चलो साथ में सीखें!","ಬನ್ನಿ, ಜೊತೆಗೆ ಕಲಿಯೋಣ!","Let’s learn together!"],
  learnS:["{cow} तुम्हारे टीचर के नोट्स को 5 मिनट के छोटे पाठों में बदल देती है।","{cow} ನಿಮ್ಮ ಶಿಕ್ಷಕರ ನೋಟ್ಸ್ ಅನ್ನು 5 ನಿಮಿಷದ ಸಣ್ಣ ಪಾಠಗಳಾಗಿ ಮಾಡುತ್ತದೆ.","{cow} turns your teacher’s notes into 5-minute lessons."],
  day1:["दिन 1","ದಿನ 1","Day 1"],
  wed:["बुधवार में स्वागत है!","ಬುಧವಾರಕ್ಕೆ ಸ್ವಾಗತ!","Welcome to Wednesday!"],
  thought:["आज का विचार","ಇಂದಿನ ಚಿಂತನೆ","Today’s thought"],
  quote:["गलती करना सीखने का हिस्सा है।","ತಪ್ಪು ಮಾಡುವುದು ಕಲಿಕೆಯ ಭಾಗ.","Mistakes are part of learning."],
  day1go:["एक पाठ पूरा करो ताकि {cow} नए मैदान में घूम सके!","ಒಂದು ಪಾಠ ಮುಗಿಸಿ, {cow} ಹೊಸ ಹುಲ್ಲುಗಾವಲಿಗೆ ಹೋಗಲಿ!","Finish one lesson so {cow} can graze in a new meadow!"],
  start:["आज शुरू करो","ಇಂದು ಶುರು ಮಾಡಿ","Start today"],
  walk:["पहली सैर","ಮೊದಲ ನಡಿಗೆ","1st meadow walk"],
  left:["आज {n} लक्ष्य बाकी!","ಇಂದು {n} ಗುರಿ ಬಾಕಿ!","{n} goals left today!"],
  g1:["आज का पाठ पूरा करो","ಇಂದಿನ ಪಾಠ ಮುಗಿಸಿ","Finish today’s lesson"],
  g2:["2 गलतियाँ दोहराओ","2 ತಪ್ಪುಗಳನ್ನು ಮತ್ತೆ ನೋಡಿ","Review 2 mistakes"],
  g3:["{cow} से एक सवाल पूछो","{cow}ಗೆ ಒಂದು ಪ್ರಶ್ನೆ ಕೇಳಿ","Ask {cow} one doubt"],
  tapCow:["{cow} को छूकर देखो!","{cow}ನ್ನು ಮುಟ್ಟಿ ನೋಡಿ!","Tap {cow}!"],
  seePath:["पाठ का रास्ता देखो","ಪಾಠದ ದಾರಿ ನೋಡಿ","See lesson path"],
  unit:["कक्षा 9 जीव विज्ञान · इकाई 1","9ನೇ ತರಗತಿ ಜೀವಶಾಸ್ತ್ರ · ಘಟಕ 1","Class 9 Biology · Unit 1"],
  cell:["कोशिका","ಕೋಶ","The Cell"],
  startTag:["शुरू","ಶುರು","Start"],
  back:["वापस","ಹಿಂದೆ","Back"],
  check:["जाँचो","ಪರಿಶೀಲಿಸಿ","Check"],
  cont:["आगे बढ़ो","ಮುಂದುವರಿಸಿ","Continue"],
  right:["सही जवाब!","ಸರಿಯಾದ ಉತ್ತರ!","Correct!"],
  wrong:["थोड़ा चूक गए। यह सवाल फिर आएगा।","ಸ್ವಲ್ಪ ತಪ್ಪಿತು. ಈ ಪ್ರಶ್ನೆ ಮತ್ತೆ ಬರುತ್ತದೆ.","Not quite. You’ll see this again."],
  newPose:["नया पोज़ मिला: {p}","ಹೊಸ ಭಂಗಿ ಸಿಕ್ಕಿತು: {p}","New pose unlocked: {p}"],
  simpler:["आसान भाषा में समझाओ","ಸುಲಭವಾಗಿ ವಿವರಿಸಿ","Explain simpler"],
  newWord:["नया शब्द","ಹೊಸ ಪದ","New word"],
  listen:["सुनो","ಕೇಳಿ","Listen"],
  fromNotes:["नोट्स पेज 3 से","ನೋಟ್ಸ್ ಪುಟ 3ರಿಂದ","From notes p.3"],
  streak:["दिन की लड़ी","ದಿನದ ಸರಣಿ","Day streak"],
  streakMsg:["+{xp} XP · {c}/3 सही। {cow} ऊर्जा से भरी है, {name}!","+{xp} XP · {c}/3 ಸರಿ. {cow} ಶಕ್ತಿಯಿಂದ ತುಂಬಿದೆ, {name}!","+{xp} XP · {c}/3 correct. {cow} is full of energy, {name}!"],
  letsgo:["चलो!","ಹೋಗೋಣ!","Let’s go!"],
  keyAsk:["टीचर से क्लास की लो, या बोर्ड पर QR स्कैन करो।","ಶಿಕ್ಷಕರಿಂದ ತರಗತಿ ಕೀ ಪಡೆಯಿರಿ, ಅಥವಾ ಬೋರ್ಡ್‌ನ QR ಸ್ಕ್ಯಾನ್ ಮಾಡಿ.","Ask your teacher for the class key, or scan the QR on the board."],
  join:["क्लास में जुड़ो","ತರಗತಿಗೆ ಸೇರಿ","Join class"],
  preview:["क्लास झलक","ತರಗತಿ ಮುನ್ನೋಟ","Class preview"],
  classInfo:["कक्षा 9 · जीव विज्ञान · राव मैडम · 38 छात्र","9ನೇ ತರಗತಿ · ಜೀವಶಾಸ್ತ್ರ · ರಾವ್ ಮೇಡಂ · 38 ವಿದ್ಯಾರ್ಥಿಗಳು","Class 9 · Biology · Mrs. Rao · 38 students"],
  dq:["आज के क्वेस्ट","ಇಂದಿನ ಕ್ವೆಸ್ಟ್‌ಗಳು","Daily quests"],
  season:["परीक्षा सीज़न","ಪರೀಕ್ಷಾ ಸೀಸನ್","Exam season"],
  seasonName:["गाय और चॉक","ಹಸು ಮತ್ತು ಚಾಕ್","Cows & Chalk"],
  unlock3:["3 दिन में खुलेगा","3 ದಿನದಲ್ಲಿ ತೆರೆಯುತ್ತದೆ","Unlocks in 3 days"],
  q1:["30 XP कमाओ","30 XP ಗಳಿಸಿ","Earn 30 XP"],
  q2:["लगातार 3 सही जवाब","ಸತತ 3 ಸರಿ ಉತ್ತರ","3 right answers in a row"],
  q3:["2 शब्द कार्ड सुनो","2 ಪದ ಕಾರ್ಡ್ ಕೇಳಿ","Listen to 2 word cards"],
  q4:["अपनी क्लास से जुड़ो","ನಿಮ್ಮ ತರಗತಿಗೆ ಸೇರಿ","Join your class"],
  classT:["क्लास के साथ सीखना ज़्यादा मज़ेदार है!","ತರಗತಿಯೊಂದಿಗೆ ಕಲಿಯುವುದು ಹೆಚ್ಚು ಖುಷಿ!","Learning is more fun with your class!"],
  board:["लीडरबोर्ड","ಲೀಡರ್‌ಬೋರ್ಡ್","Leaderboard"],
  prog:["मेरी प्रगति","ನನ್ನ ಪ್ರಗತಿ","My progress"],
  report:["सवाल रिपोर्ट करो","ಪ್ರಶ್ನೆ ವರದಿ ಮಾಡಿ","Report a question"],
  wa:["WhatsApp पर की भेजो","WhatsAppನಲ್ಲಿ ಕೀ ಕಳುಹಿಸಿ","Share key on WhatsApp"],
  baby:["नन्ही {cow}","ಪುಟ್ಟ {cow}","Baby {cow}"],
  cowOf:["{name} की गाय","{name}ರ ಹಸು","{name}’s cow"],
  classKey:["क्लास की","ತರಗತಿ ಕೀ","Class key"],
  dayStreak:["{n} दिन की लड़ी","{n} ದಿನದ ಸರಣಿ","{n} day streak"],
  best:["अब तक की सबसे लंबी!","ಇದುವರೆಗಿನ ಅತಿ ಉದ್ದ!","Longest ever!"],
  book:["{cow} की स्टिकर बुक","{cow}ನ ಸ್ಟಿಕರ್ ಪುಸ್ತಕ","{cow}’s sticker book"],
  bookMsg:["{n}/12 पोज़ · छूकर देखो, सही जवाब से और पाओ","{n}/12 ಭಂಗಿ · ಮುಟ್ಟಿ ನೋಡಿ, ಸರಿ ಉತ್ತರದಿಂದ ಇನ್ನಷ್ಟು","{n}/12 poses · tap one, earn more with right answers"],
  locked:["बंद","ಲಾಕ್","Locked"],
  changeLang:["भाषा बदलो","ಭಾಷೆ ಬದಲಿಸಿ","Change language"],
  tHome:["घर","ಮನೆ","Home"],tQuests:["क्वेस्ट","ಕ್ವೆಸ್ಟ್","Quests"],tLearn:["सीखो","ಕಲಿ","Learn"],tClass:["क्लास","ತರಗತಿ","Class"]
};
const LI={hi:0,kn:1,en:2};
const S={classes:["BIO9-K4X2"],ci:0,jp:null,role:null,screen:"lang",food:30,earned:0,bag:{water:1},feeding:false,tname:"",lang:null,langBack:null,unlocked:["wave","heart","cap"],pose:null,cowSet:false,cowPose:null,name:"",cow:"",i:0,pick:null,checked:false,xp:20,hearts:5,correct:0,done:0,lastPose:null};
const fill=(s,v={})=>s.replace(/\{(\w+)\}/g,(_,k)=>k in v?v[k]:k==="cow"?(S.cow||"Laddu"):k==="name"?(S.name||"Student"):"");
const esc=s=>String(s).replace(/[&<>"]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c]));
const R=(k,v)=>esc(fill(D[k][LI[S.lang||"hi"]],v));          // regional only
const E=(k,v)=>esc(fill(D[k][2],v));                          // English only
const T=(k,v)=>`<span class="reg">${R(k,v)}</span><span class="engl">${E(k,v)}</span>`; // both, stacked
const t=o=>`<span class="reg">${o[S.lang]}</span><span class="engl">${o.en}</span>`;
const PN=m=>{const p=POSE_NAMES[m].split("|");return {hi:p[0],kn:p[1],en:p[2]}};

// Example lesson content: Class 9 Biology, "The Cell" — sample data
const LESSON = [
  {q:{hi:"कोशिका का पावरहाउस कौन है?",kn:"ಕೋಶದ ಶಕ್ತಿಕೇಂದ್ರ ಯಾವುದು?",en:"Which is the powerhouse of the cell?"},
   opts:[{hi:"माइटोकॉन्ड्रिया",kn:"ಮೈಟೋಕಾಂಡ್ರಿಯಾ",en:"Mitochondria"},{hi:"केंद्रक",kn:"ನ್ಯೂಕ್ಲಿಯಸ್",en:"Nucleus"},{hi:"राइबोसोम",kn:"ರೈಬೋಸೋಮ್",en:"Ribosome"}],ans:0,
   why:{hi:"माइटोकॉन्ड्रिया भोजन से ऊर्जा (ATP) बनाता है — जैसे घर में बिजली का मीटर।",kn:"ಮೈಟೋಕಾಂಡ್ರಿಯಾ ಆಹಾರದಿಂದ ಶಕ್ತಿ (ATP) ತಯಾರಿಸುತ್ತದೆ.",en:"Mitochondria make energy (ATP) from food, like a generator for the cell."},page:"Notes p.3"},
  {q:{hi:"पौधों की कोशिका में कोशिका भित्ति होती है।",kn:"ಸಸ್ಯ ಕೋಶದಲ್ಲಿ ಕೋಶ ಭಿತ್ತಿ ಇರುತ್ತದೆ.",en:"Plant cells have a cell wall."},
   opts:[{hi:"सही",kn:"ಸರಿ",en:"True"},{hi:"गलत",kn:"ತಪ್ಪು",en:"False"}],ans:0,
   why:{hi:"कोशिका भित्ति पौधे को मज़बूती देती है, जैसे खेत की बाड़।",kn:"ಕೋಶ ಭಿತ್ತಿ ಸಸ್ಯಕ್ಕೆ ಬಲ ಕೊಡುತ್ತದೆ, ಹೊಲದ ಬೇಲಿಯಂತೆ.",en:"The cell wall gives plants strength, like a fence around a field."},page:"Notes p.4"},
  {q:{hi:"क्लोरोप्लास्ट किसमें पाया जाता है?",kn:"ಕ್ಲೋರೋಪ್ಲಾಸ್ಟ್ ಎಲ್ಲಿ ಕಂಡುಬರುತ್ತದೆ?",en:"Where are chloroplasts found?"},
   opts:[{hi:"जंतु कोशिका",kn:"ಪ್ರಾಣಿ ಕೋಶ",en:"Animal cells"},{hi:"पादप कोशिका",kn:"ಸಸ್ಯ ಕೋಶ",en:"Plant cells"},{hi:"दोनों",kn:"ಎರಡೂ",en:"Both"}],ans:1,
   why:{hi:"क्लोरोप्लास्ट धूप से भोजन बनाता है — यह सिर्फ़ पौधों में होता है।",kn:"ಕ್ಲೋರೋಪ್ಲಾಸ್ಟ್ ಸೂರ್ಯನ ಬೆಳಕಿನಿಂದ ಆಹಾರ ತಯಾರಿಸುತ್ತದೆ.",en:"Chloroplasts make food from sunlight, so only plants need them."},page:"Notes p.5"}
];
const $=document.getElementById("app");
const sm=(c,w)=>c.replace('class="cow ','class="cow '+(w||"")+' ');
function go(fn){S.screen=Object.keys(SCREENS).find(k=>SCREENS[k]===fn);$.innerHTML=fn()}
const btn=(k,attrs="",cls="",v)=>`<button class="btn ${cls}" ${attrs}><span class="reg">${R(k,v)}</span><small class="engl">${E(k,v)}</small></button>`;

const SCREENS={
lang:()=>`<div class="screen"><div class="grow">
  <p class="hello"><b>नमस्ते!</b> · <b>ನಮಸ್ಕಾರ!</b> · <b>Hello!</b></p>
  ${cow("question","big")}<div class="shadow"></div>
  <div class="bubble tri"><span class="reg">तुम किस भाषा में सहज हो?</span><span class="reg">ನಿಮಗೆ ಯಾವ ಭಾಷೆ ಸುಲಭ?</span><span class="engl">Which language are you comfortable with?</span></div>
  <div class="langs"><button class="opt big ${S.lang==="hi"?"sel":""}" data-l="hi"><b>हिंदी</b><small>हिंदी पहले, नीचे English · Hindi + English</small></button>
  <button class="opt big ${S.lang==="kn"?"sel":""}" data-l="kn"><b>ಕನ್ನಡ</b><small>ಕನ್ನಡ ಮೊದಲು, ಕೆಳಗೆ English · Kannada + English</small></button></div></div>
  <div class="foot"><button class="btn" id="lx" ${S.lang?"":"disabled"}>${S.lang?`<span class="reg">${R("next")}</span><small class="engl">Next</small>`:"आगे · ಮುಂದೆ · Next"}</button></div></div>`,
splash:()=>`<div class="screen"><div class="grow">${cow("wave","big")}<div class="shadow"></div><h1>${R("app")}</h1><p class="eng-title">Mooshala</p><p class="sub">${T("tag")}</p></div>
  <div class="foot">${btn("meet",'data-go="cowname"')}${btn("haveKey",'data-go="key"',"ghost")}
  <p class="fine">${T("terms")}</p></div></div>`,
cowname:()=>`<div class="screen"><div class="grow"><div class="bubble">${T("cowAsk")}</div>${cow(S.cowPose||"question")}<div class="shadow"></div>
  <input class="field" id="cn" placeholder="${R("cowPh")} · ${E("cowPh")}" value="${S.cowSet?esc(S.cow):""}" maxlength="14" autocomplete="off">
  <div class="chips">${(S.lang==="kn"?["ಲಡ್ಡು","ಚೀಕು","ಮೊಮೊ","ಗೋಲು","Laddu"]:["लड्डू","चीकू","मोमो","गोलू","Laddu"]).map(n=>`<button data-cn="${n}">${n}</button>`).join("")}</div></div>
  <div class="foot">${btn("myName",`id="cnx" ${S.cowSet?"":"disabled"}`)}</div></div>`,
name:()=>`<div class="screen"><div class="grow"><div class="bubble">${T("youAsk")}</div>${cow("heart")}<div class="shadow"></div>
  <input class="field" id="nm" placeholder="${R("youPh")} · ${E("youPh")}" value="${esc(S.name)}" autocomplete="off"></div>
  <div class="foot">${btn("next",`id="nx" ${S.name?"":"disabled"}`)}</div></div>`,
learn:()=>`<div class="screen"><div class="grow">${cow("sprout")}<div class="shadow"></div><h2>${T("learnT")}</h2><p class="sub">${T("learnS")}</p></div>
  <div class="foot">${btn("next",'data-go="day1"')}</div></div>`,
day1:()=>`<div class="screen sunrise">${sunriseBg()}<div class="grow">${cow("wave")}<p style="margin:0;font-weight:700;letter-spacing:.08em">${R("day1")} · ${E("day1").toUpperCase()}</p><h2>${T("wed")}</h2>
  <div class="polaroid"><div><small>${R("thought")} · ${E("thought").toUpperCase()}</small>${R("quote")}<br><span style="font-size:15px;opacity:.85;font-weight:500">${E("quote")}</span></div></div>
  <p style="font-size:19px;font-weight:700;margin:6px 0 0">${T("day1go")}</p></div>
  <div class="foot">${btn("start",'data-go="home"',"white")}</div></div>`,
home:()=>{const g=[[ic("book"),"g1","lesson",S.done],[ic("repeat"),"g2","",0],[ic("chat"),"g3","",0]];const left=g.filter(x=>!x[3]).length;
  return `<div class="scr"><div class="scene"><div class="sun"></div><div class="hill"></div><div class="trunk" style="left:40px"></div><div class="tree" style="left:3px"></div><div class="trunk" style="right:52px"></div><div class="tree" style="right:15px;background:#5bb04f"></div>
  <span class="petHint">${R("tapCow")}</span>${sm(cow(S.pose||(S.done?"heart":"wave")),"pet").replace("<svg",'<svg data-pet role="button" aria-label="Tap your cow"')}</div>
  <div class="ground green"><div class="adv"><div class="ic">${ic("sprout")}</div><div style="flex:1;display:flex;flex-direction:column;gap:6px;font-weight:700"><span>${R("walk")} <small class="engl inline">${E("walk")}</small></span><div class="pbar"><i style="width:${Math.min(100,S.xp/30*100)}%"></i><span>${S.xp} / 30 XP</span></div></div></div>
  <div style="font-weight:700;font-size:19px">${ic("calendar")} ${T("left",{n:left})}</div>
  ${g.map(x=>`<div class="goal"><div class="ic">${x[0]}</div><div class="tx">${T(x[1])}</div><button class="tick ${x[3]?"on":""}" ${x[2]&&!x[3]?`data-go="${x[2]}"`:""} aria-label="Start">${x[3]?"✓":"→"}</button></div>`).join("")}
  ${btn("seePath",'data-go="path"',"white")}</div>${tabs("home")}</div>`},
path:()=>{const cur=S.ci===0?(S.done?3:2):0;
  return `<div class="screen"><div class="hud"><span class="stat">${ic("flame")} ${S.done?1:0}</span><span class="stat">${ic("star")} ${S.xp} XP</span><span class="stat">${ic("heart")} ${S.hearts}</span></div>
  <div class="path"><div class="chiprow" style="align-self:stretch">${S.classes.map((k,i)=>`<button class="cchip ${i===S.ci?"on":""}" data-ci="${i}">${findClass(k).e} ${findClass(k).name[LI[S.lang]]}</button>`).join("")}</div>
  ${(()=>{const c=findClass(S.classes[S.ci]);return `<div class="unit"><small>${c.name[LI[S.lang]]} · ${esc(c.teacher)}</small><div style="font-size:20px;font-weight:700">${c.course[LI[S.lang]]} · ${c.course[2]}</div></div>`})()}
  ${[0,1,2,3,4,5].map(i=>{const cls=i<cur?"":i===cur?"cur":"lock";
    return `<button class="node ${cls}" style="margin-left:${[0,90,140,90,0,-90][i]}px" ${cls==="cur"?'data-go="lesson"':""}>${cls==="cur"?`<span class="tag">${R("startTag")}</span>${i+1}`:i<cur?"✓":i+1}</button>`}).join("")}
  ${cow(S.done?"sleep":"wave").replace('class="cow ','style="width:110px;align-self:flex-start;margin-top:-190px" class="cow ')}
  </div>${btn("back",'data-go="home"',"ghost")}</div>`},
lesson:()=>{const L=LESSON[S.i],ok=S.pick===L.ans;
  const pose=S.checked?(ok?["heart","thumbs","cool","flowers"][S.i%4]:["sad","question","mad"][S.i%3]):"question";
  return `<div class="screen"><div class="top"><button class="btn ghost" style="width:auto;padding:0;font-size:26px" data-go="path" aria-label="Close">✕</button><div class="bar"><i style="width:${(S.i+(S.checked?1:0))/LESSON.length*100}%"></i></div><span class="stat">${ic("heart")} ${S.hearts}</span></div>
  <div class="grow" style="justify-content:flex-start;padding-top:14px"><div style="display:flex;gap:10px;align-items:center;align-self:stretch">${sm(cow(pose),S.checked?(ok?"react":"shake"):"").replace('class="cow ','style="width:96px;flex:none" class="cow ')}
  <div class="bubble" style="font-size:18px">${t(L.q)}</div></div>
  <div class="opts">${L.opts.map((o,k)=>`<button class="opt ${S.checked?(k===L.ans?"right":k===S.pick?"wrong":""):k===S.pick?"sel":""}" data-k="${k}">${t(o)}</button>`).join("")}</div>
  ${S.checked?`<div class="feedback ${ok?"ok":"no"}"><b>${T(ok?"right":"wrong")}</b>${t(L.why)}${ok&&S.lastPose?`<small>${ic("party")} ${R("newPose",{p:PN(S.lastPose)[S.lang]})} · ${E("newPose",{p:PN(S.lastPose).en})}</small>`:""}<small style="opacity:.75">${ic("doc")} ${L.page} · <u>${R("simpler")} · ${E("simpler")}</u></small></div>`:""}</div>
  <div class="foot">${btn(S.checked?"cont":"check",`id="chk" ${S.pick===null?"disabled":""}`)}</div></div>`},
term:()=>`<div class="screen"><div class="grow"><span class="chip">${R("newWord")} · ${E("newWord")}</span>${cow("book")}
  <div class="card"><span class="term">${S.lang==="hi"?"माइटोकॉन्ड्रिया":"ಮೈಟೋಕಾಂಡ್ರಿಯಾ"}</span><span class="en">Mitochondria · ${S.lang==="hi"?"औपचारिक: सूत्रकणिका":"ಔಪಚಾರಿಕ: ತಂತುಕಣ"}</span>
  <span>${S.lang==="hi"?"कोशिका का बिजलीघर। जैसे गाँव का जनरेटर सबको बिजली देता है।":"ಕೋಶದ ವಿದ್ಯುತ್ ಕೇಂದ್ರ. ಊರಿನ ಜನರೇಟರ್ ಎಲ್ಲರಿಗೂ ವಿದ್ಯುತ್ ಕೊಡುವಂತೆ."}</span><span class="en">The cell’s power plant, like the village generator.</span>
  <small class="en">${ic("speaker")} ${R("listen")} · ${E("listen")} &nbsp; ${ic("doc")} ${R("fromNotes")} · ${E("fromNotes")}</small></div></div>
  <div class="foot">${btn("cont",'data-go="streak"')}</div></div>`,
streak:()=>`<div class="screen gold"><div class="rays"></div>${starsBg()}<div class="grow">${cow("cool","big")}<div class="shadow"></div><div class="num">1</div><h2 style="font-size:28px;font-weight:800">${T("streak")}</h2>
  <div class="week">${(S.lang==="hi"?["बु","गु","शु","श","र","सो","मं"]:["ಬು","ಗು","ಶು","ಶ","ಭಾ","ಸೋ","ಮಂ"]).map((d,i)=>`<div>${d}<span class="dot ${i===0?"done":""}">${i===0?"✓":""}</span></div>`).join("")}</div>
  <p style="font-size:17px;font-weight:700;margin:0">${T("streakMsg",{xp:S.xp,c:S.correct})}</p></div>
  <div class="foot">${btn("letsgo",'data-go="home"',"white")}</div></div>`,
key:()=>`<div class="screen"><div class="grow"><div class="bubble">${T("keyAsk")}</div>${cow("book")}<div class="shadow"></div>${joinBox()}</div>
  <div class="foot">${btn("back",'data-go="splash"',"ghost")}</div></div>`,
quests:()=>`<div class="scr"><div class="ground tan"><div class="season wkch"><small>${R("wkCh")} · ${E("wkCh")}</small><div style="display:flex;gap:12px;align-items:center;text-align:left">${cow("hat").replace('class="cow ','style="width:84px;flex:none" class="cow ')}<div style="flex:1;min-width:0"><b>${R("wkGoal")}</b><small class="engl">${E("wkGoal")}</small>
  <div class="pbar" style="margin-top:8px"><i style="width:${(S.done?1:0)/5*100}%;background:var(--gold)"></i><span>${S.done?1:0} / 5</span></div></div></div>
  <span class="ribbon">${ic("gift")} ${R("wkPrize")}</span><small class="engl">${E("wkPrize")} · ${ic("timer")} ${R("wkLeft")} · ${E("wkLeft")}</small></div>
  <div class="lab"><span>${R("dq")} · ${E("dq")}</span><span>⟳ 12:41</span></div>
  ${[[ic("sprout"),"q1",Math.min(S.xp,30),30],[ic("target"),"q2",S.correct,3],[ic("speaker"),"q3",S.done?1:0,2],[ic("people"),"q4",1,1]].map(q=>`<div class="goal"><div class="ic">${q[0]}</div><div class="tx">${T(q[1])}<div class="pbar" style="margin-top:6px;background:var(--soft)"><i style="width:${q[2]/q[3]*100}%;background:var(--gold)"></i><span>${q[2]} / ${q[3]}</span></div></div></div>`).join("")}
  </div>${tabs("quests","tan")}</div>`,
class:()=>`<div class="scr"><div class="ground green"><h2>${T("myCl")}</h2>
  ${S.classes.map((k,i)=>{const c=findClass(k);return `<div class="goal"><div class="ic">${c.e}</div><div class="tx">${N(c.name)}<small>${esc(c.teacher)} · ${R("students",{n:c.n})} · <b>${k}</b></small><small>${ic("books")} ${c.course[LI[S.lang]]} · ${c.course[2]}</small></div><button class="tick" data-ci="${i}" aria-label="Open">→</button></div>`}).join("")}
  ${joinBox()}</div>${tabs("class","green")}</div>`,
me:()=>{const pt=S.ptab||"about",col=S.unlocked.length;
  const body={about:`<div class="kv"><span>${R("favFood")}<small class="engl">${E("favFood")}</small></span><b>${ic("grass",20)} ${S.lang==="kn"?"ಹಸಿರು ಹುಲ್ಲು":"हरी घास"}</b></div><div class="kv"><span>${R("bornOn")}<small class="engl">${E("bornOn")}</small></span><b>21 Oct 2026</b></div><div class="kv"><span>${R("myCl")}<small class="engl">${E("myCl")}</small></span><b>${S.classes.length}</b></div>`,
    details:`<div class="kv"><span>${R("totalXp")}<small class="engl">${E("totalXp")}</small></span><b>${ic("star",20)} ${S.earned}</b></div><div class="kv"><span>${R("lessonsDone")}<small class="engl">${E("lessonsDone")}</small></span><b>${S.done?1:0}</b></div><div class="kv"><span>${R("fullness")}<small class="engl">${E("fullness")}</small></span><b>${S.food}%</b></div>`,
    traits:`<div class="traits">${[["जिज्ञासु","ಕುತೂಹಲಿ","Curious"],["घास की दीवानी","ಹುಲ್ಲು ಪ್ರಿಯೆ","Loves grass"],["सुबह जल्दी उठती","ಬೇಗ ಏಳುವ","Early riser"],["थोड़ी शर्मीली","ಸ್ವಲ್ಪ ನಾಚಿಕೆ","A bit shy"]].map(t=>`<span>${t[LI[S.lang]]}<small>${t[2]}</small></span>`).join("")}</div>`}[pt];
  return `<div class="scr"><div class="ground sand prof">
  <div class="ptop"><button class="sq" aria-label="Share">${ic("people",22)}</button><button class="sq" aria-label="Edit name" data-go="cowname">${ic("pencil",22)}</button></div>
  <div class="petcard"><div class="clip"></div><svg class="stamp" viewBox="0 0 100 100" aria-hidden="true"><circle cx="50" cy="50" r="40" fill="none" stroke="#f3d9c0" stroke-width="4"/><circle cx="50" cy="50" r="28" fill="none" stroke="#f3d9c0" stroke-width="3"/><path d="M0 40q25-10 50 0t50 0M0 56q25-10 50 0t50 0" stroke="#f3d9c0" stroke-width="3" fill="none"/></svg>
   <div class="prow"><div class="ptile">${cow(S.pose||"wave")}</div><div style="min-width:0"><h2>${R("baby")}</h2><p class="pmuted">${R("cowOf")}<br><small>${E("cowOf")}</small></p>
   <small class="plab">${R("stuCode")} · ${E("stuCode").toUpperCase()}</small><div class="code">LDU-7QX2 ${ic("copy",18)}</div></div></div>
   <div class="ptabs">${[["about","about"],["details","details"],["traits","traits"]].map(([k,d])=>`<button class="${pt===k?"on":""}" data-pt="${k}">${R(d)}<small>${E(d)}</small></button>`).join("")}</div>
   <div class="pbody">${body}</div></div>
  <div class="streakc"><div class="badge">${ic("star",30)}</div><div style="flex:1;min-width:0"><b class="big">${S.done?1:0}</b> <b>${R("dayStreakW")}</b> <small class="engl inline">${E("dayStreakW")}</small><div class="pmuted">${R("best")} · ${E("best")}</div></div><span class="chev">›</span></div>
  <h3 class="colh">${R("collection")} <small class="engl inline">${E("collection")}</small></h3>
  <div class="notebook"><div class="rings"><i></i><i></i><i></i><i></i><i></i></div><div class="nbpage"><div class="nbtitle">${R("poses")} <small class="engl inline">${E("poses")}</small></div>
   <div class="stick">${POSES.map(m=>{const u=S.unlocked.includes(m);return `<button class="${u?"":"lk"}" ${u?`data-pose="${m}"`:""}>${cow(m)}${u?`<span>${PN(m)[S.lang]}</span>`:`<span>${R("locked")}</span>`}</button>`}).join("")}</div></div>
   <div class="nbfoot">${col} / ${POSES.length} <span class="chev2">›</span></div></div>
  ${btn("changeLang",'id="chl"',"white")}${btn("switchT",'id="toT"',"ghost")}</div>${tabs("me","sand")}</div>`}
};

function tabs(on,bg="green"){const c={green:"#7cbf5a;color:#fff",tan:"#b3825a;color:#fff",sand:"#ecdcb4;color:#6b4a3a"}[bg];
  return `<nav class="tabs" style="background:${c}">${[["home",ic("home"),"tHome"],["quests",ic("backpack"),"tQuests"],["path",ic("book"),"tLearn"],["shop",ic("shop"),"tShop"],["class",ic("teacher"),"tClass"]].map(x=>`<button class="${x[0]===on?"on":""}" data-go="${x[0]}"><span>${x[1]}</span>${R(x[2])}<small>${E(x[2])}</small></button>`).join("")}<button class="${on==="me"?"on":""}" data-go="me"><span>${ic("cowface")}</span>${esc(S.cow||"Laddu")}<small>&nbsp;</small></button></nav>`}

function confetti(){const c=["#ffc23a","#55b752","#f6a9b3","#1499e0","#f08a32"];const host=$.firstElementChild;for(let i=0;i<48;i++){const e=document.createElement("i");e.className="confetti";e.style.left=Math.random()*100+"%";e.style.background=c[i%5];e.style.animationDelay=Math.random()*.6+"s";e.style.animationDuration=1.8+Math.random()+"s";host.appendChild(e)}}

$.addEventListener("input",e=>{
  if(e.target.id==="cn"){const v=e.target.value.trim();S.cowSet=!!v;S.cow=v;document.getElementById("cnx").disabled=!v}
  if(e.target.id==="nm"){S.name=e.target.value.trim();document.getElementById("nx").disabled=!S.name}
});
$.addEventListener("click",e=>{
  const pet=e.target.closest("[data-pet]");if(pet){const u=S.unlocked;S.pose=u[(u.indexOf(S.pose)+1)%u.length];go(SCREENS.home);$.querySelector(".pet").classList.add("react");return}
  const b=e.target.closest("button");if(!b||b.disabled)return;
  if(b.dataset.go){go(SCREENS[b.dataset.go]);if(b.dataset.go==="streak")confetti();return}
  if(b.dataset.l){S.lang=b.dataset.l;go(SCREENS.lang)}
  if(b.id==="lx"){const back=S.langBack;S.langBack=null;go(SCREENS[back||"role"])}
  if(b.id==="chl"){S.langBack="me";go(SCREENS.lang)}
  if(b.dataset.cn){S.cow=b.dataset.cn;S.cowSet=true;S.cowPose="heart";go(SCREENS.cowname)}
  if(b.id==="cnx")go(SCREENS.name);
  if(b.id==="nx")go(SCREENS.learn);
  if(b.dataset.pose){S.pose=b.dataset.pose;go(SCREENS.me)}
  if(b.dataset.k!==undefined&&!S.checked){S.pick=+b.dataset.k;go(SCREENS.lesson)}
  if(b.id==="chk"){
    if(!S.checked){S.checked=true;const ok=S.pick===LESSON[S.i].ans;S.lastPose=null;
      if(ok){S.xp+=10;S.earned+=10;S.correct++;const p=["thumbs","flowers","boba","teddy","cool","hat","sleep","question","rain"].find(x=>!S.unlocked.includes(x));if(p){S.unlocked.push(p);S.lastPose=p}}else S.hearts--;
      go(SCREENS.lesson);if(ok)confetti()}
    else{S.i++;S.pick=null;S.checked=false;if(S.i<LESSON.length)go(SCREENS.lesson);else{S.done=1;go(SCREENS.term)}}
  }
});

// ================= Food, mood, meadow, role choice and teacher mode =================
Object.assign(D,{
  roleQ:["तुम कौन हो?","ನೀವು ಯಾರು?","Who are you?"],
  student:["मैं छात्र हूँ","ನಾನು ವಿದ್ಯಾರ್ಥಿ","I’m a student"],studentSub:["पाठ खेलो, अपनी गाय पालो","ಪಾಠ ಆಡಿ, ನಿಮ್ಮ ಹಸು ಸಾಕಿ","Play lessons, raise your cow"],
  teacher:["मैं शिक्षक हूँ","ನಾನು ಶಿಕ್ಷಕ","I’m a teacher"],teacherSub:["नोट्स डालो, क्लास बनाओ, प्रगति देखो","ನೋಟ್ಸ್ ಹಾಕಿ, ತರಗತಿ ಮಾಡಿ, ಪ್ರಗತಿ ನೋಡಿ","Add notes, build classes, track progress"],
  tShop:["दुकान","ಅಂಗಡಿ","Shop"],
  fullness:["पेट","ಹೊಟ್ಟೆ","Fullness"],
  mHappy:["{cow} बहुत खुश है!","{cow} ತುಂಬಾ ಖುಷಿಯಾಗಿದೆ!","{cow} is very happy!"],
  mOk:["{cow} ठीक है","{cow} ಚೆನ್ನಾಗಿದೆ","{cow} is doing okay"],
  mSad:["{cow} को भूख लगी है…","{cow}ಗೆ ಹಸಿವಾಗಿದೆ…","{cow} is hungry…"],
  mMad:["{cow} भूख से नाराज़ है! खाना दो!","{cow} ಹಸಿವಿನಿಂದ ಸಿಟ್ಟಾಗಿದೆ! ಆಹಾರ ಕೊಡಿ!","{cow} is grumpy with hunger! Feed now!"],
  feed:["खिलाओ","ತಿನ್ನಿಸಿ","Feed"],emptyBag:["थैला खाली है। दुकान से खाना खरीदो","ಚೀಲ ಖಾಲಿ. ಅಂಗಡಿಯಿಂದ ಆಹಾರ ಕೊಳ್ಳಿ","Bag is empty. Buy food in the shop"],
  shopT:["{cow} की दुकान","{cow}ನ ಅಂಗಡಿ","{cow}’s shop"],
  shopS:["XP खर्च करके खाना खरीदो। ज़्यादा XP कमाने पर नया खाना खुलता है।","XP ಖರ್ಚು ಮಾಡಿ ಆಹಾರ ಕೊಳ್ಳಿ. ಹೆಚ್ಚು XP ಗಳಿಸಿದರೆ ಹೊಸ ಆಹಾರ ತೆರೆಯುತ್ತದೆ.","Spend XP on food. Earning more XP unlocks new food."],
  wallet:["खर्च करने के लिए","ಖರ್ಚಿಗೆ","To spend"],totalXp:["कुल कमाया","ಒಟ್ಟು ಗಳಿಕೆ","Total earned"],
  buy:["खरीदो","ಕೊಳ್ಳಿ","Buy"],free:["मुफ़्त","ಉಚಿತ","Free"],
  unlockAt:["{n} XP पर खुलेगा","{n} XP ನಲ್ಲಿ ತೆರೆಯುತ್ತದೆ","Unlocks at {n} XP"],
  noXp:["XP कम है। एक पाठ खेलो!","XP ಸಾಕಾಗಿಲ್ಲ. ಒಂದು ಪಾಠ ಆಡಿ!","Not enough XP. Play a lesson!"],
  bought:["थैले में डाला: {f}","ಚೀಲಕ್ಕೆ ಹಾಕಲಾಗಿದೆ: {f}","Added to bag: {f}"],
  inBag:["थैले में {n}","ಚೀಲದಲ್ಲಿ {n}","{n} in bag"],
  yum:["म्म्म! {cow} ने {f} खाया","ಮ್ಮ್ಮ್! {cow} {f} ತಿಂತು","Yum! {cow} ate {f}"],
  switchT:["शिक्षक मोड","ಶಿಕ್ಷಕ ಮೋಡ್","Teacher mode"],switchS:["छात्र मोड","ವಿದ್ಯಾರ್ಥಿ ಮೋಡ್","Student mode"],
  tnameQ:["नमस्ते शिक्षक! आपका नाम क्या है?","ನಮಸ್ಕಾರ ಶಿಕ್ಷಕರೇ! ನಿಮ್ಮ ಹೆಸರೇನು?","Welcome, teacher! What’s your name?"],
  tnamePh:["जैसे: राव मैडम","ಉದಾ: ರಾವ್ ಮೇಡಂ","e.g. Mrs. Rao"],
  hiT:["नमस्ते, {t}","ನಮಸ್ಕಾರ, {t}","Hello, {t}"],
  dash:["आज की झलक","ಇಂದಿನ ನೋಟ","Today at a glance"],
  active:["आज सक्रिय","ಇಂದು ಸಕ್ರಿಯ","Active today"],acc:["औसत सटीकता","ಸರಾಸರಿ ನಿಖರತೆ","Avg accuracy"],
  behind:["पीछे चल रहे","ಹಿಂದೆ ಇರುವವರು","Falling behind"],lessonsDone:["पाठ पूरे","ಮುಗಿದ ಪಾಠ","Lessons done"],
  week:["इस हफ़्ते की गतिविधि","ಈ ವಾರದ ಚಟುವಟಿಕೆ","Activity this week"],
  weak:["सबसे कमज़ोर विषय","ಅತಿ ದುರ್ಬಲ ವಿಷಯಗಳು","Weakest topics"],
  behindL:["जिन्हें मदद चाहिए","ಸಹಾಯ ಬೇಕಾದವರು","Students who need help"],
  assignRev:["कमज़ोर विषयों पर दोहराई भेजो","ದುರ್ಬಲ ವಿಷಯಗಳ ಪುನರಾವರ್ತನೆ ಕಳುಹಿಸಿ","Assign revision on weak topics"],
  revSent:["दोहराई 6 छात्रों को भेजी गई","6 ವಿದ್ಯಾರ್ಥಿಗಳಿಗೆ ಪುನರಾವರ್ತನೆ ಕಳುಹಿಸಲಾಗಿದೆ","Revision assigned to 6 students"],
  sample:["उदाहरण डेटा","ಉದಾಹರಣೆ ಡೇಟಾ","Example data"],
  tDash:["डैशबोर्ड","ಡ್ಯಾಶ್‌ಬೋರ್ಡ್","Dashboard"],tClasses:["क्लास","ತರಗತಿಗಳು","Classes"],tAdd:["नोट्स","ನೋಟ್ಸ್","Add notes"],tCourses:["कोर्स","ಕೋರ್ಸ್","Courses"],
  myClasses:["मेरी क्लास","ನನ್ನ ತರಗತಿಗಳು","My classes"],students:["{n} छात्र","{n} ವಿದ್ಯಾರ್ಥಿಗಳು","{n} students"],
  newKey:["नई की","ಹೊಸ ಕೀ","New key"],copyWa:["WhatsApp संदेश कॉपी करो","WhatsApp ಸಂದೇಶ ನಕಲಿಸಿ","Copy WhatsApp message"],copied:["कॉपी हो गया","ನಕಲಿಸಲಾಗಿದೆ","Copied"],
  createClass:["नई क्लास बनाओ","ಹೊಸ ತರಗತಿ ಮಾಡಿ","Create a class"],classPh:["जैसे: 8B विज्ञान","ಉದಾ: 8B ವಿಜ್ಞಾನ","e.g. 8B Science"],create:["बनाओ","ಮಾಡಿ","Create"],
  created:["क्लास बन गई। की: {k}","ತರಗತಿ ಸಿದ್ಧ. ಕೀ: {k}","Class created. Key: {k}"],
  addT:["नोट्स से कोर्स बनाओ","ನೋಟ್ಸ್‌ನಿಂದ ಕೋರ್ಸ್ ಮಾಡಿ","Turn notes into a course"],
  addS:["फ़ोटो, PDF या टाइप किए नोट्स डालो। हाथ की लिखावट, चित्र और सूत्र भी चलेंगे।","ಫೋಟೋ, PDF ಅಥವಾ ಟೈಪ್ ಮಾಡಿದ ನೋಟ್ಸ್ ಹಾಕಿ. ಕೈಬರಹ, ಚಿತ್ರ, ಸೂತ್ರಗಳೂ ಸರಿ.","Add photos, PDFs or typed notes. Handwriting, diagrams and formulas work too."],
  drop:["फ़ोटो या PDF चुनो (कई पेज)","ಫೋಟೋ ಅಥವಾ PDF ಆರಿಸಿ (ಹಲವು ಪುಟ)","Choose photos or PDFs (several pages)"],
  typeN:["या नोट्स यहाँ टाइप करो…","ಅಥವಾ ನೋಟ್ಸ್ ಇಲ್ಲಿ ಟೈಪ್ ಮಾಡಿ…","Or type notes here…"],
  useSample:["उदाहरण नोट्स लो","ಉದಾಹರಣೆ ನೋಟ್ಸ್ ಬಳಸಿ","Use sample notes"],
  make:["कोर्स बनाओ","ಕೋರ್ಸ್ ಮಾಡಿ","Make course"],
  gen1:["लिखावट पढ़ रहे हैं","ಕೈಬರಹ ಓದುತ್ತಿದೆ","Reading handwriting"],gen2:["विषय और कठिनाई पहचान रहे हैं","ವಿಷಯ ಮತ್ತು ಕಠಿಣತೆ ಗುರುತಿಸುತ್ತಿದೆ","Detecting subject and difficulty"],
  gen3:["विषय → पाठ → सवाल बना रहे हैं","ವಿಷಯ → ಪಾಠ → ಪ್ರಶ್ನೆ ಮಾಡುತ್ತಿದೆ","Building topics → lessons → questions"],gen4:["पाठ 1 तैयार!","ಪಾಠ 1 ಸಿದ್ಧ!","Lesson 1 ready!"],
  genWait:["{cow} आपके नोट्स पढ़ रही है…","{cow} ನಿಮ್ಮ ನೋಟ್ಸ್ ಓದುತ್ತಿದೆ…","{cow} is reading your notes…"],
  review:["छात्रों के देखने से पहले जाँचें","ವಿದ್ಯಾರ್ಥಿಗಳು ನೋಡುವ ಮೊದಲು ಪರಿಶೀಲಿಸಿ","Review before students see it"],
  detected:["पहचाना: विज्ञान · कक्षा 7 · आसान","ಗುರುತಿಸಲಾಗಿದೆ: ವಿಜ್ಞಾನ · 7ನೇ ತರಗತಿ · ಸುಲಭ","Detected: Science · Class 7 · Easy"],
  building:["बन रहा है… छात्र पहुँचेंगे तब तैयार","ಸಿದ್ಧವಾಗುತ್ತಿದೆ… ವಿದ್ಯಾರ್ಥಿಗಳು ತಲುಪುವಾಗ","Building… ready when students reach it"],
  flag:["नोट्स में साफ़ नहीं था, कृपया जाँचें","ನೋಟ್ಸ್‌ನಲ್ಲಿ ಸ್ಪಷ್ಟವಿರಲಿಲ್ಲ, ಪರಿಶೀಲಿಸಿ","Unclear in notes, please check"],
  edit:["बदलो","ಬದಲಿಸಿ","Edit"],done:["हो गया","ಆಯಿತು","Done"],regen:["फिर से बनाओ","ಮತ್ತೆ ಮಾಡಿ","Regenerate"],del:["हटाओ","ಅಳಿಸಿ","Delete"],
  answer:["उत्तर","ಉತ್ತರ","Answer"],
  assignTo:["किस क्लास को दें?","ಯಾವ ತರಗತಿಗೆ ಕೊಡಬೇಕು?","Assign to which class?"],due:["अंतिम तारीख (वैकल्पिक)","ಕೊನೆಯ ದಿನಾಂಕ (ಐಚ್ಛಿಕ)","Due date (optional)"],
  publish:["प्रकाशित करो और भेजो","ಪ್ರಕಟಿಸಿ ಮತ್ತು ಕಳುಹಿಸಿ","Publish and assign"],
  published:["कोर्स क्लास को भेज दिया गया","ಕೋರ್ಸ್ ತರಗತಿಗೆ ಕಳುಹಿಸಲಾಗಿದೆ","Course assigned to the class"],
  coursesT:["मेरे कोर्स","ನನ್ನ ಕೋರ್ಸ್‌ಗಳು","My courses"],live:["चालू","ಚಾಲ್ತಿ","Live"],draft:["ड्राफ़्ट","ಕರಡು","Draft"],
  completion:["{n}% ने पूरा किया","{n}% ಪೂರ್ಣಗೊಳಿಸಿದ್ದಾರೆ","{n}% completed"]
});

// ---- Food: cost and unlock are in XP. fill = how much it fills the tummy
const FOODS=[
  {id:"water",e:ic("drop"),n:["ताज़ा पानी","ತಾಜಾ ನೀರು","Fresh water"],cost:0,unlock:0,fill:8},
  {id:"grass",e:ic("grass"),n:["हरी घास","ಹಸಿರು ಹುಲ್ಲು","Fresh grass"],cost:5,unlock:0,fill:15},
  {id:"hay",e:ic("hay"),n:["सूखा चारा","ಒಣ ಹುಲ್ಲು","Hay bundle"],cost:10,unlock:10,fill:25},
  {id:"carrot",e:ic("carrot"),n:["गाजर","ಕ್ಯಾರೆಟ್","Carrots"],cost:15,unlock:20,fill:30},
  {id:"melon",e:ic("melon"),n:["तरबूज़","ಕಲ್ಲಂಗಡಿ","Watermelon"],cost:20,unlock:30,fill:40},
  {id:"corn",e:ic("corn"),n:["भुट्टा","ಮೆಕ್ಕೆಜೋಳ","Corn"],cost:20,unlock:40,fill:40},
  {id:"cane",e:ic("cane"),n:["गन्ना","ಕಬ್ಬು","Sugarcane"],cost:25,unlock:60,fill:50},
  {id:"gud",e:ic("gud"),n:["गुड़ (खास!)","ಬೆಲ್ಲ (ವಿಶೇಷ!)","Jaggery (treat!)"],cost:30,unlock:80,fill:60}
];
const FN=f=>f.n[LI[S.lang]];
const moodKey=()=>S.food>=70?"happy":S.food>=40?"ok":S.food>=20?"sad":"mad";
const moodPose={happy:"heart",ok:"wave",sad:"sad",mad:"mad"};

function toast(k,v){const el=document.createElement("div");el.className="toast";el.innerHTML=T(k,v);$.firstElementChild.appendChild(el);setTimeout(()=>el.remove(),2400)}

// ---- Layered meadow: sky, drifting clouds, turning sun, three hill bands, barn, trees, fence, pond, flowers, butterflies
function meadow(){
  const fl=[[30,262,"#f7a1b8"],[70,250,"#ffd25a"],[118,276,"#fff"],[170,258,"#f7a1b8"],[236,272,"#ffd25a"],[280,256,"#fff"],[330,270,"#f7a1b8"],[372,254,"#ffd25a"],[210,286,"#fff"],[150,290,"#ffd25a"]];
  return `<svg class="bg" viewBox="0 0 400 300" preserveAspectRatio="xMidYMax slice" aria-hidden="true">
  <defs><linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#8fd3f4"/><stop offset="1" stop-color="#e3f6fd"/></linearGradient></defs>
  <rect width="400" height="300" fill="url(#sky)"/>
  <g class="sunr" style="transform-origin:330px 52px">${[0,30,60,90,120,150].map(a=>`<rect x="326" y="12" width="8" height="80" rx="4" fill="#ffe08a" opacity=".55" transform="rotate(${a} 330 52)"/>`).join("")}</g>
  <circle cx="330" cy="52" r="24" fill="#ffd25a"/>
  <g class="cloud c1"><ellipse cx="70" cy="50" rx="34" ry="13" fill="#fff"/><ellipse cx="88" cy="42" rx="20" ry="14" fill="#fff"/><ellipse cx="56" cy="44" rx="14" ry="10" fill="#fff"/></g>
  <g class="cloud c2"><ellipse cx="220" cy="32" rx="26" ry="10" fill="#fff" opacity=".9"/><ellipse cx="234" cy="26" rx="14" ry="10" fill="#fff" opacity=".9"/></g>
  <path d="M0 150Q60 104 130 136T260 128T400 132V300H0Z" fill="#bfe39a"/>
  <path d="M0 176Q90 136 190 168T400 160V300H0Z" fill="#9fd477"/>
  <g transform="translate(286 132)"><rect x="0" y="22" width="62" height="46" fill="#d9534f" stroke="#8a3b36" stroke-width="3"/><path d="M-6 24L31 -4L68 24Z" fill="#9c3f3a" stroke="#6e2b27" stroke-width="3"/><rect x="21" y="38" width="20" height="30" fill="#fff4e6" stroke="#8a3b36" stroke-width="3"/><path d="M21 38L41 68M41 38L21 68" stroke="#8a3b36" stroke-width="2.5"/><circle cx="31" cy="12" r="5" fill="#fff4e6" stroke="#8a3b36" stroke-width="2"/></g>
  <g><rect x="34" y="140" width="10" height="56" rx="3" fill="#8a6650"/><circle cx="39" cy="128" r="30" fill="#3c9a55"/><circle cx="20" cy="144" r="18" fill="#4fae5f"/><circle cx="58" cy="146" r="18" fill="#4fae5f"/><circle cx="30" cy="120" r="5" fill="#e2574c"/><circle cx="50" cy="138" r="5" fill="#e2574c"/></g>
  <g><rect x="372" y="160" width="8" height="40" rx="3" fill="#8a6650"/><circle cx="376" cy="150" r="22" fill="#5bb04f"/></g>
  <path d="M0 214Q200 190 400 214V300H0Z" fill="#7cbf5a"/>
  <g stroke="#b58a5a" stroke-width="5" stroke-linecap="round">${[100,140,180,220,260].map(x=>`<path d="M${x} 214v-28"/>`).join("")}<path d="M96 194h168M96 206h168" stroke-width="4"/></g>
  <ellipse cx="66" cy="262" rx="52" ry="14" fill="#7cc8ec" stroke="#5aaee8" stroke-width="3"/><ellipse class="ripple" cx="66" cy="262" rx="30" ry="6" fill="none" stroke="#fff" stroke-width="2" opacity=".7"/>
  <ellipse cx="84" cy="258" rx="9" ry="4" fill="#4fae5f"/><circle cx="84" cy="256" r="3" fill="#f7a1b8"/>
  ${fl.map(([x,y,c])=>`<g><path d="M${x} ${y+8}v-8" stroke="#3d8a30" stroke-width="2"/><circle cx="${x}" cy="${y}" r="4.5" fill="${c}" stroke="#7a5644" stroke-width="1"/><circle cx="${x}" cy="${y}" r="1.6" fill="#f08a32"/></g>`).join("")}
  ${[[20,236],[130,240],[300,236],[360,288],[250,296],[190,232]].map(([x,y])=>`<path d="M${x} ${y}q2-10 4 0q2-12 4 0q2-9 4 0" stroke="#4e9a3a" stroke-width="2.5" fill="none" stroke-linecap="round"/>`).join("")}
  <g class="bfly b1"><ellipse cx="0" cy="0" rx="6" ry="4" fill="#f7a1b8" class="wing"/><ellipse cx="0" cy="0" rx="1.5" ry="4" fill="#5a3d2e"/></g>
  <g class="bfly b2"><ellipse cx="0" cy="0" rx="5" ry="3.5" fill="#ffd25a" class="wing"/><ellipse cx="0" cy="0" rx="1.4" ry="3.5" fill="#5a3d2e"/></g>
  </svg>`;
}

Object.assign(SCREENS,{
role:()=>`<div class="screen"><div class="grow">${cow("question","big")}<div class="shadow"></div><h2>${T("roleQ")}</h2>
  <div class="langs"><button class="opt big" data-role="student"><b>${ic("backpack")} ${R("student")}</b><small>${E("student")} · ${R("studentSub")}</small></button>
  <button class="opt big" data-role="teacher"><b>${ic("teacher")} ${R("teacher")}</b><small>${E("teacher")} · ${R("teacherSub")}</small></button></div></div></div>`,
home:()=>{const m=moodKey(),g=[[ic("book"),"g1","lesson",S.done],[ic("repeat"),"g2","",0],[ic("chat"),"g3","",0]],left=g.filter(x=>!x[3]).length;
  const pose=S.feeding?"eat":(m==="sad"||m==="mad")?moodPose[m]:(S.pose||moodPose[m]);
  const bag=FOODS.filter(f=>S.bag[f.id]>0);
  return `<div class="scr"><div class="scene2">${meadow()}
  <div class="meter"><span>${ic("bowl")} ${R("fullness")} <small>${E("fullness")}</small></span><div class="fbar"><i class="food-i m-${m}" style="width:${S.food}%"></i></div></div>
  <div class="moodb m-${m}">${T({happy:"mHappy",ok:"mOk",sad:"mSad",mad:"mMad"}[m])}</div>
  <div class="cowspot">${sm(cow(pose),"pet").replace("<svg",'<svg data-pet role="button" aria-label="Tap your cow"')}<div class="shadow"></div></div></div>
  <div class="ground green">
  <div class="feedbar">${bag.length?bag.map(f=>`<button class="food" data-feed="${f.id}"><span class="fe">${f.e}</span><small>${FN(f)}</small><b>×${S.bag[f.id]}</b></button>`).join(""):`<p style="margin:0">${T("emptyBag")}</p>`}<button class="food shopb" data-go="shop"><span class="fe">${ic("shop")}</span><small>${R("tShop")}</small><b>${ic("star")}${S.xp}</b></button></div>
  <div style="font-weight:700;font-size:18px">${ic("calendar")} ${T("left",{n:left})}</div>
  ${g.map(x=>`<div class="goal"><div class="ic">${x[0]}</div><div class="tx">${T(x[1])}</div><button class="tick ${x[3]?"on":""}" ${x[2]&&!x[3]?`data-go="${x[2]}"`:""} aria-label="Start">${x[3]?"✓":"→"}</button></div>`).join("")}
  ${btn("seePath",'data-go="path"',"white")}</div>${tabs("home")}</div>`},
shop:()=>`<div class="scr"><div class="ground sand"><div class="shophead">${cow(moodPose[moodKey()]).replace('class="cow ','style="width:92px;flex:none" class="cow ')}<div><h2>${R("shopT")}</h2><p style="margin:4px 0 0;font-size:14px">${T("shopS")}</p></div></div>
  <div class="wallet"><div><small>${R("wallet")} · ${E("wallet")}</small><b>${ic("star")} ${S.xp} XP</b></div><div><small>${R("totalXp")} · ${E("totalXp")}</small><b>${ic("trophy")} ${S.earned} XP</b></div></div>
  <div class="shopgrid">${FOODS.map(f=>{const lock=S.earned<f.unlock;return `<div class="item ${lock?"lk":""}"><span class="fe">${lock?ic("lock"):f.e}</span><b>${FN(f)}</b><small class="engl">${f.n[2]}</small>
    <div class="fbar sm"><i style="width:${f.fill}%"></i></div>${S.bag[f.id]?`<small>${R("inBag",{n:S.bag[f.id]})}</small>`:""}
    ${lock?`<small class="need">${R("unlockAt",{n:f.unlock})}<br>${E("unlockAt",{n:f.unlock})}</small>`:`<button class="buy" data-buy="${f.id}">${f.cost?`${ic("star")} ${f.cost} · ${R("buy")}`:R("free")}</button>`}</div>`}).join("")}</div>
  </div>${tabs("shop","sand")}</div>`,

// ---------------- Teacher mode ----------------
tname:()=>`<div class="screen teach"><div class="grow">${cow("sprout")}<div class="shadow"></div><div class="bubble">${T("tnameQ")}</div>
  <input class="field" id="tn" placeholder="${R("tnamePh")} · ${E("tnamePh")}" value="${esc(S.tname)}" autocomplete="off"></div>
  <div class="foot">${btn("next",`id="tnx" ${S.tname?"":"disabled"}`)}</div></div>`,
tdash:()=>{const c=TS.classes[TS.cur];
  return `<div class="scr teach">${thead(R("hiT",{t:esc(S.tname||"Mrs. Rao")}),E("hiT",{t:esc(S.tname||"Mrs. Rao")}))}
  <div class="tbody"><div class="chiprow">${TS.classes.map((k,i)=>`<button class="cchip ${i===TS.cur?"on":""}" data-cls="${i}">${esc(k.name)}</button>`).join("")}</div>
  <div class="lab2"><span>${T("dash")}</span><span class="pill">${R("sample")} · ${E("sample")}</span></div>
  <div class="tiles">
   <div class="tile"><span class="tic">${ic("raise")}</span><small>${R("active")}<br><span class="engl">${E("active")}</span></small><b>${c.active}<em>/${c.n}</em></b></div>
   <div class="tile"><span class="tic">${ic("target")}</span><small>${R("acc")}<br><span class="engl">${E("acc")}</span></small><b>${c.acc}%</b></div>
   <div class="tile warn"><span class="tic">${ic("snail")}</span><small>${R("behind")}<br><span class="engl">${E("behind")}</span></small><b>${c.behind}</b></div>
   <div class="tile"><span class="tic">${ic("books")}</span><small>${R("lessonsDone")}<br><span class="engl">${E("lessonsDone")}</span></small><b>${c.lessons}</b></div></div>
  <div class="tcard c-sky"><h3>${ic("chart")} ${T("week")}</h3><div class="wk">${c.week.map((v,i)=>`<div><i style="height:${v}%"></i><small>${(S.lang==="kn"?["ಸೋ","ಮಂ","ಬು","ಗು","ಶು","ಶ","ಭಾ"]:["सो","मं","बु","गु","शु","श","र"])[i]}</small></div>`).join("")}</div></div>
  <div class="tcard c-peach"><h3>${ic("puzzle")} ${T("weak")}</h3>${c.weak.map(w=>`<div class="wrow"><span>${w[LI[S.lang]]}<small class="engl">${w[2]}</small></span><div class="tbar"><i style="width:${w[3]}%;background:${w[3]<60?"var(--red)":w[3]<70?"var(--gold)":"var(--green)"}"></i></div><b>${w[3]}%</b></div>`).join("")}</div>
  <div class="tcard c-lilac"><h3>${ic("people")} ${T("behindL")}</h3>${c.kids.map(k=>`<div class="krow"><span class="av">${k[0][0]}</span><span style="flex:1">${k[0]}<small class="engl">${k[1]}</small></span><b class="${k[2]<55?"bad":""}">${k[2]}%</b></div>`).join("")}
  ${btn("assignRev",'id="rev"')}</div>
  </div>${ttabs("tdash")}</div>`},
tclasses:()=>`<div class="scr teach">${thead(R("myClasses"),E("myClasses"))}<div class="tbody">
  ${TS.classes.map((c,i)=>`<div class="tcard"><div style="display:flex;gap:12px;align-items:center"><div class="qr">${qr(c.key)}</div><div style="flex:1;min-width:0"><h3 style="margin:0">${esc(c.name)}</h3><small>${R("students",{n:c.n})} · ${E("students",{n:c.n})}</small><div class="tkey">${c.key}</div></div></div>
   <div class="row2"><button class="mini" data-wa="${i}">${ic("copy")} ${R("copyWa")}</button><button class="mini" data-nk="${i}">${ic("refresh")} ${R("newKey")}</button></div></div>`).join("")}
  <div class="tcard"><h3>${T("createClass")}</h3><input class="field sm" id="ncl" placeholder="${R("classPh")} · ${E("classPh")}" autocomplete="off">${btn("create",'id="mkc"')}</div>
  </div>${ttabs("tclasses")}</div>`,
tadd:()=>`<div class="scr teach">${thead(R("addT"),E("addT"))}<div class="tbody">
  <p style="margin:0">${T("addS")}</p>
  <label class="dropz" for="files"><span style="font-size:34px">${ic("camera")} ${ic("doc")}</span>${T("drop")}<input id="files" type="file" accept="image/*,application/pdf" multiple hidden></label>
  ${TS.files.length?`<div class="tcard">${TS.files.map(f=>`<div class="krow"><span class="av">${/pdf$/i.test(f)?ic("doc"):ic("image")}</span><span style="flex:1;min-width:0;overflow-wrap:anywhere">${esc(f)}</span></div>`).join("")}</div>`:""}
  <textarea id="ntext" class="field sm" rows="4" placeholder="${R("typeN")} · ${E("typeN")}">${esc(TS.text)}</textarea>
  <button class="mini" id="samp">${ic("sparkle")} ${R("useSample")} · ${E("useSample")}</button>
  ${btn("make",`id="mk" ${TS.files.length||TS.text.trim()?"":"disabled"}`)}
  </div>${ttabs("tadd")}</div>`,
tgen:()=>`<div class="screen teach"><div class="grow">${cow("question","big")}<h2>${T("genWait")}</h2>
  <div class="steps">${["gen1","gen2","gen3","gen4"].map((k,i)=>`<div class="stp ${i<TS.step?"ok":i===TS.step?"now":""}"><span>${i<TS.step?"✓":i===TS.step?"…":"○"}</span>${T(k)}</div>`).join("")}</div></div></div>`,
treview:()=>`<div class="scr teach">${thead(R("review"),E("review"))}<div class="tbody">
  <div class="tcard"><h3 style="margin:0">${S.lang==="kn"?"ದ್ಯುತಿಸಂಶ್ಲೇಷಣೆ":"प्रकाश संश्लेषण"} · Photosynthesis</h3><small>${R("detected")} · ${E("detected")}</small></div>
  <div class="topic"><b>1. ${S.lang==="kn"?"ಸಸ್ಯಗಳಿಗೆ ಏನು ಬೇಕು":"पौधों को क्या चाहिए"} · What plants need</b><span class="pill ok">${R("gen4")}</span></div>
  ${TS.qs.map((q,i)=>`<div class="tcard q ${q.flag?"flagged":""}">${q.flag?`<div class="flagm">${ic("warn")} ${R("flag")} · ${E("flag")} (${q.flag})</div>`:""}
    <span class="chip">${q.type}</span><div class="qt" ${TS.editing===i?'contenteditable="true"':""} data-qi="${i}">${t(q.q)}</div>
    <small>${R("answer")}: <b>${q.a}</b></small>
    <div class="row2"><button class="mini" data-ed="${i}">${ic("pencil")} ${R(TS.editing===i?"done":"edit")}</button><button class="mini" data-rg="${i}">${ic("refresh")} ${R("regen")}</button><button class="mini red" data-dl="${i}">${ic("trash")} ${R("del")}</button></div></div>`).join("")}
  <div class="topic dim"><b>2. ${S.lang==="kn"?"ಪತ್ರಹರಿತ್ತು ಮತ್ತು ಎಲೆಗಳು":"क्लोरोफ़िल और पत्तियाँ"} · Chlorophyll & leaves</b><small>${R("building")}</small></div>
  <div class="topic dim"><b>3. ${S.lang==="kn"?"ಗ್ಲೂಕೋಸ್ ಮತ್ತು ಆಮ್ಲಜನಕ":"ग्लूकोज़ और ऑक्सीजन"} · Glucose & oxygen</b><small>${R("building")}</small></div>
  <div class="tcard"><h3>${T("assignTo")}</h3>${TS.classes.map((c,i)=>`<label class="radio"><input type="radio" name="ac" value="${i}" ${i===TS.assign?"checked":""}> ${esc(c.name)}</label>`).join("")}
   <label style="font-weight:700;font-size:14px">${R("due")} · ${E("due")}<input id="due" type="date" class="field sm"></label>${btn("publish",'id="pub"')}</div>
  </div>${ttabs("tadd")}</div>`,
tcourses:()=>`<div class="scr teach">${thead(R("coursesT"),E("coursesT"))}<div class="tbody">
  ${TS.courses.map(c=>`<div class="tcard"><div style="display:flex;justify-content:space-between;gap:8px"><h3 style="margin:0">${c.t[LI[S.lang]]}<small class="engl">${c.t[2]}</small></h3><span class="pill ${c.live?"ok":""}">${R(c.live?"live":"draft")}</span></div>
   <small>${esc(c.cls)} · ${R("completion",{n:c.pct})} · ${E("completion",{n:c.pct})}</small><div class="tbar"><i style="width:${c.pct}%;background:var(--green)"></i></div></div>`).join("")}
  ${btn("addT",'data-go="tadd"')}</div>${ttabs("tcourses")}</div>`
});

// ---- Teacher sample data (clearly marked "Example data" on screen)
const TS={cur:0,files:[],text:"",step:0,editing:null,assign:0,
  classes:[
   {name:"9A Biology",key:"BIO9-K4X2",n:38,active:31,acc:74,behind:6,lessons:212,week:[40,62,80,55,90,30,20],
    weak:[["कोशिकांग","ಕೋಶಾಂಗಗಳು","Cell organelles",52],["परासरण","ಆಸ್ಮೋಸಿಸ್","Osmosis",61],["पादप बनाम जंतु कोशिका","ಸಸ್ಯ ಮತ್ತು ಪ್ರಾಣಿ ಕೋಶ","Plant vs animal cell",70]],
    kids:[["Aarav S.","2 days inactive",48],["Priya K.","Struggling with Osmosis",53],["Rohan M.","Missed 3 lessons",58],["Divya N.","Slow on diagrams",62]]},
   {name:"9B Biology",key:"BIO9-M7Q1",n:35,active:22,acc:68,behind:9,lessons:150,week:[30,45,50,60,40,20,10],
    weak:[["परासरण","ಆಸ್ಮೋಸಿಸ್","Osmosis",49],["कोशिका भित्ति","ಕೋಶ ಭಿತ್ತಿ","Cell wall",63],["केंद्रक","ನ್ಯೂಕ್ಲಿಯಸ್","Nucleus",72]],
    kids:[["Kiran R.","4 days inactive",41],["Sneha P.","Low accuracy",50],["Arjun T.","Missed 2 lessons",57]]}],
  courses:[{t:["कोशिका","ಕೋಶ","The Cell"],cls:"9A Biology",pct:72,live:true},{t:["ऊतक","ಅಂಗಾಂಶ","Tissues"],cls:"9B Biology",pct:35,live:true}],
  qs:[]};
const QS0=[
 {type:"MCQ",q:{hi:"पौधे भोजन बनाने के लिए क्या लेते हैं?",kn:"ಸಸ್ಯಗಳು ಆಹಾರ ತಯಾರಿಸಲು ಏನು ತೆಗೆದುಕೊಳ್ಳುತ್ತವೆ?",en:"What do plants take in to make food?"},a:"CO₂ + H₂O + light",
  alt:{hi:"प्रकाश संश्लेषण के लिए किन तीन चीज़ों की ज़रूरत है?",kn:"ದ್ಯುತಿಸಂಶ್ಲೇಷಣೆಗೆ ಯಾವ ಮೂರು ವಸ್ತುಗಳು ಬೇಕು?",en:"Which three things does photosynthesis need?"}},
 {type:"Fill the formula",q:{hi:"6CO₂ + 6H₂O → C₆H₁₂O₆ + 6 __",kn:"6CO₂ + 6H₂O → C₆H₁₂O₆ + 6 __",en:"6CO₂ + 6H₂O → C₆H₁₂O₆ + 6 __"},a:"O₂",
  alt:{hi:"प्रकाश संश्लेषण में कौन सी गैस निकलती है? __",kn:"ದ್ಯುತಿಸಂಶ್ಲೇಷಣೆಯಲ್ಲಿ ಯಾವ ಅನಿಲ ಬಿಡುಗಡೆ ಆಗುತ್ತದೆ? __",en:"Which gas is released in photosynthesis? __"}},
 {type:"True / False",flag:"Notes p.2",q:{hi:"प्रकाश संश्लेषण रात में होता है।",kn:"ದ್ಯುತಿಸಂಶ್ಲೇಷಣೆ ರಾತ್ರಿ ನಡೆಯುತ್ತದೆ.",en:"Photosynthesis happens at night."},a:"False",
  alt:{hi:"प्रकाश संश्लेषण के लिए धूप ज़रूरी है।",kn:"ದ್ಯುತಿಸಂಶ್ಲೇಷಣೆಗೆ ಸೂರ್ಯನ ಬೆಳಕು ಅಗತ್ಯ.",en:"Photosynthesis needs sunlight."}}];

function thead(r,e){return `<header class="thead"><svg class="tdeco" viewBox="0 0 400 140" preserveAspectRatio="xMidYMid slice" aria-hidden="true"><circle cx="360" cy="10" r="60" fill="#ffffff" opacity=".08"/><circle cx="40" cy="150" r="70" fill="#ffffff" opacity=".07"/>${[[300,30],[230,110],[120,24],[380,100]].map(([x,y],i)=>`<path class="spark" style="animation-delay:${i*.3}s" transform="translate(${x} ${y}) scale(.7)" d="M0-11l3 8 8 3-8 3-3 8-3-8-8-3 8-3z" fill="#ffd25a"/>`).join("")}</svg>${cow(S.screen==="tdash"?"heart":S.screen==="tadd"||S.screen==="treview"?"paint":S.screen==="tclasses"?"camera":"book").replace('class="cow ','style="width:64px;flex:none;position:relative" class="cow ')}<div style="flex:1;min-width:0;position:relative"><b>${r}</b><small>${e}</small></div><button class="mini dark" id="toS">${ic("backpack")} ${R("switchS")}</button></header>`}
function ttabs(on){return `<nav class="tabs ttabs">${[["tdash",ic("chart"),"tDash"],["tclasses",ic("school"),"tClasses"],["tadd",ic("plus"),"tAdd"],["tcourses",ic("books"),"tCourses"]].map(x=>`<button class="${x[0]===on?"on":""}" data-go="${x[0]}"><span>${x[1]}</span>${R(x[2])}<small>${E(x[2])}</small></button>`).join("")}</nav>`}
// Decorative QR-style pattern from the key (a real build would draw a scannable QR code)
function qr(k){let h=0;for(const ch of k)h=(h*31+ch.charCodeAt(0))>>>0;let o="";for(let i=0;i<81;i++){h=(h*1103515245+12345)>>>0;const r=Math.floor(i/9),c=i%9,corner=(r<3&&c<3)||(r<3&&c>5)||(r>5&&c<3);o+=`<i class="${corner?((r%2+c%2)!==1?"on":""):(h>>16)&1?"on":""}"></i>`}return o}
const rkey=p=>p+"-"+Array.from({length:4},()=>"ABCDEFGHJKLMNPQRSTUVWXYZ23456789"[Math.random()*32|0]).join("");

// ---- Interactions for food, role and teacher mode
$.addEventListener("input",e=>{
  if(e.target.id==="tn"){S.tname=e.target.value.trim();document.getElementById("tnx").disabled=!S.tname}
  if(e.target.id==="ntext"){TS.text=e.target.value;document.getElementById("mk").disabled=!(TS.files.length||TS.text.trim())}
});
$.addEventListener("change",e=>{
  if(e.target.id==="files"){TS.files=TS.files.concat([...e.target.files].map(f=>f.name));go(SCREENS.tadd)}
  if(e.target.name==="ac")TS.assign=+e.target.value;
});
$.addEventListener("click",e=>{
  const b=e.target.closest("button");if(!b||b.disabled)return;const d=b.dataset;
  if(d.role){S.role=d.role;go(SCREENS[d.role==="teacher"?(S.tname?"tdash":"tname"):"splash"])}
  if(b.id==="tnx")go(SCREENS.tdash);
  if(b.id==="toS"){S.role="student";go(SCREENS[S.cowSet?"home":"splash"])}
  if(b.id==="toT"){S.role="teacher";go(SCREENS[S.tname?"tdash":"tname"])}
  if(d.buy){const f=FOODS.find(x=>x.id===d.buy);if(S.xp<f.cost){toast("noXp");return}S.xp-=f.cost;S.bag[f.id]=(S.bag[f.id]||0)+1;go(SCREENS.shop);toast("bought",{f:FN(f)})}
  if(d.feed){const f=FOODS.find(x=>x.id===d.feed);S.bag[f.id]--;S.food=Math.min(100,S.food+f.fill);S.feeding=true;S.pose=null;go(SCREENS.home);
    const sc=$.querySelector(".scene2"),fly=document.createElement("span");fly.className="fly";fly.innerHTML=f.e;sc.appendChild(fly);
    for(let i=0;i<5;i++){const h=document.createElement("span");h.className="heartp";h.innerHTML=ic("heart",22);h.style.left=40+Math.random()*20+"%";h.style.animationDelay=.5+i*.15+"s";sc.appendChild(h)}
    toast("yum",{f:FN(f)});clearTimeout(S.ft);S.ft=setTimeout(()=>{S.feeding=false;if(S.screen==="home")go(SCREENS.home)},2200)}
  if(d.cls){TS.cur=+d.cls;go(SCREENS.tdash)}
  if(b.id==="rev")toast("revSent");
  if(d.wa){const c=TS.classes[+d.wa],msg=`Join my class "${c.name}" on Mooshala! Key: ${c.key}`;navigator.clipboard?.writeText(msg).then(()=>toast("copied"),()=>toast("copied"));if(!navigator.clipboard)toast("copied")}
  if(d.nk){TS.classes[+d.nk].key=rkey(TS.classes[+d.nk].key.split("-")[0]);go(SCREENS.tclasses)}
  if(b.id==="mkc"){const v=document.getElementById("ncl").value.trim();if(!v)return;const k=rkey(v.replace(/[^A-Za-z0-9]/g,"").slice(0,4).toUpperCase()||"CLS");
    TS.classes.push({name:v,key:k,n:0,active:0,acc:0,behind:0,lessons:0,week:[0,0,0,0,0,0,0],weak:[],kids:[]});go(SCREENS.tclasses);toast("created",{k})}
  if(b.id==="samp"){TS.files=["Photosynthesis_notes_p1.jpg","Photosynthesis_notes_p2.jpg","Leaf_diagram.pdf"];go(SCREENS.tadd)}
  if(b.id==="mk"){TS.step=0;TS.qs=QS0.map(q=>({...q}));go(SCREENS.tgen);const tick=()=>{TS.step++;if(TS.step<4){go(SCREENS.tgen);setTimeout(tick,900)}else{go(SCREENS.tgen);setTimeout(()=>go(SCREENS.treview),700)}};setTimeout(tick,900)}
  if(d.ed!==undefined){const i=+d.ed;if(TS.editing===i){const el=$.querySelector(`[data-qi="${i}"]`);const parts=el.querySelectorAll("span");TS.qs[i].q={...TS.qs[i].q,[S.lang]:parts[0].textContent,en:parts[1]?parts[1].textContent:TS.qs[i].q.en};TS.qs[i].flag=null;TS.editing=null}else TS.editing=i;go(SCREENS.treview);if(TS.editing===i)$.querySelector(`[data-qi="${i}"]`).focus()}
  if(d.rg!==undefined){const q=TS.qs[+d.rg];[q.q,q.alt]=[q.alt,q.q];q.flag=null;go(SCREENS.treview)}
  if(d.dl!==undefined){TS.qs.splice(+d.dl,1);go(SCREENS.treview)}
  if(b.id==="pub"){TS.courses.unshift({t:["प्रकाश संश्लेषण","ದ್ಯುತಿಸಂಶ್ಲೇಷಣೆ","Photosynthesis"],cls:TS.classes[TS.assign].name,pct:0,live:true});TS.files=[];TS.text="";go(SCREENS.tcourses);toast("published")}
});

// Tummy slowly empties while the student is in the app; mood changes show up on Home
setInterval(()=>{if(S.role!=="student"||S.feeding)return;const before=moodKey();S.food=Math.max(0,S.food-1);
  const i=$.querySelector(".food-i");if(i)i.style.width=S.food+"%";if(S.screen==="home"&&moodKey()!==before)go(SCREENS.home)},4000);

// ---- Weekly challenge + multiple classrooms
Object.assign(D,{
  wkCh:["इस हफ़्ते की चुनौती","ಈ ವಾರದ ಸವಾಲು","This week’s challenge"],wkGoal:["5 पाठ पूरे करो","5 ಪಾಠ ಮುಗಿಸಿ","Finish 5 lessons"],
  wkPrize:["इनाम: {cow} के लिए खास गुड़","ಬಹುಮಾನ: {cow}ಗೆ ವಿಶೇಷ ಬೆಲ್ಲ","Prize: special jaggery for {cow}"],wkLeft:["3 दिन बाकी","3 ದಿನ ಬಾಕಿ","3 days left"],
  myCl:["मेरी क्लास","ನನ್ನ ತರಗತಿಗಳು","My classes"],joinMore:["एक और क्लास जोड़ो","ಇನ್ನೊಂದು ತರಗತಿ ಸೇರಿ","Join another class"],
  keyPh:["क्लास की डालो","ತರಗತಿ ಕೀ ಹಾಕಿ","Enter class key"],find:["खोजो","ಹುಡುಕಿ","Find class"],
  notFound:["यह की नहीं मिली। टीचर से दोबारा पूछो","ಈ ಕೀ ಸಿಕ್ಕಿಲ್ಲ. ಶಿಕ್ಷಕರನ್ನು ಮತ್ತೆ ಕೇಳಿ","Key not found. Check with your teacher"],
  already:["तुम पहले से इस क्लास में हो","ನೀವು ಈಗಾಗಲೇ ಈ ತರಗತಿಯಲ್ಲಿದ್ದೀರಿ","You’re already in this class"],
  joined:["क्लास में जुड़ गए!","ತರಗತಿಗೆ ಸೇರಿದಿರಿ!","You joined the class!"],cancel:["रद्द करो","ರದ್ದು ಮಾಡಿ","Cancel"],
  tryKeys:["आज़माने के लिए: MATH9-T3R8 या SCI7-P2L5","ಪ್ರಯತ್ನಿಸಲು: MATH9-T3R8 ಅಥವಾ SCI7-P2L5","To try: MATH9-T3R8 or SCI7-P2L5"],
  soon:["जल्द आ रहा है","ಶೀಘ್ರದಲ್ಲೇ","Coming soon"]
});
const CAT={
  "BIO9-K4X2":{e:ic("flask"),name:["कक्षा 9 · जीव विज्ञान","9ನೇ ತರಗತಿ · ಜೀವಶಾಸ್ತ್ರ","Class 9 · Biology"],teacher:"Mrs. Rao",n:38,course:["कोशिका","ಕೋಶ","The Cell"]},
  "MATH9-T3R8":{e:ic("calc"),name:["कक्षा 9 · गणित","9ನೇ ತರಗತಿ · ಗಣಿತ","Class 9 · Maths"],teacher:"Mr. Iyer",n:41,course:["रैखिक समीकरण","ರೇಖಾತ್ಮಕ ಸಮೀಕರಣಗಳು","Linear equations"]},
  "SCI7-P2L5":{e:ic("sprout"),name:["कक्षा 7 · विज्ञान","7ನೇ ತರಗತಿ · ವಿಜ್ಞಾನ","Class 7 · Science"],teacher:"Ms. Fernandes",n:33,course:["प्रकाश संश्लेषण","ದ್ಯುತಿಸಂಶ್ಲೇಷಣೆ","Photosynthesis"]}};
// Classes a teacher makes in teacher mode can be joined with their key too
function findClass(k){if(CAT[k])return CAT[k];const t=TS.classes.find(c=>c.key===k);if(!t)return null;
  return {e:ic("school"),name:[t.name,t.name,t.name],teacher:S.tname||"Teacher",n:t.n,course:[D.soon[0],D.soon[1],D.soon[2]]}}
const N=a=>`<span class="reg">${esc(a[LI[S.lang]])}</span><span class="engl">${esc(a[2])}</span>`;
function joinBox(){
  if(S.jp){const c=findClass(S.jp);return `<div class="tcard joinbox"><span class="chip">${R("preview")} · ${E("preview")}</span>
    <div style="display:flex;gap:12px;align-items:center"><div class="ic big">${c.e}</div><div style="min-width:0">${N(c.name)}<small>${esc(c.teacher)} · ${R("students",{n:c.n})}</small><br><small>${ic("books")} ${N(c.course)}</small></div></div>
    ${btn("join",'id="jy"')}<button class="mini" id="jn">${R("cancel")} · ${E("cancel")}</button></div>`}
  return `<div class="tcard joinbox"><h3>${T(S.screen==="key"?"join":"joinMore")}</h3>
    <input class="field sm" id="jk" placeholder="${R("keyPh")} · ${E("keyPh")}" autocomplete="off" style="text-transform:uppercase">
    ${btn("find",'id="jf"')}<small style="color:var(--muted)">${R("tryKeys")}<br>${E("tryKeys")}</small></div>`}
$.addEventListener("click",e=>{
  const b=e.target.closest("button");if(!b)return;
  if(b.dataset.ci!==undefined){S.ci=+b.dataset.ci;go(SCREENS.path)}
  if(b.id==="jf"){const v=document.getElementById("jk").value.trim().toUpperCase();
    if(S.classes.includes(v)){toast("already");return}if(!findClass(v)){toast("notFound");return}S.jp=v;go(SCREENS[S.screen])}
  if(b.id==="jn"){S.jp=null;go(SCREENS[S.screen])}
  if(b.id==="jy"){S.classes.push(S.jp);S.jp=null;toast("joined");if(S.screen==="key")go(SCREENS.cowname);else go(SCREENS.class)}
});
function sunriseBg(){return `<svg class="bg skybg" viewBox="0 0 400 800" preserveAspectRatio="xMidYMax slice" aria-hidden="true">
  <circle cx="200" cy="660" r="150" fill="#fff3c4" opacity=".55"/><circle cx="200" cy="660" r="90" fill="#ffd25a"/>
  <g class="cloud c1"><ellipse cx="80" cy="120" rx="44" ry="15" fill="#fff" opacity=".85"/><ellipse cx="100" cy="110" rx="24" ry="16" fill="#fff" opacity=".85"/></g>
  <g class="cloud c2"><ellipse cx="260" cy="70" rx="30" ry="11" fill="#fff" opacity=".8"/><ellipse cx="274" cy="63" rx="16" ry="11" fill="#fff" opacity=".8"/></g>
  <path d="M0 690Q100 630 210 676T400 660V800H0Z" fill="#c9e7a6"/><path d="M0 730Q130 690 250 728T400 718V800H0Z" fill="#a5d77f"/></svg>`}
function starsBg(){return `<svg class="bg skybg" viewBox="0 0 400 800" preserveAspectRatio="xMidYMid slice" aria-hidden="true">${[[50,90,1],[340,120,1.3],[80,420,.8],[330,470,1],[200,60,.7],[30,640,1],[370,700,.8]].map(([x,y,k],i)=>`<path class="spark" style="animation-delay:${i*.2}s" transform="translate(${x} ${y}) scale(${k*1.6})" d="M0-11l3 8 8 3-8 3-3 8-3-8-8-3 8-3z" fill="#fff"/>`).join("")}</svg>`}

Object.assign(D,{about:["परिचय","ಪರಿಚಯ","About"],details:["विवरण","ವಿವರ","Details"],traits:["खूबियाँ","ಗುಣಗಳು","Traits"],
 favFood:["पसंदीदा खाना","ಇಷ್ಟದ ಆಹಾರ","Favourite food"],bornOn:["जन्मदिन","ಹುಟ್ಟಿದ ದಿನ","Birthday"],stuCode:["छात्र कोड","ವಿದ್ಯಾರ್ಥಿ ಕೋಡ್","Student code"],
 dayStreakW:["दिन की लड़ी","ದಿನದ ಸರಣಿ","day streak"],collection:["{cow} का कलेक्शन","{cow}ನ ಸಂಗ್ರಹ","{cow}’s collection"],poses:["पोज़","ಭಂಗಿಗಳು","Poses"]});
$.addEventListener("click",e=>{const b=e.target.closest("[data-pt]");if(b){S.ptab=b.dataset.pt;go(SCREENS.me)}});

go(SCREENS.lang);