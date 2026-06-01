package com.resumeanalyzer.dto;

public record PersonalInfo(
    String name,
    String email,
    String phone,
    String location,
    String linkedin,
    String github,
    String portfolio,
    String website
) {
    public static PersonalInfo createEmpty() {
        return new PersonalInfo("", "", "", "", "", "", "", "");
    }
}
