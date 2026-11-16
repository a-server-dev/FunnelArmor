// GUND-bit-first source: eleven independent models assembled by translation.
// Generated assets are committed for Java/Gradle builds without a Node dependency.
import fs from 'node:fs';
import path from 'node:path';
function main(){
  // Independent bit recipes based on the supplied disassembled model photo.
  // The shield is created by joining bits, never by cutting a completed shield.
  const palette={white:[22.5,22.5],porcelain:[70.5,22.5],blue:[166.5,22.5],cobalt:[214.5,22.5],trim:[70.5,70.5],gold:[118.5,70.5],dark:[214.5,70.5],graphite:[22.5,118.5],charcoal:[70.5,118.5],steel:[118.5,118.5],bore:[166.5,118.5],shine:[22.5,166.5]};
  // Profiles use Y, left X, right X: each is an independently authored detached bit.
  const recipes={
    core:{shade:"blue",depth:.80,profile:[[0,-1.05,1.05],[.7,-1.4,1.4],[1.6,-1.0,1.0],[3.7,-.8,.8],[4.9,-.48,.48]],
      detail:[[[-1.4,.6,.4],[1.4,1.45,.61],"cobalt"],[[-1.2,1.1,.61],[-.72,1.65,.75],"steel"],[[.72,1.1,.61],[1.2,1.65,.75],"steel"],[[-.99,1.25,.76],[-.79,1.49,.81],"bore"],[[.79,1.25,.76],[.99,1.49,.81],"bore"],[[-.3,3.3,.42],[.3,3.9,.6],"dark"],[[-.15,3.48,.61],[.15,3.74,.68],"bore"],[[-.81,4.53,.03],[-.58,5.5,.35],"porcelain"],[[.58,4.53,.03],[.81,5.5,.35],"porcelain"]]},
    upper:{shade:"white",depth:.58,profile:[[0,-.72,.78],[.85,-1,.95],[2,-.79,.83],[3.95,-.57,.67],[5.9,-.35,.4]],
      detail:[[[-.70,.30,.30],[.57,4.3,.42],"porcelain"],[[-.75,.40,.44],[-.56,1.34,.55],"steel"],[[-.66,.65,.56],[-.58,1.2,.59],"bore"]]},
    nose:{shade:"white",depth:.74,profile:[[0,-.58,.58],[.40,-1.05,1.05],[.95,-1.31,1.31],[2.45,-1.09,1.09],[3.75,-.51,.51]],
      detail:[[[-1.06,.1,.39],[1.06,.40,.54],"cobalt"],[[-.28,2.80,.43],[.28,3.43,.55],"porcelain"]]},
    shoulder:{shade:"blue",depth:.85,profile:[[0,-.85,.85],[.6,-1.04,1.02],[1.62,-1.1,1.01],[2.43,-.84,.81],[3.35,-.59,.6]],
      detail:[[[-.86,2.52,.44],[.71,3.14,.69],"gold"],[[-.66,2.65,.70],[.49,3.01,.78],"gold"],[[-.9,.27,.44],[-.71,1.36,.60],"trim"],[[.54,.2,.39],[.70,1.51,.57],"dark"],[[-.60,.1,-.53],[.52,.8,-.39],"graphite"]]},
    outer:{shade:"white",depth:.58,profile:[[0,-.33,.34],[2,-.49,.49],[4,-.62,.51],[6.3,-.98,.76],[7.3,-1.06,.79],[8.15,-.64,.52],[9.75,-.84,.72],[11.7,-.74,.71],[13.05,-.48,.55],[14.2,-.32,.37]],
      detail:[[[-.57,0,.25],[-.31,12.4,.47],"cobalt"],[[.26,3.2,.33],[.39,5.1,.51],"steel"],[[.25,8.9,.33],[.37,10,.51],"steel"]]},
    crest:{shade:"white",depth:.46,profile:[[0,-.5,.5],[.55,-.94,.94],[2,-1.34,1.34],[3.55,-1.58,1.58],[4.42,-1.67,1.67]],
      detail:[[[-.19,.1,.26],[.19,.7,.48],"porcelain"],[[-1.42,3.10,.30],[-1.11,4.4,.51],"porcelain"],[[1.11,3.10,.30],[1.42,4.4,.51],"porcelain"],[[-1.55,2.30,-.35],[1.55,2.58,-.26],"steel"]]},
    inner:{shade:"charcoal",depth:.57,profile:[[0,-.28,.30],[1.75,-.44,.39],[4.05,-.52,.49],[5.82,-.76,.62],[7.15,-.69,.61],[8.92,-.39,.42]],
      detail:[[[-.32,.35,.30],[.28,7.1,.40],"steel"],[[.11,3.12,.41],[.19,5.25,.50],"graphite"],[[-.27,6.16,.40],[.24,6.32,.47],"dark"]]}
  };
  const placements=[
    ["01","上部青コア", "core",0,8.1,.4,false],
    ["02","左上白ロングブレード","upper",-2.40,9.13,.10,false],
    ["03","右上白ロングブレード","upper",2.40,9.13,.10,true],
    ["04","中央白ノーズ","nose",0,3.98,.46,false],
    ["05","左肩青・黄色ベント","shoulder",-3.69,2.36,.75,false],
    ["06","右肩青・黄色ベント","shoulder",3.69,2.36,.75,true],
    ["07","左下白ロングレール","outer",-4.30,-13.3,0,false],
    ["08","右下白ロングレール","outer",4.30,-13.3,0,true],
    ["09","中央白V字アーマー","crest",0,-3.1,.84,false],
    ["10","左グレー内側ブレード","inner",-1.43,-12.6,.06,false],
    ["11","右グレー内側ブレード","inner",1.43,-12.6,.06,true]
  ];
  const round=n=>Math.round(n*1e5)/1e5;
  function piece(recipe,mirror){
    const boxes=[];
    const put=(from,to,mat)=>{
      if(mirror){const f=from[0];from=[-to[0],from[1],from[2]];to=[-f,to[1],to[2]];}
      if(!from.every((v,i)=>v<to[i]))throw Error("Invalid box");
      boxes.push({from,to,mat});
    };
    const stations=recipe.profile,last=stations[stations.length-1][0],step=.43;
    for(let y=0;y<last-1e-6;y+=step){
      const top=Math.min(last,y+step);
      function edges(Y){for(let i=0;i<stations.length-1;i++){
        const a=stations[i],b=stations[i+1]; if(Y<=b[0]+1e-7){const u=Math.max(0,Math.min(1,(Y-a[0])/(b[0]-a[0])));return [a[1]+u*(b[1]-a[1]),a[2]+u*(b[2]-a[2])];}
      }return stations.at(-1).slice(1);}
      const a=edges(y),b=edges(top);
      put([Math.min(a[0],b[0]),y,-recipe.depth/2],[Math.max(a[1],b[1]),top,recipe.depth/2],recipe.shade);
    }
    for(const [from,to,mat] of recipe.detail)put(from,to,mat);
    return boxes;
  }
  const items=placements.map(([id,label,type,x,y,z,mirror])=>({id,label,x,y,z,boxes:piece(recipes[type],mirror)}));
  const model=(elements,display)=>({parent:"minecraft:block/block",ambientocclusion:false,textures:{"0":"funnel:item/aerial_escutcheon",particle:"funnel:item/aerial_escutcheon"},elements,display,gui_light:"front"});
  const make=(boxes,shift)=>boxes.map(b=>{
    const center=palette[b.mat];const u=center[0]/16,v=center[1]/16,delta=.020;
    const face={uv:[round(u-delta),round(v-delta),round(u+delta),round(v+delta)],texture:"#0"};
    return {from:b.from.map((n,i)=>round(n+8+shift[i])),to:b.to.map((n,i)=>round(n+8+shift[i])),faces:Object.fromEntries(["north","south","east","west","up","down"].map(f=>[f,face]))};
  });
  const bitDisplay=h=>({gui:{rotation:[18,200,0],translation:[0,0,0],scale:Array(3).fill(round(Math.min(2.8,13/h)))},fixed:{rotation:[0,180,0],translation:[0,0,0],scale:[1,1,1]}});
  const shieldDisplay={gui:{rotation:[15,-25,-5],translation:[0,1,0],scale:[.65,.65,.65]},fixed:{rotation:[0,180,0],translation:[0,0,0],scale:[1,1,1]}};
  const outputs={};let total=0;
  for(const item of items){
    const min=Array(3).fill(Infinity),max=Array(3).fill(-Infinity);
    for(const b of item.boxes)for(let i=0;i<3;i++){min[i]=Math.min(min[i],b.from[i]);max[i]=Math.max(max[i],b.to[i]);}
    const shift=min.map((n,i)=>(-n-max[i])/2);
    const key="aerial_v2_bit_"+item.id;
    outputs["resourcepack/assets/funnel/models/item/"+key+".json"]=JSON.stringify(model(make(item.boxes,shift),bitDisplay(max[1]-min[1])),null,2)+"\n";
    outputs["resourcepack/assets/funnel/items/"+key+".json"]=JSON.stringify({model:{type:"minecraft:model",model:"funnel:item/"+key}},null,2)+"\n";
    total+=item.boxes.length;
  }
  const full=items.flatMap(item=>make(item.boxes,[item.x,item.y,item.z]));
  if(full.length!==total||items.length!==11)throw Error("Assembly does not match 11 constituent bits");
  outputs["resourcepack/assets/funnel/models/item/aerial_v2_escutcheon.json"]=JSON.stringify(model(full,shieldDisplay),null,2)+"\n";
  outputs["resourcepack/assets/funnel/items/aerial_v2_escutcheon.json"]=JSON.stringify({model:{type:"minecraft:model",model:"funnel:item/aerial_v2_escutcheon"}},null,2)+"\n";
  outputs["docs/AERIAL_BIT_FIRST.md"]=[
    "# Aerial bit-first preview (not yet approved)",
    "",
    "The eleven GUND bits are independently authored from the user's physical disassembled shield reference. The joined shield is generated **only by translating the exact same eleven bit geometries**, without sourcing geometry from the old assembled shield.",
    "",
    "| Bit | Reference part | Joined X,Y,Z in pixels |",
    "| --- | --- | --- |",
    ...items.map(p=>`| ${p.id} | ${p.label} | ${p.x}, ${p.y}, ${p.z} |`),
    "",
    "The separate charcoal-colored frame at the far right of the reference photo is a mounting frame, not one of the eleven detached GUND-bit parts. This preview does not include it.",
    "",
    "**Source of truth**: `tools/generate_aerial_bit_first.mjs`; `node tools/generate_aerial_bit_first.mjs` regenerates all models, and `--check` verifies the committed pack. All colors come from the existing `aerial_escutcheon.png` palette. No `.bbmodel` is altered or uploaded.",
    "",
    "Use `/funnel aerial preview 1` through `11` (OP-only) to view one piece or `/funnel aerial preview shield` to see all eleven joined. `/funnel aerial preview clear` removes the temporary nonpersistent display. Production GUND bits and shield remain unchanged until approved.",
    "",
    "The preview is a first-pass, vanilla cuboid approximation intended for iterative inspection of each piece's taper, layering, accent colors and 11-to-1 fit.",
    ""
  ].join("\n");
  return {outputs,items:items.map(x=>({id:x.id,label:x.label,count:x.boxes.length})),total};
}
const {outputs}=main();
const check=process.argv.includes('--check');
let stale=0;
for(const [name,contents] of Object.entries(outputs)){
  const target=path.resolve(name);
  if(check){
    if(!fs.existsSync(target)||fs.readFileSync(target,'utf8')!==contents){console.error('STALE '+name);stale++;}
  }else{
    fs.mkdirSync(path.dirname(target),{recursive:true});fs.writeFileSync(target,contents,'utf8');
    console.log('WROTE '+name);
  }
}
if(stale)process.exitCode=1;
else console.log(check?'All GUND-bit-first assets up to date.':'Generated eleven bits and their assembled shield.');
