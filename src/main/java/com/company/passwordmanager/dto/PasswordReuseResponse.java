package com.company.passwordmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordReuseResponse {
    private boolean reused;
    private long count;
    private List<ReuseItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReuseItem {
        private Long id;
        private String serviceName;
        private String category;
    }
}
