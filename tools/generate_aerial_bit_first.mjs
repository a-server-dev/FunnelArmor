// Source geometry for eleven independent GUND bits (photo-based), then join all 11.
// Vanilla JSON only; generates non-production preview items.
// Run: node tools/generate_aerial_bit_first.mjs [--check]
import fs from 'node:fs';
import path from 'node:path';
function buildBitFirst(){
 const palette={white:[70.5,22.5],porcelain:[22.5,22.5],blue:[166.5,22.5],cobalt:[214.5,22.5],trim:[70.5,70.5],gold:[118.5,70.5],dark:[214.5,70.5],graphite:[22.5,118.5],charcoal:[70.5,118.5],steel:[118.5,118.5],bore:[166.5,118.5],shine:[22.5,166.5]};
 // The x/y profiles are for eleven separate parts from the loose-piece photo.
 // z+ faces are the colored front surfaces; assembled positions use the LEFT joined shield photo.
 const recipes={
  core:{shade:"blue",depth:.90,profile:[[0,-.85,.85],[.6,-1.46,1.46],[1.48,-1.42,1.42],[2.10,-1.14,1.14],[3.25,-.91,.91],[4.65,-.72,.72],[5.10,-.44,.44]],front:"cobalt"},
  upper:{shade:"white",depth:.72,profile:[[0,-.42,.56],[.75,-.70,.72],[1.70,-.87,.83],[2.55,-.79,.77],[4.40,-.57,.55],[6.05,-.35,.35],[6.75,-.20,.22]],front:"porcelain"},
  nose:{shade:"white",depth:1.03,profile:[[0,-.46,.46],[.54,-.93,.93],[1.33,-1.17,1.17],[2.14,-1.18,1.18],[3.21,-1.04,1.04],[4.44,-.82,.82],[5.32,-.46,.46]],front:"porcelain"},
  shoulder:{shade:"blue",depth:1.01,profile:[[0,-.59,.59],[.60,-.97,.97],[1.30,-1.03,1.01],[2.07,-.88,.88],[2.78,-.69,.70],[3.32,-.46,.50]],front:"cobalt"},
  outer:{shade:"white",depth:1.03,profile:[[0,-.29,.34],[1.05,-.56,.56],[2.4,-.87,.90],[4.70,-1.22,1.28],[6.6,-1.50,1.49],[8.05,-1.52,1.55],[8.75,-1.13,1.47],[10.20,-1.24,1.49],[12.15,-1.13,1.34],[13.18,-.88,1.08],[14.54,-.68,.81]],front:"porcelain",edge:"cobalt"},
  crest:{shade:"white",depth:.93,profile:[[0,-.22,.22],[.67,-.70,.70],[1.30,-1.04,1.04],[2.58,-1.44,1.44],[3.80,-1.56,1.56],[4.63,-1.41,1.41]],front:"porcelain"},
  inner:{shade:"charcoal",depth:.74,profile:[[0,-.24,.23],[1.1,-.39,.40],[2.85,-.59,.61],[4.37,-.82,.74],[6.30,-.84,.78],[7.75,-.69,.67],[9.08,-.41,.44]],front:"steel"}
 };
 const placements=[
  ["01","上部青コア","core",0,9.28,1.0,false],
  ["02","左上白ブレード","upper",-2.07,9.68,.25,false],
  ["03","右上白ブレード","upper",2.07,9.68,.25,true],
  ["04","中央白ノーズ","nose",0,4.62,1.40,false],
  ["05","左肩青＋黄ベント","shoulder",-2.27,7.17,1.04,false],
  ["06","右肩青＋黄ベント","shoulder",2.27,7.17,1.04,true],
  ["07","左下白外装","outer",-3.41,-8.32,.25,false],
  ["08","右下白外装","outer",3.41,-8.32,.25,true],
  ["09","中央白V字装甲","crest",0,.85,1.0,false],
  ["10","左グレー内装","inner",-1.24,-8.20,.08,false],
  ["11","右グレー内装","inner",1.24,-8.20,.08,true]
 ];
 const step=.53,round=n=>Math.round(n*1e5)/1e5;
 function piece(type,mirror){
  const recipe=recipes[type],stations=recipe.profile,thick=recipe.depth;
  let boxes=[];
  function put(from,to,material){
   if(mirror){const old=from[0];from=[-to[0],from[1],from[2]];to=[-old,to[1],to[2]];}
   if(!from.every((v,i)=>v<to[i]-1e-7))throw Error("Invalid box "+type+":"+material);
   boxes.push({from,to,material});
  }
  function bounds(y){for(let i=0;i<stations.length-1;i++){let a=stations[i],b=stations[i+1];if(y<=b[0]+1e-8){let t=Math.max(0,Math.min(1,(y-a[0])/(b[0]-a[0])));return [a[1]+t*(b[1]-a[1]),a[2]+t*(b[2]-a[2])];}}return stations.at(-1).slice(1);}
  const end=stations.at(-1)[0];
  for(let y=0;y<end-1e-6;y+=step){
   let y2=Math.min(end,y+step),aa=bounds(y),bb=bounds(y2),lo=Math.min(aa[0],bb[0]),hi=Math.max(aa[1],bb[1]);
   put([lo,y,-thick/2],[hi,y2,thick/2],recipe.shade);
   // Leave each large front face uninterrupted. Earlier per-slice overlays
   // created dark horizontal zebra stripes and thick rectangular plates.
   if(recipe.edge&&y2<end-.25){
    put([lo,y,thick/2+.018],[Math.min(lo+.19,hi-.1),y2,thick/2+.18],recipe.edge);
    if(y>2 && y2<end-1 && hi-lo>1.55)
     put([hi-.33,y+.022,thick/2+.02],[hi-.1,y2-.022,thick/2+.115],"porcelain");
   }
  }
  const p=(from,to,mat)=>put(from,to,mat);
  function bore(x,y,r=.30){
   let z=thick/2+.47; // keep sockets in FRONT of the blue armor, never buried by its inset
   p([x-r*.65,y-r*.65,z],[x+r*.65,y+r*.65,z+.04],"bore");
   const slices=8;
   for(let k=0;k<slices;k++){
    let a=2*Math.PI*k/slices,xx=x+Math.cos(a)*r,yy=y+Math.sin(a)*r;
    p([xx-.065,yy-.065,z+.04],[xx+.065,yy+.065,z+.10],"cobalt");
   }
  }
  switch(type){
   case "core":
    p([-1.16,.63,.51],[1.16,1.14,.68],"cobalt");
    p([-.84,1.27,.56],[.84,3.31,.72],"blue");
    p([-.57,3.52,.55],[.57,4.64,.68],"cobalt");
    bore(0,3.80,.42);bore(-.84,1.30,.32);bore(.84,1.30,.32);
    p([-.85,4.76,-.26],[-.63,6.50,.25],"white");
    p([.63,4.76,-.26],[.85,6.50,.25],"white");
    p([-.42,.08,.45],[.42,.38,.61],"dark");
    break;
   case "upper":
    p([-.61,.29,.39],[.40,1.21,.53],"porcelain");
    p([-.41,.57,.61],[-.14,1.33,.67],"steel");
    p([-.33,.74,.69],[-.20,1.15,.73],"bore");
    p([-.37,1.70,.48],[.50,2.20,.55],"white");
    break;
   case "nose":
    p([-.77,.35,.57],[.77,.64,.69],"cobalt");
    p([-.97,1.05,.68],[.97,3.64,.80],"white");
    p([-.45,4.39,.63],[.45,5.06,.80],"porcelain");
    p([-.24,4.62,.81],[.24,4.96,.87],"steel");
    p([-.38,.08,.69],[.38,.34,.80],"cobalt");
    break;
   case "shoulder":
    // Slanted gold vent made of staggered narrow steps.
    for(let i=0;i<5;i++){
     let x=-.69+i*.21, y=2.48+i*.09;
     p([x,y,thick/2+.13],[x+.27,y+.49,thick/2+.37],"gold");
    }
    p([-.96,.29,.66],[-.77,1.67,.77],"trim");
    p([.56,.26,.65],[.73,1.79,.74],"dark");
    p([-.49,.01,-.69],[.49,.70,-.56],"graphite");
    break;
   case "outer":
    // Broad white armor, only limited edge panel details and outside blue rim.
    p([.78,9.62,thick/2+.03],[.94,10.78,thick/2+.15],"steel");
    p([.69,5.02,thick/2+.03],[.83,6.18,thick/2+.15],"porcelain");
    p([-.21,.90,thick/2+.03],[-.05,1.55,thick/2+.15],"porcelain");
    break;
   case "crest":
    p([-1.33,3.27,.51],[-1.13,4.36,.65],"porcelain");
    p([1.13,3.27,.51],[1.33,4.36,.65],"porcelain");
    p([-.32,.24,.57],[.32,.92,.78],"cobalt");
    p([-.20,.16,.79],[.20,.48,.86],"white");
    p([-.93,2.09,.52],[.93,3.23,.69],"porcelain");
    break;
   case "inner":
    p([-.31,1.04,.42],[.24,7.45,.52],"steel");
    p([-.22,7.53,.41],[.22,8.20,.52],"charcoal");
    p([-.14,2.83,.53],[.02,6.57,.60],"shine");
    p([.13,2.12,.52],[.26,3.29,.60],"dark");
    break;
  }
  return boxes;
 }
 const parts=placements.map(([id,label,type,x,y,z,mirror])=>({id,label,x,y,z,boxes:piece(type,mirror)}));
 if(parts.length!==11)throw Error("exactly 11 required");
 const model=(elements,display)=>({parent:"minecraft:block/block",ambientocclusion:false,textures:{"0":"funnel:item/aerial_escutcheon",particle:"funnel:item/aerial_escutcheon"},elements,display,gui_light:"front"});
 const convert=(boxes,shift)=>boxes.map(box=>{
  const pixel=palette[box.material],u=pixel[0]/16,v=pixel[1]/16,d=.02;
  const face={uv:[round(u-d),round(v-d),round(u+d),round(v+d)],texture:"#0"};
  return {from:box.from.map((n,i)=>round(n+8+shift[i])),to:box.to.map((n,i)=>round(n+8+shift[i])),faces:Object.fromEntries(["north","south","east","west","up","down"].map(f=>[f,face]))};
 });
 const bitDisplay=(height)=>({gui:{rotation:[15,205,0],translation:[0,0,0],scale:Array(3).fill(round(Math.min(2.6,12/height)))},fixed:{rotation:[0,180,0],translation:[0,0,0],scale:[1,1,1]}});
 const shieldDisplay={gui:{rotation:[15,-25,-5],translation:[0,1,0],scale:[.65,.65,.65]},fixed:{rotation:[0,180,0],translation:[0,0,0],scale:[1,1,1]}};
 const outputs={};
 for(const part of parts){
  const min=[Infinity,Infinity,Infinity],max=[-Infinity,-Infinity,-Infinity];
  for(const box of part.boxes)for(let i=0;i<3;i++){min[i]=Math.min(min[i],box.from[i]);max[i]=Math.max(max[i],box.to[i]);}
  const localOffset=min.map((n,i)=>-(n+max[i])/2),id="aerial_v2_bit_"+part.id;
  outputs["resourcepack/assets/funnel/models/item/"+id+".json"]=JSON.stringify(model(convert(part.boxes,localOffset),bitDisplay(max[1]-min[1])),null,2)+"\n";
  // Existing item-definition model references are unchanged.
 }
 const whole=parts.flatMap(part=>convert(part.boxes,[part.x,part.y,part.z]));
 outputs["resourcepack/assets/funnel/models/item/aerial_v2_escutcheon.json"]=JSON.stringify(model(whole,shieldDisplay),null,2)+"\n";
 const maxX=Math.max(...whole.map(x=>x.to[0])),minX=Math.min(...whole.map(x=>x.from[0])),maxY=Math.max(...whole.map(x=>x.to[1])),minY=Math.min(...whole.map(x=>x.from[1]));
 if(whole.length!==parts.reduce((a,p)=>a+p.boxes.length,0))throw Error("Failed bit-first join");
 if((maxY-minY)/(maxX-minX)<2.10 || (maxY-minY)/(maxX-minX)>2.70)
   throw Error("Joined shield should match the primary reference, not look like two thin sticks");
 if(minY < -16 || maxY > 32)throw Error("Model out of bounds");
 outputs["docs/AERIAL_BIT_FIRST.md"]=[
 "# Bit-first Escutcheon preview, v4 primary front reference (not yet approved)",
 "",
 "Each of the 11 detached GUND-bit parts is separately authored from the right-hand part layout in the user's photo. The assembled preview contains only those exact 11 geometries translated into the arrangement shown on the LEFT side of the photo.",
 "",
 "This 0.8.22 iteration targets the SECOND image (stylized frontal joined shield) as the primary silhouette. The THIRD physical photo defines the 11 separate component shapes. Compared with 0.8.21, the white upper fins are narrower and longer; the blue core's three sockets move in front of its surfaces; the white central nose is longer and sharper; gold shoulder vents become compact and slanted; the side armor is broadened inward with blue exterior edge; the grey center blades are inset and gain blue/cyan channels. Redundant per-slice raised face plates were removed to reduce zebra striping.",
 "",
 "| Bit | Part | Joined offset x,y,z | Cuboids |",
 "| --- | --- | --- | --- |",
 ...parts.map(p=>`| ${p.id} | ${p.label} | ${p.x}, ${p.y}, ${p.z} | ${p.boxes.length} |`),
 "",
 `Total ${whole.length} cuboids; shield width ${round(maxX-minX)} vs height ${round(maxY-minY)} (height/width ratio ${round((maxY-minY)/(maxX-minX))}).`,
 "",
 "Single source of truth: `tools/generate_aerial_bit_first.mjs` which regenerates all 12 vanilla JSON geometry files with `node tools/generate_aerial_bit_first.mjs` or checks with `--check`.",
 "",
 "Previews only: `/funnel aerial preview <1..11|shield|clear>` (OP). Existing normal aerial bits, native equipped shield, rifle and original `.bbmodel` files remain unchanged. No BetterModel required. The dark non-bit rear frame in the picture is not counted among the 11 bits.",
 ""
 ].join("\n");
 return {outputs,parts:parts.map(p=>({id:p.id,materialCount:p.boxes.length})),sum:whole.length,bounds:{x:[minX,maxX],y:[minY,maxY],ratio:(maxY-minY)/(maxX-minX)}};
}
const {outputs}=buildBitFirst();
const check=process.argv.includes('--check');
let stale=0;
for(const [name,contents] of Object.entries(outputs)){
 const target=path.resolve(name);
 if(check){
  if(!fs.existsSync(target)||fs.readFileSync(target,'utf8')!==contents){console.error('STALE '+name);stale++;}
 } else {
  fs.mkdirSync(path.dirname(target),{recursive:true});
  fs.writeFileSync(target,contents,'utf8');
  console.log('WROTE '+name);
 }
}
if(stale)process.exitCode=1;
else console.log(check?'Bit-first generated models match.':'Regenerated 11 bits and their joined Escutcheon.');
