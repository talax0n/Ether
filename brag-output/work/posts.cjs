const {chromium}=require(require('child_process').execSync('npm root -g').toString().trim()+'/@playwright/mcp/node_modules/playwright-core');
const IDS=['how-1','how-2','how-3','how-4','how-5','how-6','etha-1','etha-2','etha-3','chat-1','chat-2','xp','levels','roles','swipe-quote','download'];
const out=process.argv[2]; require('fs').mkdirSync(out,{recursive:true});
(async()=>{const b=await chromium.launch({executablePath:'/Users/theo/Library/Caches/ms-playwright/chromium_headless_shell-1254/chrome-headless-shell-mac-arm64/chrome-headless-shell'});
  const p=await b.newPage({viewport:{width:1080,height:1350}});
  p.on('pageerror',e=>console.log('ERR',e.message));
  for(const id of IDS){await p.goto('file://'+__dirname+'/site/posts.html?p='+id);await p.evaluate(()=>window.ready);await p.screenshot({path:`${out}/${id}.png`})}
  await b.close()})();
