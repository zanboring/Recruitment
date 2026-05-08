package com.example.recruitment.crawl;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ParserFactory {

    private final Map<String, JobParser> parserMap = new HashMap<>();

    public ParserFactory(List<JobParser> parsers) {
        for (JobParser parser : parsers) {
            parserMap.put(parser.getPlatform(), parser);
        }
    }

    public JobParser getParser(String platform) {
        return parserMap.get(platform);
    }

    public JobParser getParserByCode(String code) {
        return switch (code.toLowerCase()) {
            case "boss" -> parserMap.get("BOSS直聘");
            case "zhaopin" -> parserMap.get("智联招聘");
            case "51job" -> parserMap.get("前程无忧");
            case "liepin" -> parserMap.get("猎聘");
            default -> null;
        };
    }
}
