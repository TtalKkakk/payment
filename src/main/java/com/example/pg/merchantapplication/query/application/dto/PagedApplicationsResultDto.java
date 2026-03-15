package com.example.pg.merchantapplication.query.application.dto;

import com.example.pg.merchantapplication.domain.aggregate.MerchantApplication;
import com.example.pg.common.util.PaginationHelper;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 가맹점 신청 목록 + 페이지네이션 블록 정보.
 */
public record PagedApplicationsResultDto(
        List<MerchantApplication> content,
        Page<MerchantApplication> page,
        int startPage,
        int endPage
) {
    public static PagedApplicationsResultDto of(Page<MerchantApplication> page, int blockSize) {
        PaginationHelper.PaginationBlockInfo block = PaginationHelper.computeBlock(page, blockSize);
        return new PagedApplicationsResultDto(
                page.getContent(),
                page,
                block.startPage(),
                block.endPage()
        );
    }
}
