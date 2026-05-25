const { PDFParse } = require('pdf-parse');
const fs = require('fs');

const pdfBuffer = fs.readFileSync('analises/TCC (1).pdf');
const parser = new PDFParse({ data: pdfBuffer });
parser.getText().then(data => {
  const text = data.text;
  console.log('TOTAL PAGINAS:', data.total);
  const chunkSize = 6000;
  for (let i = 0; i < Math.min(text.length, 60000); i += chunkSize) {
    console.log(`\n=== TRECHO ${Math.floor(i/chunkSize)+1} ===\n`);
    console.log(text.substring(i, i + chunkSize));
  }
}).catch(e => console.error(e));
