// Validate reachable pressure model inheritance, face slots, textures and recipe structure.
// Runtime baking, visuals and gameplay are checked separately by the Forge QA fixtures.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..', 'resources');
const assets = path.join(root, 'assets/pressure');
let models = new Set(), textures = new Set(), issues = [];
function read(file) { return JSON.parse(fs.readFileSync(file, 'utf8')); }
function model(id) {
  if (id.startsWith('minecraft:')) return null;
  if (!id.startsWith('pressure:')) throw new Error('Unexpected model namespace: ' + id);
  const file = path.join(assets, 'models', id.slice(9) + '.json');
  if (!fs.existsSync(file)) throw new Error('Missing model ' + id);
  return read(file);
}
function trace(id, inherited = {}, chain = []) {
  if (!id.includes(':')) id = 'minecraft:' + id;
  if (chain.includes(id)) throw new Error('Parent cycle: ' + [...chain, id].join(' -> '));
  const m = model(id);
  if (!m) return;
  models.add(id);
  // Child slots override the parent slots, including aliases resolved in ancestor geometry.
  const slots = { ...m.textures, ...inherited };
  for (const value of Object.values(slots)) texture(value, slots);
  for (const element of m.elements || []) for (const face of Object.values(element.faces)) {
    if (!face.texture.startsWith('#')) throw new Error(id + ': element face must reference a named texture slot: ' + face.texture);
    texture(face.texture, slots);
  }
  if (m.parent && !m.parent.startsWith('builtin/')) trace(m.parent, slots, [...chain, id]);
}
function texture(value, slots) {
  const seen = new Set();
  while (value.startsWith('#')) {
    if (seen.has(value)) throw new Error('Texture alias cycle: ' + value);
    seen.add(value);
    if (!slots[value.slice(1)]) throw new Error('Unresolved texture alias: ' + value);
    value = slots[value.slice(1)];
  }
  if (!value.startsWith('pressure:')) return;
  textures.add(value);
  if (/^pressure:(blocks|items)\//.test(value)) throw new Error('Legacy atlas path: ' + value);
  if (!fs.existsSync(path.join(assets, 'textures', value.slice(9) + '.png'))) throw new Error('Missing texture: ' + value);
}
function collect(value) {
  if (!value || typeof value !== 'object') return;
  if (value.model) trace(value.model);
  for (const child of Object.values(value)) collect(child);
}
for (const file of fs.readdirSync(path.join(assets, 'blockstates'))) {
  try { collect(read(path.join(assets, 'blockstates', file))); } catch (e) { issues.push(file + ': ' + e.message); }
}
for (const file of fs.readdirSync(path.join(assets, 'models/item')).filter(f => f.endsWith('.json'))) {
  try { trace('pressure:item/' + file.slice(0, -5)); } catch (e) { issues.push(file + ': ' + e.message); }
}
const recipes = fs.readdirSync(path.join(root, 'data/pressure/recipes')).filter(f => f.endsWith('.json'));
for (const file of recipes) {
  const recipe = read(path.join(root, 'data/pressure/recipes', file));
  if (recipe.type === 'minecraft:crafting_shaped') {
    for (const row of recipe.pattern) for (const symbol of row)
      if (symbol !== ' ' && !recipe.key[symbol]) issues.push(file + ': no ingredient for pattern symbol ' + symbol);
    for (const symbol of Object.keys(recipe.key))
      if (!recipe.pattern.some(row => row.includes(symbol))) issues.push(file + ': unused ingredient ' + symbol);
  }
}
console.log(JSON.stringify({ models: models.size, textures: textures.size, recipes: recipes.length, issues }, null, 2));
process.exitCode = issues.length ? 1 : 0;
