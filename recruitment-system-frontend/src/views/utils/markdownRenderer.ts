/**
 * Markdown 渲染器配置
 *
 * 为 AIChat 组件提供美化的 Markdown 渲染支持
 * 包含：标题、段落、列表、代码块、表格、引用块等自定义渲染
 * 所有颜色值均使用 CSS 变量（通过 getComputedStyle 获取）
 */
import { marked, Renderer } from 'marked';

/**
 * 从 CSS 变量获取实际颜色值
 * 由于渲染器运行时需要具体色值（内联 style），这里从 document 读取 CSS 变量
 */
function cssVar(name: string, fallback: string): string {
  if (typeof document !== 'undefined' && document.documentElement) {
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
  }
  return fallback;
}

/** 创建并返回配置好的 marked 渲染器实例 */
export function createMarkdownRenderer(): Renderer {
  const renderer = new Renderer();

  // 标题渲染 - 带 emoji + 渐变下划线
  renderer.heading = function(tokens, depth) {
    const text = this.parser.parseInline(tokens);
    const emojis = ['\uD83D\uDCA1', '\uD83D\uDCCA', '\uD83C\uDFAF', '\uD83D\uDCB0', '\uD83D\uDD25', '\u2B50', '\u2705'];
    const emoji = emojis[Math.min(depth - 1, emojis.length - 1)] || '\uD83D\uDCCC';
    const borderClr = cssVar('--md-heading-border', '#e8f0fe');
    const txtClr = cssVar('--md-heading-text', '#1a1a2e');
    return `<h${depth} style="margin-top:16px;margin-bottom:8px;padding-bottom:6px;border-bottom:2px solid ${borderClr};color:${txtClr};font-weight:700;">${emoji} ${text}</h${depth}>`;
  };

  // 段落渲染
  renderer.paragraph = function(tokens) {
    const text = this.parser.parseInline(tokens);
    const txtClr = cssVar('--md-body-text', '#3d3d3d');
    return `<p style="margin:10px 0;line-height:1.7;color:${txtClr};">${text}</p>`;
  };

  // 列表渲染
  renderer.list = function(body, ordered) {
    const tag = ordered ? 'ol' : 'ul';
    return `<${tag} style="padding-left:20px;margin:8px 0;list-style-position:outside;">${body}</${tag}>`;
  };

  // 代码块渲染（暗色主题风格）
  renderer.code = function(code, infostring) {
    const language = infostring || '';
    const bgCode = cssVar('--bg-code', '#f5f7fa');
    const primary = cssVar('--primary-color', '#667eea');
    const codeAccent = cssVar('--md-code-accent', '#e83e8c');
    return `<pre style="background:${bgCode};border-radius:8px;padding:12px;overflow-x:auto;margin:10px 0;border-left:4px solid ${primary};"><code class="language-${language}" style="font-family:'Fira Code',Consolas,Monaco,monospace;font-size:13px;color:${codeAccent};background:transparent;">${code}</code></pre>`;
  };

  // 表格渲染（渐变表头 + 斑马纹行）
  renderer.table = function(header, body) {
    const primary = cssVar('--primary-color', '#667eea');
    const primaryLight = cssVar('--primary-light', '#764ba2');
    const bgWhite = cssVar('--bg-white', '#fff');
    const bgAlt = cssVar('--bg-table-alt', '#f8f9fc');
    const tableTxt = cssVar('--md-table-text', '#555');
    const tblBorder = cssVar('--md-table-border', '#eee');
    
    let html = '<div style="overflow-x:auto;margin:10px 0;"><table style="width:100%;border-collapse:collapse;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.06);">';
    if (header) {
      html += `<thead><tr style="background:linear-gradient(135deg, ${primary}, ${primaryLight});color:#fff;">${header}</tr></thead>`;
    }
    html += `<tbody>${body}</tbody></table></div>`;
    return html;
  };

  // 引用块渲染（左侧蓝紫色渐变条）
  renderer.blockquote = function(quote) {
    const primary = cssVar('--primary-color', '#667eea');
    const bgBq = cssVar('--bg-blockquote', '#f8f9ff');
    const bqTxt = cssVar('--md-blockquote-text', '#556680');
    return `<blockquote style="border-left:4px solid ${primary};padding:10px 16px;margin:10px 0;background:${bgBq};border-radius:0 8px 8px 0;color:${bqTxt};font-style:italic;">${quote}</blockquote>`;
  };

  // 粗体渲染（暖色高亮背景）
  renderer.strong = function(text) {
    const strongClr = cssVar('--md-strong-color', '#e65100');
    const bgWarm = cssVar('--bg-highlight-warm', '#fff7ed');
    const bgYel = cssVar('--bg-highlight-yellow', '#fffbe6');
    return `<strong style="color:${strongClr};font-weight:700;background:linear-gradient(135deg, ${bgWarm}, ${bgYel});padding:1px 4px;border-radius:3px;">${text}</strong>`;
  };

  // 图片渲染
  renderer.image = function(href, title, text) {
    return `<img src="${href}" alt="${text || ''}" title="${title || ''}" style="max-width:100%;border-radius:8px;margin:10px 0;box-shadow:0 2px 8px rgba(0,0,0,0.1);">`;
  };

  return renderer;
}

/** 预创建的渲染器单例，避免每次渲染都重新创建 */
export const mdRendererInstance: Renderer = createMarkdownRenderer();

/**
 * 将 Markdown 文本渲染为 HTML 字符串
 * @param content Markdown 原文
 * @returns 渲染后的 HTML 字符串
 */
export function renderMarkdown(content: string): string {
  if (!content || !content.trim()) return '';
  try {
    // 深度清理文本，确保Markdown格式正确
    let cleanedContent = content
      // 清理多余的标点符号
      .replace(/([.!?，。！？]){2,}/g, '$1') // 移除重复标点
      .replace(/([,，]){2,}/g, '$1') // 移除重复逗号
      .replace(/([:：]){2,}/g, '$1') // 移除重复冒号
      
      // 确保Markdown格式正确
      .replace(/([#]+)([^\s#])/g, '$1 $2') // 确保标题后有空格
      .replace(/([*_]+)([^\s*_])/g, '$1 $2') // 确保粗体/斜体标记后有空格
      .replace(/([*_]+)$/g, '') // 移除末尾的标记
      .replace(/\|{3,}/g, '|') // 清理多余的表格分隔符
      
      // 确保数字和单位之间的空格
      .replace(/(\d+)([kK])([^\s])/g, '$1$2 $3') // 确保k后面有空格
      .replace(/(\d+)([%,])([^\s])/g, '$1$2 $3') // 确保百分号/逗号后有空格
      
      // 清理多余的空格
      .replace(/\s{2,}/g, ' ') // 多个空格替换为单个空格
      .replace(/^\s+|\s+$/g, ''); // 移除首尾空格
    
    return marked(cleanedContent, { renderer: mdRendererInstance, breaks: true, gfm: true }) as string;
  } catch {
    return marked.parse(content) as string;
  }
}
