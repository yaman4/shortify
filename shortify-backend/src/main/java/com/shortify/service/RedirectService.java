package com.shortify.service;

public interface RedirectService {
    String getLongUrlAndIncrementCount(String shortCode);
    String resolve(String shortCode);
}
