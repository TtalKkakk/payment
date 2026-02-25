package com.example.pg.common.util;

import org.springframework.data.domain.Page;

/**
 * 페이지네이션 블록 계산 유틸리티.
 * 화면에 노출할 페이지 번호 범위(startPage ~ endPage)를 10개 단위 등으로 계산한다.
 */
public final class PaginationHelper {

    private PaginationHelper() {
    }

    /**
     * 현재 페이지가 속한 블록의 startPage, endPage를 계산한다.
     * 예: blockSize=10일 때, 1~10페이지는 [0,9], 11~20페이지는 [10,19]
     *
     * @param page     Spring Page (0-based)
     * @param blockSize 블록 크기 (한 번에 보여줄 페이지 개수)
     * @return PaginationBlockInfo
     */
    public static PaginationBlockInfo computeBlock(Page<?> page, int blockSize) {
        int totalPages = page.getTotalPages();
        int currentPage = page.getNumber();
        int startPage = (currentPage / blockSize) * blockSize;
        int endPage = Math.min(startPage + blockSize - 1, totalPages > 0 ? totalPages - 1 : 0);
        return new PaginationBlockInfo(startPage, endPage);
    }

    public record PaginationBlockInfo(int startPage, int endPage) {
    }
}
