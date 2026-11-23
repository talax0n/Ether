const {chromium}=require(require('child_process').execSync('npm root -g').toString().trim()+'/@playwright/mcp/node_modules/playwright-core');
const [,,mode,...ts]=process.argv; // mode: stills t1 t2.. | all
(async()=>{
  const b=await chromium.launch({executablePath:'/Users/theo/Library/Caches/ms-playwright/chromium_headless_shell-1254/chrome-headless-shell-mac-arm64/chrome-headless-shell'}); const p=await b.newPage({viewport:{width:+(process.env.W||1920),height:+(process.env.H||1080)}});
  await p.goto('file://'+__dirname+'/site/'+(process.env.PAGE||'index.html')); await p.evaluate(()=>window.ready);
  const fs=require('fs');
  if(mode==='stills'){fs.mkdirSync(__dirname+'/stills',{recursive:true});
    for(const t of ts){await p.evaluate(t=>window.render(+t),t);await p.screenshot({path:`${__dirname}/stills/${t}.jpg`,quality:80,type:'jpeg'})}}
  else{fs.mkdirSync(__dirname+'/'+(process.env.FRAMES||'frames')+'',{recursive:true});const N=Math.round(+ts[0]*30);
    for(let i=0;i<N;i++){await p.evaluate(t=>window.render(t),i/30);await p.screenshot({path:`${__dirname}/${process.env.FRAMES||'frames'}/${String(i).padStart(4,'0')}.jpg`,quality:92,type:'jpeg'})}}
  await b.close();
})();
