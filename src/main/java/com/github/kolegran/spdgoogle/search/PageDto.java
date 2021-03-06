package com.github.kolegran.spdgoogle.search;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class PageDto {
    private int numberOfDocs;
    private List<PageItemDto> pageItems;
}
