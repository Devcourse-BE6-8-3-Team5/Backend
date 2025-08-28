package com.back.domain.news.common.dto

//네이버 API로 조회한 뉴스의 링크 정보를 바탕으로 원본 링크에서 추출할 정보들을 담은 DTO
data class NewsDetailDto(
    val content: String,  // 본문 내용
    val imgUrl: String,  // 이미지 URL
    val journalist: String,  // 기자명
    val mediaName: String // 언론사명
)
//{
//    companion object {
//        @JvmStatic
//        fun of(content: String?, imgUrl: String?, journalist: String?, mediaName: String?): NewsDetailDto {
//            return NewsDetailDto(content, imgUrl, journalist, mediaName)
//        }
//
//        fun from(item: JsonNode): NewsDetailDto {
//            return NewsDetailDto(
//                item.get("content").asText(""),
//                item.get("imgUrl").asText(""),
//                item.get("journalist").asText(""),
//                item.get("mediaName").asText("")
//            )
//        }
//    }
//}