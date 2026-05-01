const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  HeadingLevel, AlignmentType, BorderStyle, WidthType, ShadingType,
  VerticalAlign, LevelFormat, PageNumber, Header, Footer, PageBreak,
  TabStopType, TabStopPosition
} = require('docx');
const fs = require('fs');
const path = require('path');

// ===== 读取 MD 文件 =====
const mdPath = "C:/Users/zanboring/OneDrive - Ormesby Primary/Desktop/AI\u62db\u8058\u6570\u636e\u53ef\u89c6\u5316\u7cfb\u7edf\u7684\u5f00\u53d1\u4e0e\u8bbe\u8ba1_\u8bba\u6587\u7ec8\u7a3f.md";
const mdContent = fs.readFileSync(mdPath, 'utf-8');
const lines = mdContent.split('\n');

// ===== 颜色与字体常量 =====
const FONT_MAIN = "\u4eff\u5b8b";       // 仿宋
const FONT_TITLE = "\u9ed1\u4f53";      // 黑体
const FONT_CODE = "Courier New";
const COLOR_HEAD = "1F3864";            // 深蓝
const COLOR_TABLE_HEADER = "2E75B6";   // 蓝色表头
const COLOR_CODE_BG = "F5F5F5";
const A4_WIDTH_DXA = 11906;
const A4_MARGIN = 1440; // 1 inch
const CONTENT_WIDTH = A4_WIDTH_DXA - 2 * A4_MARGIN; // ~9026

// ===== 边框辅助 =====
const thinBorder = { style: BorderStyle.SINGLE, size: 4, color: "BBBBBB" };
const thickBorder = { style: BorderStyle.SINGLE, size: 8, color: COLOR_TABLE_HEADER };
const cellBorders = { top: thinBorder, bottom: thinBorder, left: thinBorder, right: thinBorder };
const headerCellBorders = { top: thickBorder, bottom: thickBorder, left: thinBorder, right: thinBorder };

// ===== 解析行内粗体 / 代码 =====
function parseInlineRuns(text) {
  const runs = [];
  // 支持 **bold** 和 `code`
  const re = /(\*\*(.+?)\*\*|`([^`]+)`)/g;
  let lastIdx = 0;
  let m;
  while ((m = re.exec(text)) !== null) {
    if (m.index > lastIdx) {
      runs.push(new TextRun({ text: text.slice(lastIdx, m.index), font: FONT_MAIN, size: 24 }));
    }
    if (m[2]) {
      runs.push(new TextRun({ text: m[2], bold: true, font: FONT_MAIN, size: 24 }));
    } else if (m[3]) {
      runs.push(new TextRun({ text: m[3], font: FONT_CODE, size: 22, shading: { fill: "EEEEEE", type: ShadingType.CLEAR } }));
    }
    lastIdx = m.index + m[0].length;
  }
  if (lastIdx < text.length) {
    runs.push(new TextRun({ text: text.slice(lastIdx), font: FONT_MAIN, size: 24 }));
  }
  return runs.length > 0 ? runs : [new TextRun({ text, font: FONT_MAIN, size: 24 })];
}

// ===== 构建表格 =====
function buildTable(tableLines) {
  const rows = [];
  let isFirst = true;
  for (const line of tableLines) {
    if (/^\|[\s\-|:]+\|$/.test(line.trim())) continue; // 分隔行
    const cells = line.trim().replace(/^\|/, '').replace(/\|$/, '').split('|');
    const tableCells = cells.map(cell => {
      const cellText = cell.trim();
      const isHeader = isFirst;
      return new TableCell({
        borders: isHeader ? headerCellBorders : cellBorders,
        shading: isHeader
          ? { fill: COLOR_TABLE_HEADER, type: ShadingType.CLEAR }
          : { fill: "FFFFFF", type: ShadingType.CLEAR },
        margins: { top: 80, bottom: 80, left: 120, right: 120 },
        children: [new Paragraph({
          children: parseInlineRuns(cellText),
          alignment: AlignmentType.LEFT,
          ...(isHeader ? {
            shading: undefined
          } : {})
        })]
      });
    });
    // 均分宽度
    const colW = Math.floor(CONTENT_WIDTH / cells.length);
    const adjustedCells = cells.map((cell, idx) => {
      const cellText = cell.trim();
      const isHeader = isFirst;
      return new TableCell({
        borders: isHeader ? headerCellBorders : cellBorders,
        width: { size: colW, type: WidthType.DXA },
        shading: isHeader
          ? { fill: COLOR_TABLE_HEADER, type: ShadingType.CLEAR }
          : { fill: "FFFFFF", type: ShadingType.CLEAR },
        margins: { top: 80, bottom: 80, left: 120, right: 120 },
        children: [new Paragraph({
          children: isHeader
            ? [new TextRun({ text: cellText, bold: true, color: "FFFFFF", font: FONT_TITLE, size: 22 })]
            : parseInlineRuns(cellText),
          alignment: AlignmentType.LEFT
        })]
      });
    });
    rows.push(new TableRow({
      tableHeader: isFirst,
      children: adjustedCells
    }));
    isFirst = false;
  }
  if (rows.length === 0) return null;
  const firstRowCells = tableLines[0].trim().replace(/^\|/, '').replace(/\|$/, '').split('|').length;
  const colWidth = Math.floor(CONTENT_WIDTH / firstRowCells);
  return new Table({
    width: { size: CONTENT_WIDTH, type: WidthType.DXA },
    columnWidths: Array(firstRowCells).fill(colWidth),
    rows
  });
}

// ===== 构建代码块 =====
function buildCodeBlock(codeLines, lang) {
  const children = [];
  for (const cl of codeLines) {
    children.push(new Paragraph({
      children: [new TextRun({
        text: cl || ' ',
        font: FONT_CODE,
        size: 18,
        color: "333333"
      })],
      spacing: { before: 20, after: 20, line: 240, lineRule: "exact" },
      shading: { fill: COLOR_CODE_BG, type: ShadingType.CLEAR },
      indent: { left: 200, right: 200 }
    }));
  }
  return children;
}

// ===== 主解析函数 =====
function parseMd(lines) {
  const children = [];

  // === 封面 ===
  children.push(new Paragraph({
    children: [new TextRun({
      text: "AI\u62db\u8058\u6570\u636e\u53ef\u89c6\u5316\u7cfb\u7edf\u7684\u5f00\u53d1\u4e0e\u8bbe\u8ba1",
      bold: true, font: FONT_TITLE, size: 56, color: COLOR_HEAD
    })],
    alignment: AlignmentType.CENTER,
    spacing: { before: 2880, after: 480 }
  }));
  children.push(new Paragraph({
    children: [new TextRun({ text: "\u534e\u5317\u7406\u5de5\u5927\u5b66 \u7f51\u7edc\u5de5\u7a0b\u4e13\u4e1a", font: FONT_MAIN, size: 28 })],
    alignment: AlignmentType.CENTER,
    spacing: { after: 240 }
  }));
  children.push(new Paragraph({
    children: [new TextRun({ text: "\u5f20\u535a\u4ec1", font: FONT_MAIN, size: 28 })],
    alignment: AlignmentType.CENTER,
    spacing: { after: 240 }
  }));
  children.push(new Paragraph({
    children: [new TextRun({ text: "2026\u5c4a\u6bd5\u4e1a\u8bba\u6587", font: FONT_MAIN, size: 28 })],
    alignment: AlignmentType.CENTER,
    spacing: { after: 4800 }
  }));
  children.push(new Paragraph({ children: [new PageBreak()] }));

  let i = 0;
  let inCode = false;
  let codeLang = '';
  let codeLines = [];
  let inTable = false;
  let tableLines = [];
  let inBullet = false;

  while (i < lines.length) {
    let line = lines[i];
    const trimmed = line.trim();

    // --- 代码块 ---
    if (trimmed.startsWith('```')) {
      if (!inCode) {
        inCode = true;
        codeLang = trimmed.slice(3).trim();
        codeLines = [];
      } else {
        inCode = false;
        const codeBlocks = buildCodeBlock(codeLines, codeLang);
        children.push(new Paragraph({
          children: [new TextRun({ text: codeLang ? `[${codeLang}]` : '[code]', font: FONT_CODE, size: 18, color: "777777", bold: true })],
          spacing: { before: 120, after: 60 },
          shading: { fill: "E8EDF2", type: ShadingType.CLEAR },
          border: { left: { style: BorderStyle.SINGLE, size: 12, color: COLOR_TABLE_HEADER } },
          indent: { left: 200 }
        }));
        children.push(...codeBlocks);
        children.push(new Paragraph({
          children: [new TextRun({ text: '' })],
          spacing: { after: 120 }
        }));
        codeLines = [];
        codeLang = '';
      }
      i++;
      continue;
    }
    if (inCode) {
      codeLines.push(line);
      i++;
      continue;
    }

    // --- 表格 ---
    if (trimmed.startsWith('|')) {
      if (!inTable) {
        inTable = true;
        tableLines = [];
      }
      tableLines.push(trimmed);
      i++;
      continue;
    } else if (inTable) {
      inTable = false;
      const tbl = buildTable(tableLines);
      if (tbl) {
        children.push(new Paragraph({ children: [], spacing: { before: 120 } }));
        children.push(tbl);
        children.push(new Paragraph({ children: [], spacing: { after: 240 } }));
      }
      tableLines = [];
    }

    // --- 空行 ---
    if (!trimmed) {
      inBullet = false;
      i++;
      continue;
    }

    // --- 水平线 ---
    if (/^---+$/.test(trimmed)) {
      children.push(new Paragraph({
        children: [new TextRun({ text: '' })],
        border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: "AAAAAA" } },
        spacing: { before: 240, after: 240 }
      }));
      i++;
      continue;
    }

    // --- 标题 ---
    const h1 = trimmed.match(/^# (.+)/);
    const h2 = trimmed.match(/^## (.+)/);
    const h3 = trimmed.match(/^### (.+)/);
    const h4 = trimmed.match(/^#### (.+)/);
    const h5 = trimmed.match(/^##### (.+)/);

    if (h1) {
      // 文章主标题单独处理
      children.push(new Paragraph({
        children: [new TextRun({ text: h1[1], bold: true, font: FONT_TITLE, size: 48, color: COLOR_HEAD })],
        alignment: AlignmentType.CENTER,
        spacing: { before: 480, after: 480 }
      }));
      i++; continue;
    }
    if (h2) {
      children.push(new Paragraph({
        heading: HeadingLevel.HEADING_1,
        children: [new TextRun({ text: h2[1], bold: true, font: FONT_TITLE, size: 36, color: COLOR_HEAD })],
        spacing: { before: 400, after: 200 }
      }));
      i++; continue;
    }
    if (h3) {
      children.push(new Paragraph({
        heading: HeadingLevel.HEADING_2,
        children: [new TextRun({ text: h3[1], bold: true, font: FONT_TITLE, size: 30 })],
        spacing: { before: 300, after: 160 }
      }));
      i++; continue;
    }
    if (h4) {
      children.push(new Paragraph({
        heading: HeadingLevel.HEADING_3,
        children: [new TextRun({ text: h4[1], bold: true, font: FONT_TITLE, size: 26 })],
        spacing: { before: 240, after: 120 }
      }));
      i++; continue;
    }
    if (h5) {
      children.push(new Paragraph({
        heading: HeadingLevel.HEADING_4,
        children: [new TextRun({ text: h5[1], bold: true, font: FONT_TITLE, size: 24 })],
        spacing: { before: 200, after: 100 }
      }));
      i++; continue;
    }

    // --- 列表项（- 或 数字.) ---
    const bulletMatch = trimmed.match(/^[-*] (.+)/);
    const numMatch = trimmed.match(/^(\d+)\. (.+)/);
    const subBulletMatch = trimmed.match(/^  - (.+)/);
    if (bulletMatch) {
      children.push(new Paragraph({
        children: parseInlineRuns(bulletMatch[1]),
        bullet: { level: 0 },
        spacing: { before: 60, after: 60 },
        indent: { left: 720, hanging: 360 }
      }));
      i++; continue;
    }
    if (subBulletMatch) {
      children.push(new Paragraph({
        children: parseInlineRuns(subBulletMatch[1]),
        bullet: { level: 1 },
        spacing: { before: 40, after: 40 },
        indent: { left: 1080, hanging: 360 }
      }));
      i++; continue;
    }
    if (numMatch) {
      children.push(new Paragraph({
        children: [
          new TextRun({ text: `${numMatch[1]}. `, bold: true, font: FONT_MAIN, size: 24 }),
          ...parseInlineRuns(numMatch[2])
        ],
        spacing: { before: 60, after: 60 },
        indent: { left: 720, hanging: 420 }
      }));
      i++; continue;
    }

    // --- **【图x-x 图标题】** 这类图说明 ---
    const figureMatch = trimmed.match(/^\*\*【(.+?)】\*\*$/);
    if (figureMatch) {
      children.push(new Paragraph({
        children: [new TextRun({ text: `【${figureMatch[1]}】`, bold: true, font: FONT_TITLE, size: 22, color: "666666" })],
        alignment: AlignmentType.CENTER,
        spacing: { before: 160, after: 160 }
      }));
      i++; continue;
    }

    // --- 普通段落 ---
    const runs = parseInlineRuns(trimmed);
    children.push(new Paragraph({
      children: runs,
      spacing: { before: 80, after: 80, line: 360, lineRule: "auto" },
      indent: { firstLine: 480 }
    }));
    i++;
  }

  // 处理未关闭的表格
  if (inTable && tableLines.length > 0) {
    const tbl = buildTable(tableLines);
    if (tbl) {
      children.push(tbl);
    }
  }

  return children;
}

// ===== 构建文档 =====
const children = parseMd(lines);

const doc = new Document({
  creator: "\u5f20\u535a\u4ec1",
  title: "AI\u62db\u8058\u6570\u636e\u53ef\u89c6\u5316\u7cfb\u7edf\u7684\u5f00\u53d1\u4e0e\u8bbe\u8ba1",
  description: "2026\u5c4a\u6bd5\u4e1a\u8bba\u6587",
  numbering: {
    config: [
      {
        reference: "default-bullets",
        levels: [
          {
            level: 0, format: LevelFormat.BULLET, text: "\u25cf",
            alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 720, hanging: 360 } } }
          },
          {
            level: 1, format: LevelFormat.BULLET, text: "\u25cb",
            alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 1080, hanging: 360 } } }
          }
        ]
      }
    ]
  },
  styles: {
    default: {
      document: {
        run: { font: FONT_MAIN, size: 24, color: "000000" }
      }
    },
    paragraphStyles: [
      {
        id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 36, bold: true, font: FONT_TITLE, color: COLOR_HEAD },
        paragraph: { spacing: { before: 400, after: 200 }, outlineLevel: 0,
          border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: COLOR_TABLE_HEADER } } }
      },
      {
        id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 30, bold: true, font: FONT_TITLE, color: "1F5490" },
        paragraph: { spacing: { before: 300, after: 160 }, outlineLevel: 1 }
      },
      {
        id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 26, bold: true, font: FONT_TITLE },
        paragraph: { spacing: { before: 240, after: 120 }, outlineLevel: 2 }
      },
      {
        id: "Heading4", name: "Heading 4", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 24, bold: true, font: FONT_TITLE },
        paragraph: { spacing: { before: 200, after: 100 }, outlineLevel: 3 }
      }
    ]
  },
  sections: [{
    properties: {
      page: {
        size: { width: A4_WIDTH_DXA, height: 16838 },
        margin: { top: A4_MARGIN, right: A4_MARGIN, bottom: A4_MARGIN, left: 1800 }
      }
    },
    headers: {
      default: new Header({
        children: [new Paragraph({
          children: [
            new TextRun({ text: "AI\u62db\u8058\u6570\u636e\u53ef\u89c6\u5316\u7cfb\u7edf\u7684\u5f00\u53d1\u4e0e\u8bbe\u8ba1", font: FONT_MAIN, size: 18, color: "777777" }),
            new TextRun({ text: "\t\u5f20\u535a\u4ec1 / 2026\u5c4a", font: FONT_MAIN, size: 18, color: "777777" })
          ],
          tabStops: [{ type: TabStopType.RIGHT, position: TabStopPosition.MAX }],
          border: { bottom: { style: BorderStyle.SINGLE, size: 4, color: "CCCCCC" } }
        })]
      })
    },
    footers: {
      default: new Footer({
        children: [new Paragraph({
          children: [
            new TextRun({ text: "\u534e\u5317\u7406\u5de5\u5927\u5b66 \u7f51\u7edc\u5de5\u7a0b\u4e13\u4e1a", font: FONT_MAIN, size: 18, color: "777777" }),
            new TextRun({ text: "\t\u7b2c ", font: FONT_MAIN, size: 18, color: "777777" }),
            new TextRun({ children: [PageNumber.CURRENT], font: FONT_MAIN, size: 18, color: "777777" }),
            new TextRun({ text: " \u9875", font: FONT_MAIN, size: 18, color: "777777" })
          ],
          tabStops: [{ type: TabStopType.RIGHT, position: TabStopPosition.MAX }],
          border: { top: { style: BorderStyle.SINGLE, size: 4, color: "CCCCCC" } }
        })]
      })
    },
    children
  }]
});

// ===== 输出文档 =====
const outputPath = "C:/Users/zanboring/OneDrive - Ormesby Primary/Desktop/AI\u62db\u8058\u6570\u636e\u53ef\u89c6\u5316\u7cfb\u7edf\u7684\u5f00\u53d1\u4e0e\u8bbe\u8ba1_\u8bba\u6587\u7ec8\u7a3f.docx";
Packer.toBuffer(doc).then(buffer => {
  fs.writeFileSync(outputPath, buffer);
  console.log('SUCCESS: ' + outputPath);
}).catch(err => {
  console.error('ERROR:', err.message);
  process.exit(1);
});
