package com.back.global.util;

import java.util.HashMap;
import java.util.Map;

public class HtmlEntityDecoder {

    // HTML 엔티티 매핑 테이블
    private static final Map<String, String> HTML_ENTITIES = new HashMap<>();

    static {
        // 기본 HTML 엔티티들
        HTML_ENTITIES.put("&lt;", "<");
        HTML_ENTITIES.put("&gt;", ">");
        HTML_ENTITIES.put("&amp;", "&");
        HTML_ENTITIES.put("&quot;", "\"");
        HTML_ENTITIES.put("&apos;", "'");
        HTML_ENTITIES.put("&#39;", "'");
        HTML_ENTITIES.put("&nbsp;", " ");

        // HTML 태그들
        HTML_ENTITIES.put("&lt;b&gt;", "");
        HTML_ENTITIES.put("&lt;/b&gt;", "");
        HTML_ENTITIES.put("\\u003Cb\\u003E", "");
        HTML_ENTITIES.put("\\u003C/b\\u003E", "");
        HTML_ENTITIES.put("\\u003C", "<");
        HTML_ENTITIES.put("\\u003E", ">");

        // 자주 사용되는 특수문자들
        HTML_ENTITIES.put("&mdash;", "—");
        HTML_ENTITIES.put("&ndash;", "–");
        HTML_ENTITIES.put("&lsquo;", "'");
        HTML_ENTITIES.put("&rsquo;", "'");
        HTML_ENTITIES.put("&ldquo;", "\"");
        HTML_ENTITIES.put("&rdquo;", "\"");
        HTML_ENTITIES.put("&hellip;", "…");
    }


    public static String decode(String encodedText) {
        if (encodedText == null || encodedText.isEmpty()) {
            return encodedText;
        }

        String result = encodedText;

        // HTML 엔티티 디코딩
        for (Map.Entry<String, String> entry : HTML_ENTITIES.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        // 남아있는 HTML 태그 제거 (정규식 사용)
        result = result.replaceAll("<[^>]+>", "");

        // 연속된 공백을 하나로 합치고 앞뒤 공백 제거
        result = result.replaceAll("\\s+", " ").trim();

        // 남아있는 유니코드 이스케이프 처리
        result = decodeUnicodeEscapes(result);

        return result;
    }


    private static String decodeUnicodeEscapes(String text) {
        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < text.length()) {
            if (text.startsWith("\\u", i) && i + 5 < text.length()) {
                try {
                    String hex = text.substring(i + 2, i + 6);
                    int codePoint = Integer.parseInt(hex, 16);
                    result.append((char) codePoint);
                    i += 6;
                } catch (NumberFormatException e) {
                    result.append(text.charAt(i));
                    i++;
                }
            } else {
                result.append(text.charAt(i));
                i++;
            }
        }

        return result.toString();
    }
}