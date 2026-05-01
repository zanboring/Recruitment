package com.example.recruitment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring MVC 全局配置
 * <p>
 * 核心作用：强制所有消息转换器使用 UTF-8 编码，解决前端发送含中文的
 * JSON 请求体时 Jackson 解析失败（Invalid UTF-8 middle byte）的问题。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置消息转换器，强制 UTF-8
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 1. String 转换器：强制 UTF-8，避免 ISO-8859-1 乱码
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setSupportedMediaTypes(List.of(
                new MediaType("text", "plain", StandardCharsets.UTF_8),
                new MediaType("text", "event-stream", StandardCharsets.UTF_8),
                new MediaType("*", "*", StandardCharsets.UTF_8)
        ));
        converters.add(0, stringConverter);

        // 2. Jackson JSON 转换器：强制 UTF-8
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.setSupportedMediaTypes(List.of(
                new MediaType("application", "json", StandardCharsets.UTF_8),
                new MediaType("application", "*+json", StandardCharsets.UTF_8)
        ));
        converters.add(1, jacksonConverter);
    }

    /**
     * 在已有转换器列表中也追加 UTF-8 支持（双重保障）
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof StringHttpMessageConverter sc) {
                sc.setDefaultCharset(StandardCharsets.UTF_8);
            } else if (converter instanceof MappingJackson2HttpMessageConverter jc) {
                List<MediaType> types = jc.getSupportedMediaTypes();
                boolean hasUtf8 = types.stream().anyMatch(
                        t -> StandardCharsets.UTF_8.name().equalsIgnoreCase(
                                t.getCharset() != null ? t.getCharset().name() : ""));
                if (!hasUtf8) {
                    jc.setSupportedMediaTypes(List.of(
                            new MediaType("application", "json", StandardCharsets.UTF_8),
                            new MediaType("application", "*+json", StandardCharsets.UTF_8)
                    ));
                }
            }
        }
    }
}
