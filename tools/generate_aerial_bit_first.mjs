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
  nose:{shade:"white",depth:1.04,profile:[[0,-.025,.025],[1.05,-.88,.88],[4.10,-.88,.88],[4.83,-.39,.39]],front:"porcelain"},
  // Hard-edged sloped blue shoulder, with compact gold inset.
  shoulder:{shade:"blue",depth:1.00,tilt:22.5,profile:[[0,-.37,.41],[.39,-.68,.72],[1.20,-.86,.89],[1.86,-.81,.85],[2.57,-.62,.65],[3.18,-.36,.40]],front:"cobalt"},
  outer:{shade:"white",depth:.96,profile:[[0,.30,.34],[1.12,-.43,.73],[2.53,-.57,.96],[4.47,-.72,1.26],[6.55,-.91,1.47],[7.59,-1.03,1.50],[8.24,-.66,1.19],[8.82,-.69,1.24],[10.65,-.79,1.30],[12.12,-.63,1.06],[13.10,-.43,.81],[14.18,-.29,.49]],front:"porcelain",edge:"cobalt"},
  crest:{shade:"white",depth:.91,profile:[[0,-.025,.025],[1.20,-.72,.72],[3.44,-1.45,1.45],[4.46,-1.25,1.25]],front:"porcelain"},
  inner:{shade:"charcoal",depth:.74,profile:[[0,.36,.50],[1.20,-.15,.50],[2.93,-.90,.50],[4.24,-1.02,.50],[5.71,-1.16,.50],[6.65,-1.11,.50],[7.60,-.96,.50],[8.34,-.77,.50]],front:"steel"}
 };
 const placements=[
  ["01","上部青コア","core",0,9.30,.80,false],
  ["02","左上白ブレード","upper",-1.99,9.51,.12,false],
  ["03","右上白ブレード","upper",1.99,9.51,.12,true],
  ["04","中央白ノーズ","nose",0,5.18,1.00,false],
  ["05","左肩青＋黄ベント","shoulder",-1.82,7.12,1.06,false],
  ["06","右肩青＋黄ベント","shoulder",1.82,7.12,1.06,true],
  ["07","左下白外装","outer",-2.59,-6.98,.13,false],
  ["08","右下白外装","outer",2.59,-6.98,.13,true],
  ["09","中央白V字装甲","crest",0,1.02,.58,false],
  ["10","左グレー内装","inner",-.72,-6.12,-.30,false],
  ["11","右グレー内装","inner",.72,-6.12,-.30,true]
 ];
 const round=n=>Math.round(n*1e5)/1e5;
 function piece(type,mirror){
  const recipe=recipes[type],stations=recipe.profile,thick=recipe.depth;
  const step=(type==="nose"||type==="crest")?.11:.44;
  let boxes=[];
  function put(from,to,material,outerSide=null){
   if(mirror){
    const old=from[0];from=[-to[0],from[1],from[2]];to=[-old,to[1],to[2]];
    if(outerSide==="west")outerSide="east";
   }
   if(!from.every((v,i)=>v<to[i]-1e-7))throw Error("Invalid box "+type+":"+material);
   boxes.push({from,to,material,...(outerSide?{outerSide}:{}),
     ...(recipe.tilt?{rotation:{origin:[0,0,0],axis:"z",angle:mirror?-recipe.tilt:recipe.tilt,rescale:false}}:{})});
  }
  function bounds(y){for(let i=0;i<stations.length-1;i++){let a=stations[i],b=stations[i+1];if(y<=b[0]+1e-8){let t=Math.max(0,Math.min(1,(y-a[0])/(b[0]-a[0])));return [a[1]+t*(b[1]-a[1]),a[2]+t*(b[2]-a[2])];}}return stations.at(-1).slice(1);}
  const end=stations.at(-1)[0];
  for(let y=0;y<end-1e-6;){
   const slice=type==="outer"&&y<1.32-1e-6?.11:step;
   let y2=Math.min(end,y+slice),aa=bounds(y),bb=bounds(y2),lo=Math.min(aa[0],bb[0]),hi=Math.max(aa[1],bb[1]);
   // Only the OUTWARD lateral side of each white lower fin is cobalt.
   // Keep its front white and its geometry identical.
   put([lo,y,-thick/2],[hi,y2,thick/2],recipe.shade,type==="outer"?"west":null);
   // Keep outer armor an angular white panel: remove striped white overlays.
   // No raised plate per horizontal row; use continuous surfaces instead.
   if(recipe.edge&&y2<end-.25)
     put([lo,y,thick/2+.012],[Math.min(lo+.15,hi-.12),y2,thick/2+.14],recipe.edge);
   y=y2;
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
    // Slanted blue shoulder with a framed gold vent, following reference 1.
    p([-.78,.54,thick/2+.012],[-.59,1.79,thick/2+.19],"cobalt");
    p([.55,.48,thick/2+.012],[.72,1.59,thick/2+.16],"cobalt");
    p([-.56,1.76,thick/2+.014],[.53,2.06,thick/2+.12],"blue");
    // A recessed rectangular vent. The entire independent bit, including
    // this rim and its dark opening, shares the same legal vanilla rotation.
    p([-.54,2.10,thick/2+.024],[.40,2.96,thick/2+.09],"dark");
    p([-.54,2.10,thick/2+.12],[-.37,2.96,thick/2+.26],"gold");
    p([.23,2.10,thick/2+.12],[.40,2.96,thick/2+.26],"gold");
    p([-.37,2.10,thick/2+.12],[.23,2.27,thick/2+.26],"gold");
    p([-.37,2.79,thick/2+.12],[.23,2.96,thick/2+.26],"gold");
    p([-.74,.26,thick/2+.02],[-.53,1.37,thick/2+.13],"trim");
    p([.45,.26,thick/2+.02],[.60,1.38,thick/2+.14],"dark");
    p([-.45,.08,-thick/2-.10],[.45,.58,-thick/2-.015],"graphite");
    p([-.28,.055,thick/2+.020],[.28,.29,thick/2+.095],"white");
    p([-.49,1.05,thick/2+.025],[-.35,2.09,thick/2+.14],"blue");
    p([.33,1.05,thick/2+.025],[.46,2.09,thick/2+.14],"cobalt");
    break;
   case "outer":
    // The lower white GUND-bit and the upper blue shoulder used to stop short
    // of one another. Add a stepped collar at the UPPER (local +Y) edge of
    // EACH white bit: it tucks BEHIND the blue armor and eliminates the
    // visible sky gap without changing the narrow outside silhouette.
    // p() mirrors this geometry automatically for right-hand bit 08.
    // Stepped diagonal joint: top narrows as it slides under the shoulder.
    // Pieces 07/08 are mirrored, so both sides use identical join geometry.
    p([-.12,13.74,-.24],[.38,14.24,.24],"white");
    p([.06,14.08,-.18],[.72,14.56,.25],"porcelain");
    p([.30,14.42,-.14],[.96,14.88,.20],"white");
    // v0.8.29: narrow, stepped INBOARD fairing behind the blue shoulder.
    // Reduce the triangular daylight gap while leaving the outer edge alone.
    // Mirroring this independent bit also covers the right side (bit 08).
    p([.50,13.52,-.26],[.86,14.10,.20],"white");
    p([.70,13.84,-.25],[1.10,14.43,.21],"porcelain");
    p([.87,14.19,-.24],[1.28,14.67,.20],"white");
    // v0.8.31: narrow inner lap joints behind the nose/crest transition.
    // They stay on each independent white bit and do not widen the shield.
    p([.88,11.90,.25],[1.36,12.64,.50],"white");
    p([1.06,12.56,.25],[1.51,13.22,.50],"white");
    p([1.12,13.14,.23],[1.57,13.72,.48],"white");
    // Preserve the thin exterior cobalt seam without a square plug.
    p([-.22,13.86,.34],[-.11,14.24,.48],"cobalt");
    // Continuous white faces fill the inner armor without widening the outer edge.
    p([-.29,2.85,thick/2+.022],[.66,7.10,thick/2+.11],"porcelain");
    p([-.30,8.94,thick/2+.022],[.60,11.95,thick/2+.11],"porcelain");
    p([.77,10.20,thick/2+.025],[.88,11.18,thick/2+.13],"white");
    p([.71,4.82,thick/2+.025],[.83,5.83,thick/2+.13],"white");
    p([-.15,1.10,thick/2+.025],[-.04,1.55,thick/2+.13],"white");
    break;
   case "crest":
    // Thin nested blue V below the white nose; keep the white point exposed.
    for(let y=2.40;y<4.46-1e-6;y+=.11){
     const top=Math.min(4.46,y+.11),half=Math.min(1.02,.025+(top-2.40)*.66);
     p([-half,y,.86],[half,top,.94],"cobalt");
    }
    p([-1.54,3.06,.51],[-1.14,4.37,.63],"porcelain");
    p([1.14,3.06,.51],[1.54,4.37,.63],"porcelain");
    p([-.12,.30,.55],[.12,.88,.77],"white");
    p([-.06,.22,.78],[.06,.53,.83],"steel");
    p([-.73,1.22,.52],[.73,2.68,.60],"porcelain");
    break;
   case "inner":
    p([-.26,2.93,.40],[.24,6.17,.54],"steel");
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
  const cobalt=palette.cobalt,sideFace={
   uv:[round(cobalt[0]/16-d),round(cobalt[1]/16-d),round(cobalt[0]/16+d),round(cobalt[1]/16+d)],texture:"#0"};
  return {from:box.from.map((n,i)=>round(n+8+shift[i])),to:box.to.map((n,i)=>round(n+8+shift[i])),...(box.rotation?{rotation:{...box.rotation,origin:box.rotation.origin.map((n,i)=>round(n+8+shift[i]))}}:{}),faces:Object.fromEntries(["north","south","east","west","up","down"].map(f=>[f,box.outerSide===f?sideFace:face]))};
 });
 // Evaluate rendered corners, not only the unrotated from/to coordinates.
 function corners(box){
  const result=[];
  for(const x of [box.from[0],box.to[0]])for(const y of [box.from[1],box.to[1]])for(const z of [box.from[2],box.to[2]]){
   if(!box.rotation){result.push([x,y,z]);continue;}
   const {origin:o,axis,angle}=box.rotation;
   if(axis!=="z"||![-45,-22.5,0,22.5,45].includes(angle))throw Error("Invalid vanilla rotation");
   const a=angle*Math.PI/180,c=Math.cos(a),s=Math.sin(a);
   result.push([o[0]+c*(x-o[0])-s*(y-o[1]),o[1]+s*(x-o[0])+c*(y-o[1]),z]);
  }
  return result;
 }
 const bitDisplay=(height)=>({gui:{rotation:[15,205,0],translation:[0,0,0],scale:Array(3).fill(round(Math.min(2.6,12/height)))},fixed:{rotation:[0,180,0],translation:[0,0,0],scale:[1,1,1]}});
 // v0.8.38: lower held Escutcheon again, without changing angle, size or geometry.
 // Hand-only display poses: never change model geometry, preview fixed/gui
 // transforms, flying bit models, inventory model keys, or rear handle.
 // 65 deg X reduces the previous 75 deg backward pitch; 165/-165 deg Y
 // turns the outward plate slightly toward the viewer for left/right hands.
 const shieldDisplay={
  gui:{rotation:[15,-25,-5],translation:[0,1,0],scale:[.65,.65,.65]},
  fixed:{rotation:[0,180,0],translation:[0,0,0],scale:[1,1,1]},
  ground:{rotation:[0,0,0],translation:[0,3,0],scale:[.27,.27,.27]},
  firstperson_lefthand:{rotation:[65,165,0],translation:[4,1,-1],scale:[1.34,1.34,1.34]},
  firstperson_righthand:{rotation:[65,-165,0],translation:[-4,1,1],scale:[1.34,1.34,1.34]},
  thirdperson_lefthand:{rotation:[65,165,0],translation:[6,3,-1],scale:[1.40,1.40,1.40]},
  thirdperson_righthand:{rotation:[65,-165,0],translation:[-6,3,1],scale:[1.40,1.40,1.40]}
 };
 const shieldBlockingDisplay={
  ...shieldDisplay,
  firstperson_lefthand:{rotation:[65,165,0],translation:[4,1,-1],scale:[1.49,1.49,1.49]},
  firstperson_righthand:{rotation:[65,-165,0],translation:[-4,1,1],scale:[1.49,1.49,1.49]},
  thirdperson_lefthand:{rotation:[65,165,0],translation:[6,5,-1],scale:[1.56,1.56,1.56]},
  thirdperson_righthand:{rotation:[65,-165,0],translation:[-6,5,1],scale:[1.56,1.56,1.56]}
 };
 const outputs={};
 for(const part of parts){
  const min=[Infinity,Infinity,Infinity],max=[-Infinity,-Infinity,-Infinity];
  for(const box of part.boxes)for(const point of corners(box))for(let i=0;i<3;i++){min[i]=Math.min(min[i],point[i]);max[i]=Math.max(max[i],point[i]);}
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
 // Native shield blocking changes the hand pose, NOT the GUND-bit geometry.
 outputs["resourcepack/assets/funnel/models/item/aerial_v2_escutcheon_blocking.json"]=
  JSON.stringify(model(whole,shieldBlockingDisplay))+"\n";
 const points=whole.flatMap(corners);
 const maxX=Math.max(...points.map(p=>p[0])),minX=Math.min(...points.map(p=>p[0])),maxY=Math.max(...points.map(p=>p[1])),minY=Math.min(...points.map(p=>p[1]));
 if(whole.length!==parts.reduce((a,p)=>a+p.boxes.length,0)+rearMount.length)throw Error("Failed bit-first join and grip");
 if((maxY-minY)/(maxX-minX)<2.77||(maxY-minY)/(maxX-minX)>3.25)throw Error("Shield too round or skinny");
 if(points.some(p=>p.some(n=>!Number.isFinite(n)||n < -16||n > 32)))throw Error("Model out of bounds");
 outputs["docs/AERIAL_BIT_FIRST.md"]=[
 "# Bit-first Escutcheon: lower the equipped shield hand pose v0.8.37",
 "",
 "Each of the 11 detached GUND-bit parts remains independently authored. Reference 1 (the user-authored model) guides appearance; reference 3 (Gunpla assembly and loose parts) guides structure. The assembled preview translates those exact 11 geometries, including their rotation pivots.",
 "",
 "0.8.37 lowers ONLY native equipped Aerial Escutcheon poses after the v0.8.36 in-game screenshot. Both third-person hands change translation Y from 7 to 5 (idle) and 9 to 7 (guard); first-person hands change Y from 3 to 2 in both states. X/Z, rotations, scales, all 493 geometry cuboids, eleven standalone bits, rear grip, fixed/GUI preview poses, colors, rifle and game behavior remain exactly as v0.8.36. Shield item identifiers and blocking-state selector stay unchanged.",
 "",
 "| Bit | Part | Joined offset x,y,z | Cuboids |",
 "| --- | --- | --- | --- |",
 ...parts.map(p=>`| ${p.id} | ${p.label} | ${p.x}, ${p.y}, ${p.z} | ${p.boxes.length} |`),
 "",
 `Total ${whole.length} cuboids (11 standalone bits plus ${rearMount.length} rear-only handle parts); shield width ${round(maxX-minX)} vs height ${round(maxY-minY)} (height/width ratio ${round((maxY-minY)/(maxX-minX))}).`,
 "",
 "Single source of truth: `tools/generate_aerial_bit_first.mjs`; generates 11 individual models, the joined shield, and the same shield geometry with a blocking hand pose (13 JSON model files). Regenerate with `node tools/generate_aerial_bit_first.mjs` or check with `--check`.",
 "",
 "Live Aerial flying bits and native held shield now reference the generated bit-first models. OP previews remain available. The gunbit rifle and original `.bbmodel` sources are unchanged. The five-cuboid rear grip belongs to the shield only. No BetterModel needed.",
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
else console.log(check?'Bit-first generated models match.':'Regenerated 11 bits and joined/raised shield poses.');
