package com.example.recruitment;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CrawlTest {
    private static final Map<String, String> SITE_MAP = new HashMap<>();
    static {
        SITE_MAP.put("boss", "BOSS直聘");
        SITE_MAP.put("zhaopin", "智联招聘");
        SITE_MAP.put("51job", "前程无忧");
        SITE_MAP.put("liepin", "猎聘");
    }

    public static void main(String[] args) {
        System.out.println("=== 开始爬虫测试 ===\n");
        
        testBossZhipin();
        testZhaopin();
        test51Job();
        testLiepin();
        
        System.out.println("\n=== 测试完成 ===");
    }

    private static void testBossZhipin() {
        System.out.println("\n--- 测试BOSS直聘 ---");
        try {
            String url = "https://www.zhipin.com/web/geek/job?query=Java&city=150";
            System.out.println("请求URL: " + url);
            
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(15000)
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Cache-Control", "max-age=0")
                .header("Referer", "https://www.baidu.com/")
                .followRedirects(true)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .get();
            
            System.out.println("页面标题: " + doc.title());
            System.out.println("页面HTML长度: " + doc.html().length());
            System.out.println("前500字符: " + doc.html().substring(0, Math.min(500, doc.html().length())));
            
            Elements cards = doc.select("div,li,article,.job-card,.job-item,.job-info");
            System.out.println("找到 " + cards.size() + " 个候选元素");
            
            for (int i = 0; i < Math.min(3, cards.size()); i++) {
                Element card = cards.get(i);
                System.out.println("\n元素 " + (i + 1) + ":");
                System.out.println("  文本: " + card.text().substring(0, Math.min(100, card.text().length())));
            }
            
        } catch (Exception e) {
            System.out.println("BOSS直聘测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testZhaopin() {
        System.out.println("\n--- 测试智联招聘 ---");
        try {
            String url = "https://sou.zhaopin.com/?kw=Java&jl=长沙";
            System.out.println("请求URL: " + url);
            
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(15000)
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .followRedirects(true)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .get();
            
            System.out.println("页面标题: " + doc.title());
            System.out.println("页面HTML长度: " + doc.html().length());
            
        } catch (Exception e) {
            System.out.println("智联招聘测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void test51Job() {
        System.out.println("\n--- 测试前程无忧 ---");
        try {
            String url = "https://we.51job.com/pc/search?keyword=Java&jobarea=150";
            System.out.println("请求URL: " + url);
            
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(15000)
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .followRedirects(true)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .get();
            
            System.out.println("页面标题: " + doc.title());
            System.out.println("页面HTML长度: " + doc.html().length());
            
        } catch (Exception e) {
            System.out.println("前程无忧测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testLiepin() {
        System.out.println("\n--- 测试猎聘 ---");
        try {
            String url = "https://www.liepin.com/zhaopin/?dqs=150&key=Java";
            System.out.println("请求URL: " + url);
            
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(15000)
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .followRedirects(true)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .get();
            
            System.out.println("页面标题: " + doc.title());
            System.out.println("页面HTML长度: " + doc.html().length());
            
        } catch (Exception e) {
            System.out.println("猎聘测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}