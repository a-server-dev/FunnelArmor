// Imported V3 shield is the canonical source. Detached models use the exact
// offsets and five-decimal rounding corrections from the supplied archive.
// Run with --check to validate generated detached geometry and blocking pose.
import fs from 'node:fs';
import path from 'node:path';
const dir='resourcepack/assets/funnel/models/item/';
const shield=JSON.parse(fs.readFileSync(dir+'aerial_v2_escutcheon.json','utf8'));
const counts=[45,20,20,54,22,22,96,96,65,23,23];
const shifts=[[0,12.4,1.045],[-1.85369,12.8,.295],[1.85369,12.8,.295],[0,7.595,1.11],[-2.76575,8.52962,1.14],[2.76575,8.52962,1.14],[-2.86893,.46,.2],[2.86893,.46,.2],[0,3.25,.8225],[-1.17767,-1.95,-.165],[1.17767,-1.95,-.165]];
const scales=[1.93548,1.82371,1.82371,2.48447,2.6,2.6,.80645,.80645,2.6,1.43885,1.43885];
const corrections=[{}, {27:1,36:1},{30:-1,39:-1},{},{},{},{48:1,102:1,153:1,162:1,165:1,660:1},{45:-1,99:-1,102:-1,108:-1,111:-1,156:-1,162:-1,165:-1,387:-1,390:-1,396:-1,399:-1,657:-1},{},{},{}];
const round=n=>Math.round(n*1e5)/1e5;
if(shield.elements.length!==491)throw Error('V3 must have 486 bit cubes and 5 grip cubes');
const outputs={};let offset=0;
for(let i=0;i<counts.length;i++){
 const elements=shield.elements.slice(offset,offset+=counts[i]).map((e,j)=>{
  const shift=(v,k)=>v.map((n,a)=>round(round(n-shifts[i][a])+(corrections[i][j*9+k*3+a]||0)/1e5));
  return {...e,from:shift(e.from,0),to:shift(e.to,1),...(e.rotation?{rotation:{...e.rotation,origin:shift(e.rotation.origin,2)}}:{})};
 });
 const display={gui:{rotation:[15,205,0],translation:[0,0,0],scale:Array(3).fill(scales[i])},fixed:{rotation:[0,180,0],translation:[0,0,0],scale:[1,1,1]}};
 outputs[dir+'aerial_v2_bit_'+String(i+1).padStart(2,'0')+'.json']={...shield,elements,display};
}
const blocking={...shield,display:structuredClone(shield.display)};
for(const key of ['firstperson_lefthand','firstperson_righthand','thirdperson_lefthand','thirdperson_righthand']){
 const d=blocking.display[key],first=key.startsWith('first');
 d.translation[1]=first?2:5;d.scale=Array(3).fill(first?1.49:1.56);
}
outputs[dir+'aerial_v2_escutcheon_blocking.json']=blocking;
const check=process.argv.includes('--check');
for(const [file,model] of Object.entries(outputs)){
 const contents=JSON.stringify(model)+'\n';
 if(check){if(!fs.existsSync(file)||JSON.stringify(JSON.parse(fs.readFileSync(file,'utf8')))!==JSON.stringify(model)){console.error('STALE '+file);process.exitCode=1;}}
 else {fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,contents);}
}
if(!process.exitCode)console.log('V3 Escutcheon: 11 detached parts, 486 bit cubes + 5 handle cubes.');
