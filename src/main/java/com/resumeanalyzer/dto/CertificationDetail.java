package com.resumeanalyzer.dto;

public record CertificationDetail(
    String certificationName,
    String platform
) {
    public static CertificationDetail createEmpty() {
        return new CertificationDetail("", "");
    }
}
