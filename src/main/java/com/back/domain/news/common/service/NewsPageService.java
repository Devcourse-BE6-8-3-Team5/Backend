package com.back.domain.news.common.service;

import com.back.domain.news.common.enums.NewsType;
import com.back.global.rsData.RsData;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NewsPageService {

    public <T> RsData<Page<T>> getPagedNews(
            Page<T> newsPage,
            NewsType newsType
    ) {
        String newsTypeDescription = newsType.getDescription();
        if(newsPage.getTotalPages() == 0){
            return RsData.of(404, String.format("%s 뉴스가 없습니다", newsTypeDescription));
        }
        if(newsPage.isEmpty()) {
            return RsData.of(
                    400,
                    String.format("요청한 페이지의 범위 초과. 총 %d페이지 중 %d페이지를 요청.",
                    newsPage.getTotalPages(), newsPage.getNumber()+1));
        }

        return RsData.of(
                200,
                String.format("%s 뉴스 %d건 조회(전체 %d건)  [ %d / %d pages]",
                        newsType,
                        newsPage.getNumberOfElements(),
                        newsPage.getTotalElements(),
                        newsPage.getNumber()+1,
                        newsPage.getTotalPages()),
                newsPage
        );
    }

    public <T> RsData<T> getSingleNews(
            Optional<T> news,
            NewsType newsType,
            Long id
    ) {
        String newsTypeDescription = newsType.getDescription();

        return news
                .map(dto -> RsData.of(200, "조회 성공", dto))
                .orElse(RsData.of(404,
                        String.format("%d 번의 %s 뉴스가 존재하지 않습니다", id, newsTypeDescription)));
    }
}
