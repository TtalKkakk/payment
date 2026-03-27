package com.example.pg.merchant.application.dto;

import com.example.pg.common.util.PaginationHelper;
import com.example.pg.merchant.domain.aggregate.Merchant;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 가맹점 목록 + 페이지네이션 블록 정보.
 */
public record PagedMerchantsResultDto(
        List<Merchant> content,
        Page<Merchant> page,
        int startPage,
        int endPage
) {
    public static PagedMerchantsResultDto of(Page<Merchant> page, int blockSize) {
        PaginationHelper.PaginationBlockInfo block = PaginationHelper.computeBlock(page, blockSize);
        return new PagedMerchantsResultDto(
                page.getContent(),
                page,
                block.startPage(),
                block.endPage()
        );
    }
}
