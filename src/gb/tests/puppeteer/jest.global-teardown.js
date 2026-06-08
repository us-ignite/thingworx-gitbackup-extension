const fs = require('fs');
const path = require('path');

module.exports = async () => {
  console.log('Jest global teardown: all tests complete.');
  const thingNameFile = path.join(__dirname, '.thing-name');
  try {
    if (fs.existsSync(thingNameFile)) {
      fs.unlinkSync(thingNameFile);
    }
  } catch {
    // ignore
  }
};
