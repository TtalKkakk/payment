package com.example.pg.merchant.query.application.dto;

import com.example.pg.common.util.PaginationHelper;
import com.example.pg.merchant.domain.aggregate.Merchant;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 가맹점 목록 + 페이지네이션 블록 정보.
 */
public record PagedMerchantsResult(
        List<Merchant> content,
        Page<Merchant> page,
        int startPage,
        int endPage
) {
    public static PagedMerchantsResult of(Page<Merchant> page, int blockSize) {
        PaginationHelper.PaginationBlockInfo block = PaginationHelper.computeBlock(page, blockSize);
        return new PagedMerchantsResult(
                page.getContent(),
                page,
                block.startPage(),
                block.endPage()
        );
    }
}
