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
  core:{shade:"blue",depth:.82,profile:[[0,-.95,.95],[.42,-1.42,1.42],[1.04,-1.50,1.50],[1.62,-1.21,1.21],[2.35,-1.0,1],[3.15,-.89,.89],[4.10,-.72,.72],[4.82,-.48,.48]],front:"cobalt"},
  upper:{shade:"white",depth:.78,profile:[[0,-.52,.74],[.85,-1.0,.90],[1.55,-1.10,.85],[2.25,-.95,.78],[3.6,-.69,.70],[5.24,-.48,.48],[5.98,-.34,.33]],front:"porcelain"},
  nose:{shade:"white",depth:1.05,profile:[[0,-.66,.66],[.33,-1.0,1],[.83,-1.32,1.32],[1.60,-1.42,1.42],[2.56,-1.16,1.16],[3.21,-.91,.91],[3.8,-.57,.57]],front:"porcelain"},
  shoulder:{shade:"blue",depth:1.16,profile:[[0,-.82,.83],[.61,-1.09,1.06],[1.30,-1.17,1.12],[2.05,-.97,1.02],[2.77,-.78,.89],[3.34,-.66,.69]],front:"cobalt"},
  outer:{shade:"white",depth:.94,profile:[[0,-.40,.49],[1.4,-.53,.60],[3.10,-.65,.69],[5.7,-.84,.82],[7.6,-1.08,.95],[8.2,-1.02,.98],[8.7,-.65,.73],[10.25,-.77,.87],[11.55,-.79,.79],[12.55,-.51,.56],[14.23,-.37,.43]],front:"porcelain",edge:"cobalt"},
  crest:{shade:"white",depth:.93,profile:[[0,-.58,.58],[.36,-.95,.95],[1.35,-1.26,1.26],[2.25,-1.45,1.45],[3.43,-1.65,1.65],[4.48,-1.63,1.63]],front:"porcelain"},
  inner:{shade:"charcoal",depth:.75,profile:[[0,-.25,.27],[1.10,-.35,.38],[2.8,-.42,.46],[4.15,-.54,.55],[5.55,-.79,.65],[6.42,-.74,.72],[7.5,-.56,.54],[8.92,-.36,.42]],front:"steel"}
 };
 const placements=[
  ["01","上部青コア","core",0,9.45,.56,false],
  ["02","左上白ブレード","upper",-1.92,9.52,.12,false],
  ["03","右上白ブレード","upper",1.92,9.52,.12,true],
  ["04","中央白ノーズ","nose",0,6.13,.84,false],
  ["05","左肩青＋黄ベント","shoulder",-2.17,7.20,.92,false],
  ["06","右肩青＋黄ベント","shoulder",2.17,7.20,.92,true],
  ["07","左下白外装","outer",-2.50,-7.10,.04,false],
  ["08","右下白外装","outer",2.50,-7.10,.04,true],
  ["09","中央白V字装甲","crest",0,1.69,.47,false],
  ["10","左グレー内装","inner",-1.00,-7.02,.04,false],
  ["11","右グレー内装","inner",1.00,-7.02,.04,true]
 ];
 const step=.24,round=n=>Math.round(n*1e5)/1e5;
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
   // independently authored beveled surface inset to avoid a flat plank.
   if(y>0.24&&y2<end-.4&&hi-lo>.65){
    const inset=type==="outer"?.21:.18;
    put([lo+inset,y+.014,thick/2],[hi-inset,y2-.014,thick/2+.085],recipe.front);
   }
   if(recipe.edge&&y2<end-.25){
    put([lo,y,thick/2-.005],[Math.min(lo+.16,hi-.1),y2,thick/2+.13],recipe.edge);
   }
  }
  const p=(from,to,mat)=>put(from,to,mat);
  function bore(x,y,r=.30){
   let z=thick/2+.12; // black recess backed by eight small blue ring facets
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
    bore(0,3.42,.29);bore(-1.09,1.16,.23);bore(1.09,1.16,.23);
    p([-.85,4.7,-.24],[-.61,5.78,.26],"white");
    p([.61,4.7,-.24],[.85,5.78,.26],"white");
    p([-.42,.08,.45],[.42,.38,.61],"dark");
    break;
   case "upper":
    p([-.69,.40,.43],[.28,1.42,.57],"porcelain");
    p([-.41,.57,.61],[-.14,1.33,.67],"steel");
    p([-.33,.74,.69],[-.20,1.15,.73],"bore");
    p([-.37,1.70,.48],[.50,2.20,.55],"white");
    break;
   case "nose":
    p([-1.13,.25,.52],[1.13,.53,.68],"cobalt");
    p([-1.00,.50,.72],[1.00,1.95,.78],"white");
    p([-.43,2.72,.65],[.43,3.62,.77],"porcelain");
    p([-.25,3.02,.78],[.25,3.47,.83],"steel");
    break;
   case "shoulder":
    // Slanted gold vent made of staggered narrow steps.
    for(let i=0;i<6;i++){
     let x=-.87+i*.22, y=2.30+i*.105;
     p([x,y,thick/2+.12],[x+.27,y+.48,thick/2+.32],"gold");
    }
    p([-.96,.29,.66],[-.77,1.67,.77],"trim");
    p([.56,.26,.65],[.73,1.79,.74],"dark");
    p([-.49,.01,-.69],[.49,.70,-.56],"graphite");
    break;
   case "outer":
    // Beveled outer edge follows every bend, long white face stays wide.
    for(let y=.7;y<end-1.4;y+=1.0){
     let y2=Math.min(end-1.4,y+.62),[lo]=bounds(y);
     p([lo+.26,y,thick/2+.10],[lo+.37,y2,thick/2+.18],"white");
    }
    p([.25,9.58,.51],[.34,10.43,.58],"steel");
    p([.27,4.76,.51],[.38,5.88,.58],"steel");
    p([-.23,.90,.51],[-.08,1.43,.58],"porcelain");
    break;
   case "crest":
    p([-1.54,3.06,.51],[-1.14,4.37,.63],"porcelain");
    p([1.14,3.06,.51],[1.54,4.37,.63],"porcelain");
    p([-.37,.20,.55],[.37,.88,.77],"white");
    p([-.23,.22,.78],[.23,.53,.83],"steel");
    p([-.73,1.22,.52],[.73,2.68,.60],"porcelain");
    break;
   case "inner":
    p([-.24,1.10,.42],[.22,5.46,.52],"steel");
    p([-.25,6.51,.41],[.27,7.11,.52],"charcoal");
    p([-.18,3.36,.53],[.02,4.93,.60],"graphite");
    p([.12,2.15,.52],[.25,3.22,.60],"dark");
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
 if((maxY-minY)/(maxX-minX)<2.4)throw Error("Shield looks too wide relative to reference");
 if(minY < -16 || maxY > 32)throw Error("Model out of bounds");
 outputs["docs/AERIAL_BIT_FIRST.md"]=[
 "# Bit-first Escutcheon preview, v3 revised fit (not yet approved)",
 "",
 "Each of the 11 detached GUND-bit parts is separately authored from the right-hand part layout in the user's photo. The assembled preview contains only those exact 11 geometries translated into the arrangement shown on the LEFT side of the photo.",
 "",
 "The first (0.8.20) draft looked like an exploded shield because part placements copied the spacing of the loose pieces. In 0.8.21, each join transform is tightened, the upper core/side-armor/nose/crest and white/grey lower rails are brought into contact, the white palette is corrected, and new layered bevels, blue outlines, gold vent and nozzle-ring details are included.",
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
