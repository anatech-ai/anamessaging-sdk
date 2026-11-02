// Writes dist/index.mjs: an ES-module entry that re-exports the CommonJS build, so both
// `import { AnaMessagingClient } from '@anamessaging/sdk'` and `require('@anamessaging/sdk')` work.
const fs = require('fs');
const path = require('path');
const dist = path.join(__dirname, '..', 'dist');
const names = Object.keys(require(path.join(dist, 'index.js'))).filter((n) => n !== 'default' && n !== '__esModule');
fs.writeFileSync(
  path.join(dist, 'index.mjs'),
  `import cjs from './index.js';\nexport const { ${names.join(', ')} } = cjs;\nexport default cjs;\n`,
);
