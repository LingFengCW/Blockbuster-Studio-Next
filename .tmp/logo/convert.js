const sharp = require('sharp');

sharp('logo.svg', { density: 512 })
    .resize(128, 128)
    .png()
    .toFile('icon.png')
    .then(() => console.log('done 128x128'))
    .catch((e) => { console.error(e); process.exit(1); });
