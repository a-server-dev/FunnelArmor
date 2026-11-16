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
  core:{shade:"blue",depth:.90,profile:[[0,-.89,.89],[.6,-1.44,1.44],[1.48,-1.42,1.42],[2.10,-1.14,1.14],[3.25,-.91,.91],[4.58,-.72,.72],[5.08,-.44,.44]],front:"cobalt"},
  upper:{shade:"white",depth:.76,profile:[[0,-.49,.68],[.80,-.87,.84],[1.62,-.98,.88],[2.41,-.84,.79],[3.85,-.66,.65],[5.37,-.43,.44],[6.58,-.18,.20]],front:"porcelain"},
  // Pointed, angular six-sided main white nose, not a rounded capsule.
  nose:{shade:"white",depth:1.04,profile:[[0,-.12,.12],[.50,-.54,.54],[1.40,-1.00,1.00],[2.00,-1.06,1.06],[3.20,-1.06,1.06],[4.10,-.74,.74],[4.83,-.39,.39]],front:"porcelain"},
  // Hard-edged sloped blue shoulder, with compact gold inset.
  shoulder:{shade:"blue",depth:1.05,profile:[[0,-.44,.48],[.39,-.76,.80],[1.20,-.98,1.00],[1.86,-.93,.97],[2.57,-.71,.73],[3.26,-.43,.46]],front:"cobalt"},
  outer:{shade:"white",depth:.96,profile:[[0,-.28,.53],[1.12,-.43,.73],[2.53,-.57,.96],[4.47,-.72,1.26],[6.55,-.91,1.47],[7.59,-1.03,1.50],[8.24,-.66,1.19],[8.82,-.69,1.24],[10.65,-.79,1.30],[12.12,-.63,1.06],[13.10,-.43,.81],[14.18,-.29,.49]],front:"porcelain",edge:"cobalt"},
  crest:{shade:"white",depth:.91,profile:[[0,-.21,.21],[.60,-.74,.74],[1.18,-1.13,1.13],[2.31,-1.46,1.46],[3.44,-1.58,1.58],[4.46,-1.37,1.37]],front:"porcelain"},
  inner:{shade:"charcoal",depth:.74,profile:[[0,-.28,.28],[1.20,-.40,.43],[2.93,-.51,.56],[4.24,-.63,.62],[5.71,-.77,.70],[6.65,-.72,.66],[7.60,-.57,.54],[8.34,-.38,.42]],front:"steel"}
 };
 const placements=[
  ["01","上部青コア","core",0,9.30,.80,false],
  ["02","左上白ブレード","upper",-1.99,9.51,.12,false],
  ["03","右上白ブレード","upper",1.99,9.51,.12,true],
  ["04","中央白ノーズ","nose",0,5.10,1.02,false],
  ["05","左肩青＋黄ベント","shoulder",-2.08,7.23,1.06,false],
  ["06","右肩青＋黄ベント","shoulder",2.08,7.23,1.06,true],
  ["07","左下白外装","outer",-2.67,-7.12,.13,false],
  ["08","右下白外装","outer",2.67,-7.12,.13,true],
  ["09","中央白V字装甲","crest",0,1.12,.56,false],
  ["10","左グレー内装","inner",-.91,-6.23,-.24,false],
  ["11","右グレー内装","inner",.91,-6.23,-.24,true]
 ];
 const step=.44,round=n=>Math.round(n*1e5)/1e5;
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
   // Keep outer armor an angular white panel: remove striped white overlays.
   // No raised plate per horizontal row; use continuous surfaces instead.
   if(recipe.edge&&y2<end-.25)
     put([lo,y,thick/2+.012],[Math.min(lo+.15,hi-.12),y2,thick/2+.14],recipe.edge);
  }
  const p=(from,to,mat)=>put(from,to,mat);
  function bore(x,y,r=.30){
   let z=thick/2+.39; // keep dark apertures visible above blue surface
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
    bore(0,3.84,.39);bore(-.85,1.28,.31);bore(.85,1.28,.31);
    p([-.82,4.70,-.25],[-.60,6.20,.27],"white");
    p([.60,4.70,-.25],[.82,6.20,.27],"white");
    p([-.42,.08,.45],[.42,.38,.61],"dark");
    break;
   case "upper":
    p([-.69,.40,.43],[.28,1.42,.57],"porcelain");
    p([-.41,.57,.61],[-.14,1.33,.67],"steel");
    p([-.33,.74,.69],[-.20,1.15,.73],"bore");
    p([-.37,1.70,.48],[.50,2.20,.55],"white");
    p([-.30,2.24,thick/2+.025],[.30,5.75,thick/2+.12],"porcelain");
    break;
   case "nose":
    // Tall central ridge with narrow side facets. The silhouette stays pointed.
    p([-.53,1.26,thick/2+.018],[.53,3.72,thick/2+.145],"porcelain");
    p([-.86,1.67,thick/2+.014],[-.65,3.27,thick/2+.095],"white");
    p([.65,1.67,thick/2+.014],[.86,3.27,thick/2+.095],"white");
    p([-.39,3.85,thick/2+.018],[.39,4.46,thick/2+.14],"porcelain");
    p([-.20,4.18,thick/2+.15],[.20,4.48,thick/2+.21],"steel");
    // Two cobalt lower facets form the nose's tiny V-point.
    p([-.40,.48,thick/2+.035],[-.15,.79,thick/2+.18],"cobalt");
    p([.15,.48,thick/2+.035],[.40,.79,thick/2+.18],"cobalt");
    p([-.13,1.98,thick/2+.155],[.13,3.45,thick/2+.22],"white");
    p([-.91,1.69,thick/2+.020],[-.77,2.83,thick/2+.105],"porcelain");
    p([.77,1.69,thick/2+.020],[.91,2.83,thick/2+.105],"porcelain");
    break;
   case "shoulder":
    // Sharp blue shoulder faces, with five gold steps on a diagonal recess.
    p([-.78,.54,thick/2+.012],[-.59,1.79,thick/2+.19],"cobalt");
    p([.55,.48,thick/2+.012],[.72,1.59,thick/2+.16],"cobalt");
    p([-.56,1.76,thick/2+.014],[.53,2.06,thick/2+.12],"blue");
    p([-.70,2.19,thick/2+.024],[.43,2.85,thick/2+.09],"dark");
    for(let i=0;i<5;i++){
     let x=-.73+i*.20, y=2.17+i*.105;
     p([x,y,thick/2+.12],[x+.23,y+.49,thick/2+.34],"gold");
    }
    p([-.74,.26,thick/2+.02],[-.53,1.37,thick/2+.13],"trim");
    p([.45,.26,thick/2+.02],[.60,1.38,thick/2+.14],"dark");
    p([-.45,.08,-thick/2-.10],[.45,.58,-thick/2-.015],"graphite");
    p([-.28,.055,thick/2+.020],[.28,.29,thick/2+.095],"white");
    p([-.49,1.05,thick/2+.025],[-.35,2.09,thick/2+.14],"blue");
    p([.33,1.05,thick/2+.025],[.46,2.09,thick/2+.14],"cobalt");
    break;
   case "outer":
    // Continuous white faces fill the inner armor without widening the outer edge.
    p([-.29,2.85,thick/2+.022],[.66,7.10,thick/2+.11],"porcelain");
    p([-.30,8.94,thick/2+.022],[.60,11.95,thick/2+.11],"porcelain");
    p([.77,10.20,thick/2+.025],[.88,11.18,thick/2+.13],"white");
    p([.71,4.82,thick/2+.025],[.83,5.83,thick/2+.13],"white");
    p([-.15,1.10,thick/2+.025],[-.04,1.55,thick/2+.13],"white");
    break;
   case "crest":
    p([-1.54,3.06,.51],[-1.14,4.37,.63],"porcelain");
    p([1.14,3.06,.51],[1.54,4.37,.63],"porcelain");
    p([-.37,.20,.55],[.37,.88,.77],"white");
    p([-.23,.22,.78],[.23,.53,.83],"steel");
    p([-.73,1.22,.52],[.73,2.68,.60],"porcelain");
    break;
   case "inner":
    p([-.26,1.08,.40],[.24,6.17,.54],"steel");
    p([-.24,6.51,.39],[.25,7.14,.52],"charcoal");
    p([-.12,3.24,.55],[.035,5.93,.64],"shine");
    p([.11,2.09,.54],[.23,3.14,.63],"dark");
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
  outputs["resourcepack/assets/funnel/models/item/"+id+".json"]=JSON.stringify(model(convert(part.boxes,localOffset),bitDisplay(max[1]-min[1])))+"\n";
  // Existing item-definition model references are unchanged.
 }
 const whole=parts.flatMap(part=>convert(part.boxes,[part.x,part.y,part.z]));
 // The grey rear handle is separate mounting hardware, not one of the 11 bits.
 // Add only to the assembled shield. All pieces remain behind the centre plate.
 const rearMount=[
  {from:[-1.16,4.33,-1.66],to:[1.16,4.71,-1.34],material:"graphite"},
  {from:[-.33,4.66,-1.43],to:[.33,5.26,.075],material:"steel"},
  {from:[-.33,3.78,-1.43],to:[.33,4.38,.075],material:"steel"},
  {from:[-.68,5.05,-.83],to:[.68,5.32,.080],material:"charcoal"},
  {from:[-.68,3.71,-.83],to:[.68,3.98,.080],material:"charcoal"}
 ];
 if(rearMount.some(x=>x.to[2]>.1))throw Error("Grip passes through front");
 whole.push(...convert(rearMount,[0,0,0]));
 outputs["resourcepack/assets/funnel/models/item/aerial_v2_escutcheon.json"]=JSON.stringify(model(whole,shieldDisplay))+"\n";
 const maxX=Math.max(...whole.map(x=>x.to[0])),minX=Math.min(...whole.map(x=>x.from[0])),maxY=Math.max(...whole.map(x=>x.to[1])),minY=Math.min(...whole.map(x=>x.from[1]));
 if(whole.length!==parts.reduce((a,p)=>a+p.boxes.length,0)+rearMount.length)throw Error("Failed bit-first join and grip");
 if((maxY-minY)/(maxX-minX)<2.77||(maxY-minY)/(maxX-minX)>3.25)throw Error("Shield too round or skinny");
 if(minY < -16 || maxY > 32)throw Error("Model out of bounds");
 outputs["docs/AERIAL_BIT_FIRST.md"]=[
 "# Bit-first Escutcheon preview: rear hand grip added (not yet approved)",
 "",
 "Each of the 11 detached GUND-bit parts is separately authored from the right-hand part layout in the user's photo. The assembled preview contains only those exact 11 geometries translated into the arrangement shown on the LEFT side of the photo.",
 "",
 "0.8.26 retains the approved-for-preview sharp nose, blue shoulders, 11 standalone bit geometries, and join positions from v0.8.25. Only the assembled shield receives the rear handle as separate non-bit mounting hardware: one horizontal charcoal grip, two steel supports, and two grey reinforcement plates. This preview does not affect the production shield or any source BBModel.",
 "",
 "| Bit | Part | Joined offset x,y,z | Cuboids |",
 "| --- | --- | --- | --- |",
 ...parts.map(p=>`| ${p.id} | ${p.label} | ${p.x}, ${p.y}, ${p.z} | ${p.boxes.length} |`),
 "",
 `Total ${whole.length} cuboids (11 standalone bits plus ${rearMount.length} rear-only handle parts); shield width ${round(maxX-minX)} vs height ${round(maxY-minY)} (height/width ratio ${round((maxY-minY)/(maxX-minX))}).`,
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
