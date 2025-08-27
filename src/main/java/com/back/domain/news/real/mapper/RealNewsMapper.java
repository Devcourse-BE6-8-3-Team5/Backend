package com.back.domain.news.real.mapper;

import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.entity.RealNews;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RealNewsMapper {
    public List<RealNews> toEntityList(List<RealNewsDto> realNewsDtoList) {
        return realNewsDtoList.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public RealNews toEntity(RealNewsDto realNewsDto) {
        return RealNews.builder()
                .title(realNewsDto.title())
                .content(realNewsDto.content())
                .description(realNewsDto.description())
                .link(realNewsDto.link())
                .imgUrl(realNewsDto.imgUrl())
                .originCreatedDate(realNewsDto.originCreatedDate())
                .createdDate(realNewsDto.createdDate())
                .mediaName(realNewsDto.mediaName())
                .journalist(realNewsDto.journalist())
                .originalNewsUrl(realNewsDto.originalNewsUrl())
                .newsCategory(realNewsDto.newsCategory())
                .build();
    }

    public List<RealNewsDto> toDtoList(List<RealNews> realNewsList) {
        return realNewsList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public RealNewsDto toDto(RealNews realNews) {
        return RealNewsDto.of(
                realNews.getId(),
                realNews.getTitle(),
                realNews.getContent(),
                realNews.getDescription(),
                realNews.getLink(),
                realNews.getImgUrl(),
                realNews.getOriginCreatedDate(),
                realNews.getCreatedDate(),
                realNews.getMediaName(),
                realNews.getJournalist(),
                realNews.getOriginalNewsUrl(),
                realNews.getNewsCategory()
        );
    }


}
