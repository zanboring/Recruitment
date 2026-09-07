package com.example.recruitment.crawl;

import com.microsoft.playwright.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import java.nio.file.Paths;

@Component
public class PlaywrightBrowser {
    private Playwright playwright;
    private Browser browser;

    private synchronized void init() {
        if (playwright == null) {
            playwright = Playwright.create();
            // 无头模式，适配服务器/演示环境
            browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
            );
        }
    }

    /**
     * 渲染动态页面，返回 Jsoup Document
     * @param url 目标 URL
     * @param waitSelector 等待渲染完成的选择器（如 ".job-card-box"）
     */
    public Document render(String url, String waitSelector) {
        init();
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            page.navigate(url, new Page.NavigateOptions().setTimeout(30000));
            // 等待关键内容渲染完成
            if (waitSelector != null && !waitSelector.isBlank()) {
                page.waitForSelector(waitSelector, new Page.WaitForSelectorOptions().setTimeout(15000));
            }
            // 等待额外 2 秒，确保异步内容加载完成
            page.waitForTimeout(2000);
            String html = page.content();
            return Jsoup.parse(html);
        } catch (Exception e) {
            throw new RuntimeException("Playwright 渲染页面失败: " + url + "，原因: " + e.getMessage(), e);
        }
    }

    /**
     * 渲染并提取详情页 HTML（用于兜底存储）
     */
    public String fetchDetailHtml(String url) {
        init();
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            page.navigate(url, new Page.NavigateOptions().setTimeout(30000));
            // 等待详情页关键内容
            try {
                page.waitForSelector(".job-detail, .job-desc, .description", new Page.WaitForSelectorOptions().setTimeout(10000));
            } catch (Exception ignored) {}
            page.waitForTimeout(2000);
            return page.content();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 销毁资源（Spring 销毁 Bean 时调用）
     */
    public void destroy() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
    }
}
